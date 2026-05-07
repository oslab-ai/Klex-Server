package com.Klex.reportingService.dto;

import java.util.List;

public class MetadataResponse {

    private String reportName;
    private int pageWidth;
    private int pageHeight;
    private int columnWidth;
    private int leftMargin;
    private int rightMargin;
    private int topMargin;
    private int bottomMargin;

    private String queryLanguage;
    private String queryText;
    private String dataAdapterName;

    private List<ParameterInfo> parameters;
    private List<FieldInfo> fields;
    private List<VariableInfo> variables;
    private List<BandInfo> bands;
    private List<String> subReports;
    private List<String> charts;

    // --- Inner classes ---

    public static class ParameterInfo {
        private String name;
        private String className;
        private String defaultValueExpression;
        private boolean forPrompting;

        public ParameterInfo() {
        }

        public ParameterInfo(String name, String className, String defaultValueExpression, boolean forPrompting) {
            this.name = name;
            this.className = className;
            this.defaultValueExpression = defaultValueExpression;
            this.forPrompting = forPrompting;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public String getDefaultValueExpression() {
            return defaultValueExpression;
        }

        public void setDefaultValueExpression(String defaultValueExpression) {
            this.defaultValueExpression = defaultValueExpression;
        }

        public boolean isForPrompting() {
            return forPrompting;
        }

        public void setForPrompting(boolean forPrompting) {
            this.forPrompting = forPrompting;
        }
    }

    public static class FieldInfo {
        private String name;
        private String className;
        private String description;

        public FieldInfo() {
        }

        public FieldInfo(String name, String className, String description) {
            this.name = name;
            this.className = className;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static class VariableInfo {
        private String name;
        private String className;
        private String calculation;
        private String expression;
        private String resetType;

        public VariableInfo() {
        }

        public VariableInfo(String name, String className, String calculation, String expression, String resetType) {
            this.name = name;
            this.className = className;
            this.calculation = calculation;
            this.expression = expression;
            this.resetType = resetType;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public String getCalculation() {
            return calculation;
        }

        public void setCalculation(String calculation) {
            this.calculation = calculation;
        }

        public String getExpression() {
            return expression;
        }

        public void setExpression(String expression) {
            this.expression = expression;
        }

        public String getResetType() {
            return resetType;
        }

        public void setResetType(String resetType) {
            this.resetType = resetType;
        }
    }

    public static class BandInfo {
        private String type;
        private int height;
        private int elementCount;

        public BandInfo() {
        }

        public BandInfo(String type, int height, int elementCount) {
            this.type = type;
            this.height = height;
            this.elementCount = elementCount;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getElementCount() {
            return elementCount;
        }

        public void setElementCount(int elementCount) {
            this.elementCount = elementCount;
        }
    }

    // --- Getters and Setters ---

    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    public int getPageWidth() {
        return pageWidth;
    }

    public void setPageWidth(int pageWidth) {
        this.pageWidth = pageWidth;
    }

    public int getPageHeight() {
        return pageHeight;
    }

    public void setPageHeight(int pageHeight) {
        this.pageHeight = pageHeight;
    }

    public int getColumnWidth() {
        return columnWidth;
    }

    public void setColumnWidth(int columnWidth) {
        this.columnWidth = columnWidth;
    }

    public int getLeftMargin() {
        return leftMargin;
    }

    public void setLeftMargin(int leftMargin) {
        this.leftMargin = leftMargin;
    }

    public int getRightMargin() {
        return rightMargin;
    }

    public void setRightMargin(int rightMargin) {
        this.rightMargin = rightMargin;
    }

    public int getTopMargin() {
        return topMargin;
    }

    public void setTopMargin(int topMargin) {
        this.topMargin = topMargin;
    }

    public int getBottomMargin() {
        return bottomMargin;
    }

    public void setBottomMargin(int bottomMargin) {
        this.bottomMargin = bottomMargin;
    }

    public String getQueryLanguage() {
        return queryLanguage;
    }

    public void setQueryLanguage(String queryLanguage) {
        this.queryLanguage = queryLanguage;
    }

    public String getQueryText() {
        return queryText;
    }

    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }

    public String getDataAdapterName() {
        return dataAdapterName;
    }

    public void setDataAdapterName(String dataAdapterName) {
        this.dataAdapterName = dataAdapterName;
    }

    public List<ParameterInfo> getParameters() {
        return parameters;
    }

    public void setParameters(List<ParameterInfo> parameters) {
        this.parameters = parameters;
    }

    public List<FieldInfo> getFields() {
        return fields;
    }

    public void setFields(List<FieldInfo> fields) {
        this.fields = fields;
    }

    public List<VariableInfo> getVariables() {
        return variables;
    }

    public void setVariables(List<VariableInfo> variables) {
        this.variables = variables;
    }

    public List<BandInfo> getBands() {
        return bands;
    }

    public void setBands(List<BandInfo> bands) {
        this.bands = bands;
    }

    public List<String> getSubReports() {
        return subReports;
    }

    public void setSubReports(List<String> subReports) {
        this.subReports = subReports;
    }

    public List<String> getCharts() {
        return charts;
    }

    public void setCharts(List<String> charts) {
        this.charts = charts;
    }
}
