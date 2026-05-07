package com.Klex.reportingService.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_audit_log")
public class ReportAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Who & What ──
    private String userId;
    private String reportPath;
    private String sourceType;
    private String dataSourceType;
    private String outputFormat;
    private String outputPath;

    // ── Result ──
    private String status; // SUCCESS or ERROR

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    // ── JRL Design Metadata (extracted from KlexReport object) ──
    private String reportName; // klexReport.getName()
    private String queryLanguage; // klexReport.getQuery().getLanguage()
    private Integer fieldCount; // klexReport.getFields().length
    private Integer parameterCount; // user-defined params count
    private Integer columnCount; // klexReport.getColumnCount()
    private String orientation; // klexReport.getOrientationValue().name()
    private Integer pageWidth; // klexReport.getPageWidth()
    private Integer pageHeight; // klexReport.getPageHeight()

    // ── JRL Performance (per-phase timing) ──
    private Long compileTimeMs; // time to compile JRXML → KlexReport
    private Long fillTimeMs; // time to fill report with data
    private Long exportTimeMs; // time to export to output format
    private Long totalExecutionTimeMs; // wall-clock total

    // ── JRL Output Metrics (from KlexPrint) ──
    private Integer reportPages; // klexPrint.getPages().size()
    private Long outputFileSizeBytes; // output file size

    // ── Timestamp ──
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }

    // ── Getters & Setters ──

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getReportPath() {
        return reportPath;
    }

    public void setReportPath(String reportPath) {
        this.reportPath = reportPath;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getDataSourceType() {
        return dataSourceType;
    }

    public void setDataSourceType(String dataSourceType) {
        this.dataSourceType = dataSourceType;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    public String getQueryLanguage() {
        return queryLanguage;
    }

    public void setQueryLanguage(String queryLanguage) {
        this.queryLanguage = queryLanguage;
    }

    public Integer getFieldCount() {
        return fieldCount;
    }

    public void setFieldCount(Integer fieldCount) {
        this.fieldCount = fieldCount;
    }

    public Integer getParameterCount() {
        return parameterCount;
    }

    public void setParameterCount(Integer parameterCount) {
        this.parameterCount = parameterCount;
    }

    public Integer getColumnCount() {
        return columnCount;
    }

    public void setColumnCount(Integer columnCount) {
        this.columnCount = columnCount;
    }

    public String getOrientation() {
        return orientation;
    }

    public void setOrientation(String orientation) {
        this.orientation = orientation;
    }

    public Integer getPageWidth() {
        return pageWidth;
    }

    public void setPageWidth(Integer pageWidth) {
        this.pageWidth = pageWidth;
    }

    public Integer getPageHeight() {
        return pageHeight;
    }

    public void setPageHeight(Integer pageHeight) {
        this.pageHeight = pageHeight;
    }

    public Long getCompileTimeMs() {
        return compileTimeMs;
    }

    public void setCompileTimeMs(Long compileTimeMs) {
        this.compileTimeMs = compileTimeMs;
    }

    public Long getFillTimeMs() {
        return fillTimeMs;
    }

    public void setFillTimeMs(Long fillTimeMs) {
        this.fillTimeMs = fillTimeMs;
    }

    public Long getExportTimeMs() {
        return exportTimeMs;
    }

    public void setExportTimeMs(Long exportTimeMs) {
        this.exportTimeMs = exportTimeMs;
    }

    public Long getTotalExecutionTimeMs() {
        return totalExecutionTimeMs;
    }

    public void setTotalExecutionTimeMs(Long totalExecutionTimeMs) {
        this.totalExecutionTimeMs = totalExecutionTimeMs;
    }

    public Integer getReportPages() {
        return reportPages;
    }

    public void setReportPages(Integer reportPages) {
        this.reportPages = reportPages;
    }

    public Long getOutputFileSizeBytes() {
        return outputFileSizeBytes;
    }

    public void setOutputFileSizeBytes(Long outputFileSizeBytes) {
        this.outputFileSizeBytes = outputFileSizeBytes;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
