package com.Klex.reportingService.dto;

import com.Klex.reportingService.service.InputSourceType;

import java.util.Map;

public class ValidationRequest {
    private InputSourceType sourceType;
    private String path;
    private Map<String, String> parameters; // parameter name → user-supplied value (as string)

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

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }
}
