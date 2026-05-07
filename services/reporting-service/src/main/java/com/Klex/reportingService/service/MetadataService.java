package com.Klex.reportingService.service;

import com.Klex.reportingService.dto.MetadataResponse;
import com.Klex.reportingService.dto.MetadataResponse.*;
import net.sf.jasperreports.engine.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class MetadataService {

    @Autowired
    private List<InputSourceService> inputSourceServices;

    /**
     * Extract structural metadata from a JRXML file using the JasperReports
     * Library.
     */
    public MetadataResponse extractMetadata(InputSourceType sourceType, String path) throws Exception {
        InputSourceService selectedService = null;
        for (InputSourceService service : inputSourceServices) {
            if (service.getSourceType() == sourceType) {
                selectedService = service;
                break;
            }
        }

        if (selectedService == null) {
            throw new IllegalArgumentException("No service found for source type: " + sourceType);
        }

        // Read the stream into a byte array so we can use it twice (JRL compile + DOM
        // for properties)
        byte[] jrxmlBytes;
        try (InputStream inputStream = selectedService.getInputStream(path)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            jrxmlBytes = baos.toByteArray();
        }

        // ===== JRL Compilation =====
        JasperReport jasperReport;
        try (InputStream compileStream = new ByteArrayInputStream(jrxmlBytes)) {
            jasperReport = JasperCompileManager.compileReport(compileStream);
        }

        MetadataResponse response = new MetadataResponse();

        // --- Report name and page layout (from JasperReport API) ---
        response.setReportName(jasperReport.getName());
        response.setPageWidth(jasperReport.getPageWidth());
        response.setPageHeight(jasperReport.getPageHeight());
        response.setColumnWidth(jasperReport.getColumnWidth());
        response.setLeftMargin(jasperReport.getLeftMargin());
        response.setRightMargin(jasperReport.getRightMargin());
        response.setTopMargin(jasperReport.getTopMargin());
        response.setBottomMargin(jasperReport.getBottomMargin());

        // --- Query (from KlexReport API) ---
        JRQuery query = jasperReport.getQuery();
        if (query != null) {
            response.setQueryLanguage(query.getLanguage());
            response.setQueryText(query.getText());
        }

        // --- Parameters (from JasperReport API) ---
        response.setParameters(extractParameters(jasperReport));

        // --- Fields (from JasperReport API) ---
        response.setFields(extractFields(jasperReport));

        // --- Variables (from JasperReport API) ---
        response.setVariables(extractVariables(jasperReport));

        // --- Bands (from JasperReport API) ---
        response.setBands(extractBands(jasperReport));

        // --- Sub-reports and Charts (walk all elements) ---
        List<String> subReports = new ArrayList<>();
        List<String> charts = new ArrayList<>();
        walkElements(jasperReport, subReports, charts);
        response.setSubReports(subReports);
        response.setCharts(charts);

        // --- Data adapter (Jaspersoft Studio property — only available via XML) ---
        try (InputStream domStream = new ByteArrayInputStream(jrxmlBytes)) {
            response.setDataAdapterName(extractDataAdapterFromXml(domStream));
        }

        return response;
    }

    // ========================= JasperReports-based extraction =========================

    private List<ParameterInfo> extractParameters(JasperReport report) {
        List<ParameterInfo> list = new ArrayList<>();
        JRParameter[] params = report.getParameters();
        if (params != null) {
            for (JRParameter p : params) {
                // Skip system/built-in parameters
                if (p.isSystemDefined()) {
                    continue;
                }
                String defaultExpr = null;
                if (p.getDefaultValueExpression() != null) {
                    defaultExpr = p.getDefaultValueExpression().getText();
                }
                list.add(new ParameterInfo(
                        p.getName(),
                        p.getValueClassName(),
                        defaultExpr,
                        p.isForPrompting()));
            }
        }
        return list;
    }

    private List<FieldInfo> extractFields(JasperReport report) {
        List<FieldInfo> list = new ArrayList<>();
        JRField[] fields = report.getFields();
        if (fields != null) {
            for (JRField f : fields) {
                list.add(new FieldInfo(
                        f.getName(),
                        f.getValueClassName(),
                        f.getDescription()));
            }
        }
        return list;
    }

    private List<VariableInfo> extractVariables(JasperReport report) {
        List<VariableInfo> list = new ArrayList<>();
        JRVariable[] variables = report.getVariables();
        if (variables != null) {
            for (JRVariable v : variables) {
                // Skip system variables (PAGE_NUMBER, COLUMN_NUMBER, REPORT_COUNT, etc.)
                if (v.isSystemDefined()) {
                    continue;
                }
                String expression = null;
                if (v.getExpression() != null) {
                    expression = v.getExpression().getText();
                }
                String calculation = v.getCalculationValue() != null
                        ? v.getCalculationValue().name()
                        : null;
                String resetType = v.getResetTypeValue() != null
                        ? v.getResetTypeValue().name()
                        : null;

                list.add(new VariableInfo(
                        v.getName(),
                        v.getValueClassName(),
                        calculation,
                        expression,
                        resetType));
            }
        }
        return list;
    }

    private List<BandInfo> extractBands(JasperReport report) {
        List<BandInfo> list = new ArrayList<>();

        addBand(list, "background", report.getBackground());
        addBand(list, "title", report.getTitle());
        addBand(list, "pageHeader", report.getPageHeader());
        addBand(list, "columnHeader", report.getColumnHeader());

        // Detail bands (can be multiple)
        JRSection detailSection = report.getDetailSection();
        if (detailSection != null && detailSection.getBands() != null) {
            for (JRBand band : detailSection.getBands()) {
                addBand(list, "detail", band);
            }
        }

        addBand(list, "columnFooter", report.getColumnFooter());
        addBand(list, "pageFooter", report.getPageFooter());
        addBand(list, "lastPageFooter", report.getLastPageFooter());
        addBand(list, "summary", report.getSummary());
        addBand(list, "noData", report.getNoData());

        return list;
    }

    private void addBand(List<BandInfo> list, String type, JRBand band) {
        if (band != null) {
            int elementCount = (band.getElements() != null) ? band.getElements().length : 0;
            list.add(new BandInfo(type, band.getHeight(), elementCount));
        }
    }

    /**
     * Walk all elements in all bands to detect JRSubreport and JRChart instances.
     */
    private void walkElements(JasperReport report, List<String> subReports, List<String> charts) {
        JRBand[] allBands = getAllBands(report);
        for (JRBand band : allBands) {
            if (band != null && band.getElements() != null) {
                for (JRElement element : band.getElements()) {
                    walkElement(element, subReports, charts);
                }
            }
        }
    }

    private void walkElement(JRElement element, List<String> subReports, List<String> charts) {
        if (element instanceof JRSubreport) {
            JRSubreport sr = (JRSubreport) element;
            String expr = (sr.getExpression() != null) ? sr.getExpression().getText() : "(inline subreport)";
            subReports.add(expr);
        }

        if (element instanceof JRChart) {
            JRChart chart = (JRChart) element;
            String chartType = "chart";
            if (chart.getChartType() > 0) {
                chartType = getChartTypeName(chart.getChartType());
            }
            charts.add(chartType);
        }

        // Recurse into frames (which can contain nested elements)
        if (element instanceof JRFrame) {
            JRFrame frame = (JRFrame) element;
            if (frame.getElements() != null) {
                for (JRElement child : frame.getElements()) {
                    walkElement(child, subReports, charts);
                }
            }
        }
    }

    private JRBand[] getAllBands(JasperReport report) {
        List<JRBand> bands = new ArrayList<>();
        bands.add(report.getBackground());
        bands.add(report.getTitle());
        bands.add(report.getPageHeader());
        bands.add(report.getColumnHeader());

        JRSection detail = report.getDetailSection();
        if (detail != null && detail.getBands() != null) {
            for (JRBand b : detail.getBands()) {
                bands.add(b);
            }
        }

        bands.add(report.getColumnFooter());
        bands.add(report.getPageFooter());
        bands.add(report.getLastPageFooter());
        bands.add(report.getSummary());
        bands.add(report.getNoData());

        return bands.toArray(new JRBand[0]);
    }

    private String getChartTypeName(byte chartType) {
        switch (chartType) {
            case JRChart.CHART_TYPE_AREA:
                return "areaChart";
            case JRChart.CHART_TYPE_BAR:
                return "barChart";
            case JRChart.CHART_TYPE_BAR3D:
                return "bar3DChart";
            case JRChart.CHART_TYPE_BUBBLE:
                return "bubbleChart";
            case JRChart.CHART_TYPE_CANDLESTICK:
                return "candlestickChart";
            case JRChart.CHART_TYPE_HIGHLOW:
                return "highLowChart";
            case JRChart.CHART_TYPE_LINE:
                return "lineChart";
            case JRChart.CHART_TYPE_METER:
                return "meterChart";
            case JRChart.CHART_TYPE_MULTI_AXIS:
                return "multiAxisChart";
            case JRChart.CHART_TYPE_PIE:
                return "pieChart";
            case JRChart.CHART_TYPE_PIE3D:
                return "pie3DChart";
            case JRChart.CHART_TYPE_SCATTER:
                return "scatterChart";
            case JRChart.CHART_TYPE_STACKEDBAR:
                return "stackedBarChart";
            case JRChart.CHART_TYPE_STACKEDBAR3D:
                return "stackedBar3DChart";
            case JRChart.CHART_TYPE_STACKEDAREA:
                return "stackedAreaChart";
            case JRChart.CHART_TYPE_THERMOMETER:
                return "thermometerChart";
            case JRChart.CHART_TYPE_TIMESERIES:
                return "timeSeriesChart";
            case JRChart.CHART_TYPE_XYAREA:
                return "xyAreaChart";
            case JRChart.CHART_TYPE_XYBAR:
                return "xyBarChart";
            case JRChart.CHART_TYPE_XYLINE:
                return "xyLineChart";
            case JRChart.CHART_TYPE_GANTT:
                return "ganttChart";
            default:
                return "chart (type=" + chartType + ")";
        }
    }

    // ========================= DOM-only (for Jaspersoft Studio properties)
    // =========================

    private String extractDataAdapterFromXml(InputStream xmlStream) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setNamespaceAware(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlStream);
            Element root = doc.getDocumentElement();

            NodeList properties = root.getElementsByTagName("property");
            for (int i = 0; i < properties.getLength(); i++) {
                Element prop = (Element) properties.item(i);
                if ("com.jaspersoft.studio.data.defaultdataadapter".equals(prop.getAttribute("name"))) {
                    return prop.getAttribute("value");
                }
            }
        } catch (Exception e) {
            // Fallback silently — data adapter is non-critical metadata
        }
        return null;
    }
}
