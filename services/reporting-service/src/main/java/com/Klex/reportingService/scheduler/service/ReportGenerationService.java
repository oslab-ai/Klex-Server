package com.Klex.reportingService.scheduler.service;

import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.Klex.reportingService.scheduler.model.OutputFormat;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.data.JRCsvDataSource;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperExportManager;

@Slf4j
@Service("schedulerReportGenerationService")
public class ReportGenerationService {

    @Value("${app.report.datasource.path:/home/mayank/Documents/dollarstore/KlexReportingService/src/test/Customers.csv}")
    private String csvDataSourcePath;

    @Value("${app.report.template.path:/home/mayank/Documents/dollarstore/KlexReportingService/src/main/resources/Customers.jrxml}")
    private String reportTemplatePath;

    public byte[] generateReport(String reportUnitUri, Map<String, Object> parameters, OutputFormat outputFormat)
            throws JRException {
        log.info("Loading report from: {}", reportUnitUri);

        JasperReport jasperReport = JasperCompileManager.compileReport(reportTemplatePath);

        String[] columnNames = new String[] { "Customer Name", "Province", "Region", "Customer Segment" };
        JRCsvDataSource ds = new JRCsvDataSource(JRLoader.getLocationInputStream(csvDataSourcePath));
        ds.setRecordDelimiter("\r\n");
        ds.setFieldDelimiter(';');
        ds.setUseFirstRowAsHeader(true);
        ds.setColumnNames(columnNames);

        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, null, ds);
        log.info("Jasper Report Generated Successfully!");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        switch (outputFormat) {
            case PDF:
                JRPdfExporter pdfExporter = new JRPdfExporter();
                pdfExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                pdfExporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
                pdfExporter.exportReport();
                break;
            case CSV:
                JRCsvExporter csvExporter = new JRCsvExporter();
                csvExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                csvExporter.setExporterOutput(new SimpleWriterExporterOutput(outputStream));
                csvExporter.exportReport();
                break;
            case XLS:
                JRXlsExporter xlsExporter = new JRXlsExporter();
                xlsExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                xlsExporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
                xlsExporter.setConfiguration(xlsConfiguration());
                xlsExporter.exportReport();
                break;
            case XLSX:
                JRXlsxExporter xlsxExporter = new JRXlsxExporter();
                xlsxExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                xlsxExporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
                xlsxExporter.setConfiguration(xlsxConfiguration());
                xlsxExporter.exportReport();
                break;
            default:
                throw new IllegalArgumentException("Unsupported output format: " + outputFormat);
        }

        return outputStream.toByteArray();
    }

    private SimpleXlsReportConfiguration xlsConfiguration() {
        SimpleXlsReportConfiguration cfg = new SimpleXlsReportConfiguration();
        cfg.setDetectCellType(true);
        cfg.setOnePagePerSheet(false);
        cfg.setRemoveEmptySpaceBetweenRows(true);
        cfg.setWhitePageBackground(false);
        return cfg;
    }

    private SimpleXlsxReportConfiguration xlsxConfiguration() {
        SimpleXlsxReportConfiguration cfg = new SimpleXlsxReportConfiguration();
        cfg.setDetectCellType(true);
        cfg.setOnePagePerSheet(false);
        cfg.setRemoveEmptySpaceBetweenRows(true);
        cfg.setWhitePageBackground(false);
        return cfg;
    }
}
