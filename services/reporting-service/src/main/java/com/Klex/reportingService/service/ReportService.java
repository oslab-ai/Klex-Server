package com.Klex.reportingService.service;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRCsvDataSource;
import net.sf.jasperreports.engine.data.JRXmlDataSource;
import net.sf.jasperreports.engine.data.JsonDataSource;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.export.*;
import net.sf.jasperreports.engine.export.oasis.JROdtExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRPptxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.export.*;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.Klex.reportingService.dto.ReportRequest;
import com.Klex.reportingService.entity.ReportAuditLog;

import java.io.InputStream;
import java.util.List;

@Service
public class ReportService {

    @Autowired
    private List<InputSourceService> inputSourceServices;

    @Autowired
    private AuditService auditService;

    @org.springframework.beans.factory.annotation.Value("${app.report.datasource.path}")
    private String dataSourcePath;

    public String generateReport(ReportRequest request) {
        InputSourceType sourceType = request.getSourceType();
        String path = request.getPath();

        System.out.println("Using source: " + sourceType);
        System.out.println("Using path: " + path);

        InputSourceService selectedService = null;
        for (InputSourceService service : inputSourceServices) {
            if (service.getSourceType() == sourceType) {
                selectedService = service;
                break;
            }
        }

        if (selectedService == null) {
            return "Error: No service found for source type: " + sourceType;
        }

        // ── Audit: prepare the log entry ──
        ReportAuditLog auditLog = new ReportAuditLog();
        auditLog.setReportPath(path);
        auditLog.setSourceType(sourceType != null ? sourceType.name() : null);
        auditLog.setDataSourceType(request.getDataSourceType());
        auditLog.setOutputFormat(request.getFormat());
        auditLog.setUserId("anonymous"); // expandable when auth is added

        long totalStart = System.currentTimeMillis();

        Connection jdbcConnection = null;
        try (InputStream inputStream = selectedService.getInputStream(path)) {

            // ── Phase 1: COMPILE ──
            long compileStart = System.currentTimeMillis();
            JasperReport jasperReport = JasperCompileManager.compileReport(inputStream);
            long compileEnd = System.currentTimeMillis();
            auditLog.setCompileTimeMs(compileEnd - compileStart);

            // ── Audit: capture JRL design metadata from compiled JasperReport ──
            auditLog.setReportName(jasperReport.getName());
            if (jasperReport.getQuery() != null) {
                auditLog.setQueryLanguage(jasperReport.getQuery().getLanguage());
            }
            auditLog.setFieldCount(jasperReport.getFields() != null ? jasperReport.getFields().length : 0);
            // Count only user-defined parameters (skip system ones)
            int userParamCount = 0;
            if (jasperReport.getParameters() != null) {
                for (JRParameter p : jasperReport.getParameters()) {
                    if (!p.isSystemDefined()) {
                        userParamCount++;
                    }
                }
            }
            auditLog.setParameterCount(userParamCount);
            auditLog.setColumnCount(jasperReport.getColumnCount());
            auditLog.setOrientation(jasperReport.getOrientationValue() != null
                    ? jasperReport.getOrientationValue().name()
                    : null);
            auditLog.setPageWidth(jasperReport.getPageWidth());
            auditLog.setPageHeight(jasperReport.getPageHeight());

            // ── Phase 2: FILL ──
            long fillStart = System.currentTimeMillis();
            JasperPrint jasperPrint;
            Map<String, Object> params = new HashMap<>();

            // ── Inject user-supplied parameters / filters ──
            Map<String, String> userParams = request.getParameters();
            // Track which params were converted to Collection (multi-value)
            java.util.Set<String> collectionParams = new java.util.HashSet<>();

            if (userParams != null && !userParams.isEmpty()) {
                // Build a lookup of declared parameter types from the compiled report
                Map<String, JRParameter> declared = new HashMap<>();
                if (jasperReport.getParameters() != null) {
                    for (JRParameter p : jasperReport.getParameters()) {
                        if (!p.isSystemDefined()) {
                            declared.put(p.getName(), p);
                        }
                    }
                }

                for (Map.Entry<String, String> entry : userParams.entrySet()) {
                    String name = entry.getKey();
                    String value = entry.getValue();
                    if (value == null || value.trim().isEmpty())
                        continue;

                    JRParameter paramDef = declared.get(name);
                    String className = paramDef != null ? paramDef.getValueClassName() : "java.lang.String";

                    // Detect multi-value: comma or pipe separated strings
                    boolean isMultiValue = "java.lang.String".equals(className)
                            && (value.contains(",") || value.contains("|"));

                    if (isMultiValue) {
                        // Split into a Collection<String> for proper SQL IN handling
                        String delimiter = value.contains("|") ? "\\|" : ",";
                        String[] parts = value.split(delimiter);
                        java.util.List<String> valueList = new java.util.ArrayList<>();
                        for (String part : parts) {
                            String trimmed = part.trim();
                            if (!trimmed.isEmpty()) {
                                valueList.add(trimmed);
                            }
                        }
                        if (valueList.size() == 1) {
                            // Single value: pass as plain string
                            params.put(name, valueList.get(0));
                        } else {
                            params.put(name, valueList);
                            collectionParams.add(name);
                        }
                    } else if (paramDef != null) {
                        // Convert string value to the expected Java type
                        Object converted = convertValue(value, className);
                        if (converted != null) {
                            params.put(name, converted);
                        }
                    } else {
                        // Unknown parameter — pass as string anyway (JRL ignores extras)
                        params.put(name, value);
                    }
                }
                System.out.println("User parameters injected: " + params.keySet());
                if (!collectionParams.isEmpty()) {
                    System.out.println("Multi-value (Collection) params: " + collectionParams);
                }
            }

            // ── Rewrite SQL query: convert = $P{param} to IN for Collection params ──
            if (!collectionParams.isEmpty() && jasperReport.getQuery() != null) {
                String originalQuery = jasperReport.getQuery().getText();
                String rewrittenQuery = rewriteQueryForCollectionParams(originalQuery, collectionParams, params);
                if (!rewrittenQuery.equals(originalQuery)) {
                    System.out.println("Rewrote query for multi-select params:\n  BEFORE: " + originalQuery + "\n  AFTER:  " + rewrittenQuery);
                    // Replace the query in the compiled report via a design-object swap
                    final String queryLang = jasperReport.getQuery().getLanguage();
                    JasperDesign jasperDesign;
                    try (InputStream redesignStream = selectedService.getInputStream(path)) {
                        jasperDesign = net.sf.jasperreports.engine.xml.JRXmlLoader.load(redesignStream);
                    }
                    net.sf.jasperreports.engine.design.JRDesignQuery newQuery =
                        new net.sf.jasperreports.engine.design.JRDesignQuery();
                    newQuery.setLanguage(queryLang);
                    newQuery.setText(rewrittenQuery);
                    jasperDesign.setQuery(newQuery);
                    // Re-declare Collection params so compilation knows the type
                    for (String cpName : collectionParams) {
                        if (jasperDesign.getParametersMap().containsKey(cpName)) {
                            net.sf.jasperreports.engine.design.JRDesignParameter dp =
                                (net.sf.jasperreports.engine.design.JRDesignParameter) jasperDesign.getParametersMap().get(cpName);
                            dp.setValueClassName("java.util.Collection");
                        }
                    }
                    jasperReport = JasperCompileManager.compileReport(jasperDesign);
                }
            }

            String dsType = request.getDataSourceType();

            if ("jdbc".equalsIgnoreCase(dsType) && request.getJdbcUrl() != null) {
                // ── JDBC data source ──
                System.out.println("Using JDBC data source: " + request.getJdbcUrl());
                jdbcConnection = DriverManager.getConnection(
                        request.getJdbcUrl(),
                        request.getJdbcUser(),
                        request.getJdbcPassword());
                jasperPrint = JasperFillManager.fillReport(jasperReport, params, jdbcConnection);

            } else if ("json".equalsIgnoreCase(dsType) && request.getJsonFilePath() != null) {
                // ── JSON data source ──
                String jsonPath = request.getJsonFilePath();
                System.out.println("Using JSON data source: " + jsonPath);
                InputStream jsonStream;
                if (jsonPath.startsWith("http://") || jsonPath.startsWith("https://")) {
                    jsonStream = new java.net.URL(jsonPath).openStream();
                } else {
                    jsonStream = new FileInputStream(new File(jsonPath));
                }
                JsonDataSource jsonDs = new JsonDataSource(jsonStream);
                jasperPrint = JasperFillManager.fillReport(jasperReport, params, jsonDs);

            } else if ("xml".equalsIgnoreCase(dsType) && request.getXmlFilePath() != null) {
                // ── XML data source ──
                String xmlPath = request.getXmlFilePath();
                System.out.println("Using XML data source: " + xmlPath);
                JRXmlDataSource xmlDs;
                
                if (xmlPath.startsWith("http://") || xmlPath.startsWith("https://")) {
                    InputStream xmlStream = new java.net.URL(xmlPath).openStream();
                    if (request.getXmlRecordPath() != null && !request.getXmlRecordPath().isEmpty()) {
                        xmlDs = new JRXmlDataSource(xmlStream, request.getXmlRecordPath());
                    } else {
                        xmlDs = new JRXmlDataSource(xmlStream);
                    }
                } else {
                    if (request.getXmlRecordPath() != null && !request.getXmlRecordPath().isEmpty()) {
                        xmlDs = new JRXmlDataSource(new File(xmlPath), request.getXmlRecordPath());
                    } else {
                        xmlDs = new JRXmlDataSource(new File(xmlPath));
                    }
                }
                jasperPrint = JasperFillManager.fillReport(jasperReport, params, xmlDs);

            } else if ("inmemory".equalsIgnoreCase(dsType)) {
                // ── In-Memory (Empty) data source ──
                System.out.println("Using in-memory (empty) data source");
                jasperPrint = JasperFillManager.fillReport(jasperReport, params, new JREmptyDataSource());

            } else {
                // ── CSV data source (default) ──
                String csvPath = (request.getCsvFilePath() != null && !request.getCsvFilePath().isEmpty())
                        ? request.getCsvFilePath()
                        : dataSourcePath;

                if (csvPath == null || csvPath.isEmpty()) {
                    // No CSV path configured — use empty data source
                    System.out.println("No data source configured, using empty data source");
                    jasperPrint = JasperFillManager.fillReport(jasperReport, params, new JREmptyDataSource());
                } else {
                    System.out.println("Using CSV data source: " + csvPath);

                    InputStream csvStream;
                    if (csvPath.startsWith("http://") || csvPath.startsWith("https://")) {
                        java.net.URL url = new java.net.URL(csvPath);
                        csvStream = url.openStream();
                    } else {
                        File csvFile = new File(csvPath);
                        if (!csvFile.exists()) {
                            throw new RuntimeException(
                                    "CSV data source file not found: " + csvPath
                                            + " — make sure the file was uploaded and the path is accessible inside the container.");
                        }
                        csvStream = new FileInputStream(csvFile);
                    }
                    JRCsvDataSource ds = new JRCsvDataSource(csvStream);
                    ds.setRecordDelimiter("\n");
                    ds.setFieldDelimiter(',');
                    ds.setUseFirstRowAsHeader(true);

                    String[] columnNames = Arrays.stream(jasperReport.getFields())
                            .map(JRField::getName)
                            .toArray(String[]::new);
                    ds.setColumnNames(columnNames);
                    System.out.println("CSV column names set: " + Arrays.toString(columnNames));

                    jasperPrint = JasperFillManager.fillReport(jasperReport, params, ds);
                }
            }

            long fillEnd = System.currentTimeMillis();
            auditLog.setFillTimeMs(fillEnd - fillStart);

            System.out.println("Report pages generated: " + jasperPrint.getPages().size());
            auditLog.setReportPages(jasperPrint.getPages().size());

            String outputPath = request.getOutputPath();
            String format = request.getFormat();

            if (outputPath == null || outputPath.isEmpty()) {
                outputPath = "/tmp/report_" + System.currentTimeMillis();
            }
            if (format == null || format.isEmpty()) {
                format = "PDF";
            }

            format = format.toUpperCase();
            String extension = getFileExtension(format);

            if (!outputPath.toLowerCase().endsWith("." + extension)) {
                outputPath += "." + extension;
            }

            // ── Phase 3: EXPORT ──
            long exportStart = System.currentTimeMillis();
            exportReport(jasperPrint, format, outputPath);
            long exportEnd = System.currentTimeMillis();
            auditLog.setExportTimeMs(exportEnd - exportStart);

            // ── Audit: capture output file size ──
            auditLog.setOutputPath(outputPath);
            auditLog.setOutputFormat(format);
            File outputFile = new File(outputPath);
            if (outputFile.exists()) {
                auditLog.setOutputFileSizeBytes(outputFile.length());
            }

            long totalEnd = System.currentTimeMillis();
            auditLog.setTotalExecutionTimeMs(totalEnd - totalStart);
            auditLog.setStatus("SUCCESS");

            // ── Save audit log ──
            try {
                auditService.logReportExecution(auditLog);
            } catch (Exception auditEx) {
                System.err.println("Warning: Failed to save audit log: " + auditEx.getMessage());
            }

            return format + " generated successfully at " + outputPath;

        } catch (Exception e) {
            e.printStackTrace();

            // ── Audit: log the error ──
            long totalEnd = System.currentTimeMillis();
            auditLog.setTotalExecutionTimeMs(totalEnd - totalStart);
            auditLog.setStatus("ERROR");
            auditLog.setErrorMessage(e.getClass().getSimpleName() + ": " + e.getMessage());
            try {
                auditService.logReportExecution(auditLog);
            } catch (Exception auditEx) {
                System.err.println("Warning: Failed to save audit log: " + auditEx.getMessage());
            }

            return "Error generating report: " + e.getMessage();
        } finally {
            if (jdbcConnection != null) {
                try {
                    jdbcConnection.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * Export the filled report to the specified format using JasperReports Library
     * exporters.
     */
    private void exportReport(JasperPrint jasperPrint, String format, String outputPath) throws JRException {
        switch (format) {
            case "PDF":
                JasperExportManager.exportReportToPdfFile(jasperPrint, outputPath);
                break;

            case "HTML":
                JasperExportManager.exportReportToHtmlFile(jasperPrint, outputPath);
                break;

            case "XML":
                JasperExportManager.exportReportToXmlFile(jasperPrint, outputPath, false);
                break;

            case "XLSX": {
                JRXlsxExporter xlsxExporter = new JRXlsxExporter();
                xlsxExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                xlsxExporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputPath));
                SimpleXlsxReportConfiguration xlsxConfig = new SimpleXlsxReportConfiguration();
                xlsxConfig.setOnePagePerSheet(false);
                xlsxConfig.setDetectCellType(true);
                xlsxConfig.setRemoveEmptySpaceBetweenRows(true);
                xlsxConfig.setWhitePageBackground(false);
                xlsxExporter.setConfiguration(xlsxConfig);
                xlsxExporter.exportReport();
                break;
            }

            case "CSV": {
                JRCsvExporter csvExporter = new JRCsvExporter();
                csvExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                csvExporter.setExporterOutput(new SimpleWriterExporterOutput(outputPath));
                csvExporter.exportReport();
                break;
            }

            case "DOCX": {
                JRDocxExporter docxExporter = new JRDocxExporter();
                docxExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                docxExporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputPath));
                docxExporter.exportReport();
                break;
            }

            case "PPTX": {
                JRPptxExporter pptxExporter = new JRPptxExporter();
                pptxExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                pptxExporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputPath));
                pptxExporter.exportReport();
                break;
            }

            case "ODT": {
                JROdtExporter odtExporter = new JROdtExporter();
                odtExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                odtExporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputPath));
                odtExporter.exportReport();
                break;
            }

            default:
                throw new JRException("Unsupported export format: " + format
                        + ". Supported formats: PDF, HTML, XML, XLSX, CSV, DOCX, PPTX, ODT");
        }
    }

    /**
     * Get the file extension for the given format.
     */
    private String getFileExtension(String format) {
        switch (format) {
            case "PDF":
                return "pdf";
            case "HTML":
                return "html";
            case "XML":
                return "xml";
            case "XLSX":
                return "xlsx";
            case "CSV":
                return "csv";
            case "DOCX":
                return "docx";
            case "PPTX":
                return "pptx";
            case "ODT":
                return "odt";
            default:
                return format.toLowerCase();
        }
    }

    /**
     * Rewrite SQL query clauses to support multi-value Collection parameters.
     *
     * Replaces patterns like:
     *   column = $P{ParamName}     → column IN ($P!{ParamName})
     *   column != $P{ParamName}    → column NOT IN ($P!{ParamName})
     *   column <> $P{ParamName}    → column NOT IN ($P!{ParamName})
     *
     * The $P!{} token is a JasperReports "direct substitution" that injects
     * the value literally (without JDBC parameter binding).  We format the
     * Collection as a SQL-safe comma-separated string of quoted literals so
     * the IN clause is valid.
     */
    private String rewriteQueryForCollectionParams(
            String query,
            java.util.Set<String> collectionParams,
            Map<String, Object> params) {

        String rewritten = query;
        for (String paramName : collectionParams) {
            Object val = params.get(paramName);
            if (!(val instanceof java.util.Collection)) continue;

            @SuppressWarnings("unchecked")
            java.util.Collection<String> values = (java.util.Collection<String>) val;

            // Build SQL-safe IN list: 'val1', 'val2', ...
            StringBuilder inList = new StringBuilder();
            boolean first = true;
            for (String v : values) {
                if (!first) inList.append(", ");
                // Escape single quotes in values to prevent SQL injection
                inList.append("'").append(v.replace("'", "''")).append("'");
                first = false;
            }
            String inLiteral = inList.toString();

            // Replace the Collection param value with the formatted IN literal
            // so $P!{paramName} will inject it directly into SQL
            params.put(paramName, inLiteral);

            // Pattern 1:  column = $P{param}  →  column IN ($P!{param})
            // Handles optional whitespace and both = and == operators
            String eqPattern = "(\\w+)\\s*={1,2}\\s*\\$P\\{" + java.util.regex.Pattern.quote(paramName) + "\\}";
            rewritten = rewritten.replaceAll(
                    eqPattern,
                    "$1 IN (\\$P!{" + paramName + "})"
            );

            // Pattern 2:  column != $P{param}  or  column <> $P{param}  →  column NOT IN ($P!{param})
            String neqPattern1 = "(\\w+)\\s*!=\\s*\\$P\\{" + java.util.regex.Pattern.quote(paramName) + "\\}";
            rewritten = rewritten.replaceAll(
                    neqPattern1,
                    "$1 NOT IN (\\$P!{" + paramName + "})"
            );
            String neqPattern2 = "(\\w+)\\s*<>\\s*\\$P\\{" + java.util.regex.Pattern.quote(paramName) + "\\}";
            rewritten = rewritten.replaceAll(
                    neqPattern2,
                    "$1 NOT IN (\\$P!{" + paramName + "})"
            );

            // Pattern 3:  LIKE $P{param}  →  ( column LIKE val1 OR column LIKE val2 )
            // (No action — LIKE with multi-value is unusual; leave as-is)

            // Pattern 4: $P{param} IS NULL OR ... patterns (null-check wrappers)
            // These still work: $P!{param} IS NULL evaluates to "'a','b' IS NULL" → false, which is correct.
        }
        return rewritten;
    }

    /**
     * Convert a string value to the Java type expected by a JasperReports
     * parameter.
     */
    private Object convertValue(String value, String className) {
        if (value == null || className == null)
            return value;
        try {
            switch (className) {
                case "java.lang.String":
                    return value;
                case "java.lang.Integer":
                case "int":
                    return Integer.parseInt(value);
                case "java.lang.Long":
                case "long":
                    return Long.parseLong(value);
                case "java.lang.Double":
                case "double":
                    return Double.parseDouble(value);
                case "java.lang.Float":
                case "float":
                    return Float.parseFloat(value);
                case "java.lang.Short":
                case "short":
                    return Short.parseShort(value);
                case "java.lang.Boolean":
                case "boolean":
                    return Boolean.parseBoolean(value);
                case "java.math.BigDecimal":
                    return new java.math.BigDecimal(value);
                case "java.util.Date":
                    return tryParseDate(value);
                case "java.sql.Date":
                    return java.sql.Date.valueOf(value); // expects yyyy-MM-dd
                case "java.sql.Timestamp":
                    return java.sql.Timestamp.valueOf(value);
                case "java.util.List":
                case "java.util.Collection":
                    // Comma-separated or Pipe-separated string to List<String>
                    if (value.contains("|")) {
                        return Arrays.asList(value.split("\\|"));
                    }
                    return Arrays.asList(value.split(","));
                default:
                    return value;
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not convert parameter value '" + value
                    + "' to " + className + ": " + e.getMessage());
            return value; // Pass as string — better than nothing
        }
    }

    private java.util.Date tryParseDate(String value) {
        String[] formats = { "yyyy-MM-dd", "yyyy-MM-dd'T'HH:mm:ss", "MM/dd/yyyy", "dd/MM/yyyy", "yyyy-MM-dd HH:mm:ss" };
        for (String fmt : formats) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(fmt);
                sdf.setLenient(false);
                return sdf.parse(value);
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
