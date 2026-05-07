package com.Klex.reportingService.dto;

import com.Klex.reportingService.service.InputSourceType;
import java.util.Map;

public class ReportRequest {
    private InputSourceType sourceType;
    private String path;
    private String outputPath;
    private String format;

    // User-supplied report parameters / filters
    private Map<String, String> parameters;

    // Data source fields
    private String dataSourceType; // "jdbc", "csv", "json", "xml", or "inmemory"
    private String jdbcUrl;
    private String jdbcUser;
    private String jdbcPassword;
    private String csvFilePath;
    private String jsonFilePath;
    private String xmlFilePath;
    private String xmlRecordPath; // XPath for record nodes, e.g. "/root/record"
    private String[] xmlFieldPaths; // Field names / XPath expressions for XML

    public InputSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(InputSourceType sourceType) {
        this.sourceType = sourceType;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getDataSourceType() {
        return dataSourceType;
    }

    public void setDataSourceType(String dataSourceType) {
        this.dataSourceType = dataSourceType;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getJdbcUser() {
        return jdbcUser;
    }

    public void setJdbcUser(String jdbcUser) {
        this.jdbcUser = jdbcUser;
    }

    public String getJdbcPassword() {
        return jdbcPassword;
    }

    public void setJdbcPassword(String jdbcPassword) {
        this.jdbcPassword = jdbcPassword;
    }

    public String getCsvFilePath() {
        return csvFilePath;
    }

    public void setCsvFilePath(String csvFilePath) {
        this.csvFilePath = csvFilePath;
    }

    public String getJsonFilePath() {
        return jsonFilePath;
    }

    public void setJsonFilePath(String jsonFilePath) {
        this.jsonFilePath = jsonFilePath;
    }

    public String getXmlFilePath() {
        return xmlFilePath;
    }

    public void setXmlFilePath(String xmlFilePath) {
        this.xmlFilePath = xmlFilePath;
    }

    public String getXmlRecordPath() {
        return xmlRecordPath;
    }

    public void setXmlRecordPath(String xmlRecordPath) {
        this.xmlRecordPath = xmlRecordPath;
    }

    public String[] getXmlFieldPaths() {
        return xmlFieldPaths;
    }

    public void setXmlFieldPaths(String[] xmlFieldPaths) {
        this.xmlFieldPaths = xmlFieldPaths;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }
}
