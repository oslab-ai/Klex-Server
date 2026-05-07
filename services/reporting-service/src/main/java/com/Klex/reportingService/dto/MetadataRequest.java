package com.Klex.reportingService.dto;

import com.Klex.reportingService.service.InputSourceType;

public class MetadataRequest {
    private InputSourceType sourceType;
    private String path;

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
}
