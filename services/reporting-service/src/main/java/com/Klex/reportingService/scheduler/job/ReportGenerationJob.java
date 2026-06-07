package com.Klex.reportingService.scheduler.job;

import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.Klex.reportingService.scheduler.model.OutputFormat;
import com.Klex.reportingService.scheduler.service.ReportDeliveryService;
import com.Klex.reportingService.service.GoogleDriveDeliveryService;
import com.Klex.reportingService.service.ReportService;
import com.Klex.reportingService.dto.ReportRequest;
import com.Klex.reportingService.service.InputSourceType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

@Slf4j
@Component
public class ReportGenerationJob implements Job {

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportDeliveryService reportDeliveryService;

    @Autowired
    private GoogleDriveDeliveryService googleDriveDeliveryService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            String reportUnitUriStr = context.getMergedJobDataMap().getString("reportUnitUri");
            if (reportUnitUriStr == null || reportUnitUriStr.trim().isEmpty()) {
                throw new JobExecutionException("No reportUnitUri specified.");
            }

            // Resolve per-report output formats (report URI → list of formats)
            String reportFormatsJson = context.getMergedJobDataMap().getString("reportFormatsJson");
            Map<String, List<String>> reportOutputFormats = new HashMap<>();
            if (reportFormatsJson != null && !reportFormatsJson.isEmpty()) {
                try {
                    reportOutputFormats = objectMapper.readValue(reportFormatsJson,
                            new TypeReference<Map<String, List<String>>>() {});
                } catch (Exception e) {
                    log.warn("Failed to deserialize reportOutputFormats, falling back to global format", e);
                }
            }

            String globalOutputFormatStr = context.getMergedJobDataMap().getString("outputFormat");
            if (globalOutputFormatStr == null) {
                String listStr = context.getMergedJobDataMap().getString("outputFormats");
                if (listStr != null && !listStr.isEmpty()) {
                    globalOutputFormatStr = listStr.split(",")[0];
                } else {
                    globalOutputFormatStr = "PDF";
                }
            }
            String deliveryMethod = context.getMergedJobDataMap().getString("deliveryMethod");
            String emailTo = context.getMergedJobDataMap().getString("emailTo");

            // Reconstruct dataAdapter map from JSON string in JobDataMap
            String dataAdapterJson = context.getMergedJobDataMap().getString("dataAdapter");
            Map<String, Object> dataAdapter = null;
            if (dataAdapterJson != null) {
                dataAdapter = objectMapper.readValue(dataAdapterJson, new TypeReference<Map<String, Object>>() {
                });
            }

            String[] reportUnitUris = reportUnitUriStr.split(",");
            List<byte[]> allReportBytes = new ArrayList<>();
            List<String> allFileNames = new ArrayList<>();

            for (String uri : reportUnitUris) {
                uri = uri.trim();
                if (uri.isEmpty()) continue;

                // Resolve formats: per-report format list if available, otherwise global
                List<String> formatsForThisReport = reportOutputFormats.getOrDefault(uri,
                        Collections.singletonList(globalOutputFormatStr));

                for (String formatStr : formatsForThisReport) {
                    OutputFormat outputFormat = OutputFormat.valueOf(formatStr);

                    log.info("Generating report: {} with format: {}", uri, formatStr);

                    // Create a temporary file to store the generated report
                    File tempOutputFile = File.createTempFile("report_" + context.getFireInstanceId() + "_" + uri.replaceAll("[^a-zA-Z0-9]", "_"),
                            "." + formatStr.toLowerCase());
                    tempOutputFile.deleteOnExit();

                    // Build ReportRequest for ReportService
                    ReportRequest request = new ReportRequest();
                    request.setSourceType(InputSourceType.LOCAL);
                    request.setPath(uri);
                    request.setFormat(formatStr);
                    request.setOutputPath(tempOutputFile.getAbsolutePath());

                    if (dataAdapter != null) {
                        request.setDataSourceType((String) dataAdapter.get("dataSourceType"));
                        request.setJdbcUrl((String) dataAdapter.get("jdbcUrl"));
                        request.setJdbcUser((String) dataAdapter.get("jdbcUser"));
                        request.setJdbcPassword((String) dataAdapter.get("jdbcPassword"));
                        request.setCsvFilePath((String) dataAdapter.get("csvFilePath"));
                        request.setJsonFilePath((String) dataAdapter.get("jsonFilePath"));
                        request.setXmlFilePath((String) dataAdapter.get("xmlFilePath"));
                        request.setXmlRecordPath((String) dataAdapter.get("xmlRecordPath"));
                    } else {
                        request.setDataSourceType("inmemory");
                    }

                    // Generate the report via ReportService
                    String result = reportService.generateReport(request);
                    if (result.startsWith("Error")) {
                        tempOutputFile.delete();
                        throw new JobExecutionException("Report generation failed for URI " + uri + ": " + result);
                    }

                    // Read the generated file bytes
                    byte[] reportBytes = Files.readAllBytes(tempOutputFile.toPath());
                    allReportBytes.add(reportBytes);

                    // Construct clean filename
                    String baseName = uri.substring(uri.lastIndexOf('/') + 1);
                    String fileName = baseName + "_" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) +
                            "." + formatStr.toLowerCase();
                    allFileNames.add(fileName);

                    // Clean up
                    tempOutputFile.delete();
                }
            }

            if (!allReportBytes.isEmpty()) {
                if ("EMAIL".equals(deliveryMethod)) {
                    reportDeliveryService.sendEmailWithMultipleAttachments(allReportBytes, allFileNames, emailTo);
                } else if ("GOOGLE_DRIVE".equals(deliveryMethod)) {
                    String driveFolderId = context.getMergedJobDataMap().getString("driveFolderId");
                    for (int i = 0; i < allReportBytes.size(); i++) {
                        googleDriveDeliveryService.uploadReport(allReportBytes.get(i), allFileNames.get(i), driveFolderId);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error executing report generation job", e);
            throw new JobExecutionException(e);
        }
    }
}
