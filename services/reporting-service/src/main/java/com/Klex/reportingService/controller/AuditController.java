package com.Klex.reportingService.controller;

import com.Klex.reportingService.dto.AuditLogResponse;
import com.Klex.reportingService.service.AuditService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@Tag(name = "Audit & Logging", description = "Report Audit & Performance Tracking API")
public class AuditController {

    @Autowired
    private AuditService auditService;

    @GetMapping("/logs")
    @Operation(summary = "Get Audit Logs", description = "Returns all audit log entries. Optional filters: status, format, dataSourceType.")
    @ApiResponse(responseCode = "200", description = "Logs retrieved successfully")
    public ResponseEntity<List<AuditLogResponse>> getLogs(
            @Parameter(description = "Filter by status: SUCCESS or ERROR") @RequestParam(required = false) String status,
            @Parameter(description = "Filter by output format: PDF, XLSX, CSV, etc.") @RequestParam(required = false) String format,
            @Parameter(description = "Filter by data source type: jdbc, csv, json, xml, inmemory") @RequestParam(required = false) String dataSourceType) {

        List<AuditLogResponse> logs;

        if (status != null && !status.isEmpty()) {
            logs = auditService.getLogsByStatus(status);
        } else if (format != null && !format.isEmpty()) {
            logs = auditService.getLogsByFormat(format);
        } else if (dataSourceType != null && !dataSourceType.isEmpty()) {
            logs = auditService.getLogsByDataSourceType(dataSourceType);
        } else {
            logs = auditService.getAllLogs();
        }

        return ResponseEntity.ok(logs);
    }

    @GetMapping("/search")
    @Operation(summary = "Search Audit Logs", description = "Searches audit logs by a keyword in the report path (JRXML filename).")
    @ApiResponse(responseCode = "200", description = "Search results returned")
    public ResponseEntity<List<AuditLogResponse>> searchLogs(
            @Parameter(description = "Keyword to search in report path", required = true) @RequestParam String reportPath) {
        return ResponseEntity.ok(auditService.searchByReportPath(reportPath));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get Performance Statistics", description = "Returns aggregate performance statistics: total runs, success/error counts, "
            + "error rate, average compile/fill/export/total time, total data volume, and top 10 slowest reports.")
    @ApiResponse(responseCode = "200", description = "Statistics returned")
    public ResponseEntity<AuditLogResponse.PerformanceStats> getStats() {
        return ResponseEntity.ok(auditService.getPerformanceStats());
    }
}
