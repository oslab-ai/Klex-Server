package com.Klex.reportingService.dto;

import com.Klex.reportingService.entity.ReportAuditLog;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for returning audit log information via the REST API.
 */
public class AuditLogResponse {

    private Long id;
    private String userId;
    private String reportPath;
    private String sourceType;
    private String dataSourceType;
    private String outputFormat;
    private String outputPath;
    private String status;
    private String errorMessage;

    // JRL design metadata
    private String reportName;
    private String queryLanguage;
    private Integer fieldCount;
    private Integer parameterCount;
    private Integer columnCount;
    private String orientation;
    private Integer pageWidth;
    private Integer pageHeight;

    // JRL per-phase performance
    private Long compileTimeMs;
    private Long fillTimeMs;
    private Long exportTimeMs;
    private Long totalExecutionTimeMs;

    // JRL output metrics
    private Integer reportPages;
    private Long outputFileSizeBytes;

    private LocalDateTime timestamp;

    /** Factory: convert entity → DTO */
    public static AuditLogResponse fromEntity(ReportAuditLog entity) {
        AuditLogResponse dto = new AuditLogResponse();
        dto.id = entity.getId();
        dto.userId = entity.getUserId();
        dto.reportPath = entity.getReportPath();
        dto.sourceType = entity.getSourceType();
        dto.dataSourceType = entity.getDataSourceType();
        dto.outputFormat = entity.getOutputFormat();
        dto.outputPath = entity.getOutputPath();
        dto.status = entity.getStatus();
        dto.errorMessage = entity.getErrorMessage();
        dto.reportName = entity.getReportName();
        dto.queryLanguage = entity.getQueryLanguage();
        dto.fieldCount = entity.getFieldCount();
        dto.parameterCount = entity.getParameterCount();
        dto.columnCount = entity.getColumnCount();
        dto.orientation = entity.getOrientation();
        dto.pageWidth = entity.getPageWidth();
        dto.pageHeight = entity.getPageHeight();
        dto.compileTimeMs = entity.getCompileTimeMs();
        dto.fillTimeMs = entity.getFillTimeMs();
        dto.exportTimeMs = entity.getExportTimeMs();
        dto.totalExecutionTimeMs = entity.getTotalExecutionTimeMs();
        dto.reportPages = entity.getReportPages();
        dto.outputFileSizeBytes = entity.getOutputFileSizeBytes();
        dto.timestamp = entity.getTimestamp();
        return dto;
    }

    /** Factory: convert list of entities → list of DTOs */
    public static List<AuditLogResponse> fromEntities(List<ReportAuditLog> entities) {
        List<AuditLogResponse> list = new ArrayList<>();
        for (ReportAuditLog entity : entities) {
            list.add(fromEntity(entity));
        }
        return list;
    }

    // ── Getters ──

    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getReportPath() {
        return reportPath;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getDataSourceType() {
        return dataSourceType;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public String getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getReportName() {
        return reportName;
    }

    public String getQueryLanguage() {
        return queryLanguage;
    }

    public Integer getFieldCount() {
        return fieldCount;
    }

    public Integer getParameterCount() {
        return parameterCount;
    }

    public Integer getColumnCount() {
        return columnCount;
    }

    public String getOrientation() {
        return orientation;
    }

    public Integer getPageWidth() {
        return pageWidth;
    }

    public Integer getPageHeight() {
        return pageHeight;
    }

    public Long getCompileTimeMs() {
        return compileTimeMs;
    }

    public Long getFillTimeMs() {
        return fillTimeMs;
    }

    public Long getExportTimeMs() {
        return exportTimeMs;
    }

    public Long getTotalExecutionTimeMs() {
        return totalExecutionTimeMs;
    }

    public Integer getReportPages() {
        return reportPages;
    }

    public Long getOutputFileSizeBytes() {
        return outputFileSizeBytes;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    // ── Nested DTO: aggregate performance stats ──

    public static class PerformanceStats {
        private long totalReports;
        private long successCount;
        private long errorCount;
        private double errorRate;
        private Double avgTotalExecutionTimeMs;
        private Double avgCompileTimeMs;
        private Double avgFillTimeMs;
        private Double avgExportTimeMs;
        private long totalDataVolumeBytes;
        private List<AuditLogResponse> slowestReports;

        public long getTotalReports() {
            return totalReports;
        }

        public void setTotalReports(long totalReports) {
            this.totalReports = totalReports;
        }

        public long getSuccessCount() {
            return successCount;
        }

        public void setSuccessCount(long successCount) {
            this.successCount = successCount;
        }

        public long getErrorCount() {
            return errorCount;
        }

        public void setErrorCount(long errorCount) {
            this.errorCount = errorCount;
        }

        public double getErrorRate() {
            return errorRate;
        }

        public void setErrorRate(double errorRate) {
            this.errorRate = errorRate;
        }

        public Double getAvgTotalExecutionTimeMs() {
            return avgTotalExecutionTimeMs;
        }

        public void setAvgTotalExecutionTimeMs(Double avgTotalExecutionTimeMs) {
            this.avgTotalExecutionTimeMs = avgTotalExecutionTimeMs;
        }

        public Double getAvgCompileTimeMs() {
            return avgCompileTimeMs;
        }

        public void setAvgCompileTimeMs(Double avgCompileTimeMs) {
            this.avgCompileTimeMs = avgCompileTimeMs;
        }

        public Double getAvgFillTimeMs() {
            return avgFillTimeMs;
        }

        public void setAvgFillTimeMs(Double avgFillTimeMs) {
            this.avgFillTimeMs = avgFillTimeMs;
        }

        public Double getAvgExportTimeMs() {
            return avgExportTimeMs;
        }

        public void setAvgExportTimeMs(Double avgExportTimeMs) {
            this.avgExportTimeMs = avgExportTimeMs;
        }

        public long getTotalDataVolumeBytes() {
            return totalDataVolumeBytes;
        }

        public void setTotalDataVolumeBytes(long totalDataVolumeBytes) {
            this.totalDataVolumeBytes = totalDataVolumeBytes;
        }

        public List<AuditLogResponse> getSlowestReports() {
            return slowestReports;
        }

        public void setSlowestReports(List<AuditLogResponse> slowestReports) {
            this.slowestReports = slowestReports;
        }
    }
}
