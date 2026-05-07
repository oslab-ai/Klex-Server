package com.Klex.reportingService.dto;

import java.util.List;

public class ValidationResponse {

    private boolean valid;
    private List<ParameterDetail> parameterDetails;
    private List<String> missingRequired;
    private List<String> errors;

    // --- Inner class ---

    public static class ParameterDetail {
        private String name;
        private String expectedType;
        private String suppliedValue;
        private boolean valid;
        private String defaultValue;
        private boolean forPrompting;
        private String error;

        public ParameterDetail() {
        }

        public ParameterDetail(String name, String expectedType, String suppliedValue,
                boolean valid, String defaultValue, boolean forPrompting, String error) {
            this.name = name;
            this.expectedType = expectedType;
            this.suppliedValue = suppliedValue;
            this.valid = valid;
            this.defaultValue = defaultValue;
            this.forPrompting = forPrompting;
            this.error = error;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getExpectedType() {
            return expectedType;
        }

        public void setExpectedType(String expectedType) {
            this.expectedType = expectedType;
        }

        public String getSuppliedValue() {
            return suppliedValue;
        }

        public void setSuppliedValue(String suppliedValue) {
            this.suppliedValue = suppliedValue;
        }

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public boolean isForPrompting() {
            return forPrompting;
        }

        public void setForPrompting(boolean forPrompting) {
            this.forPrompting = forPrompting;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }

    // --- Getters and Setters ---

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<ParameterDetail> getParameterDetails() {
        return parameterDetails;
    }

    public void setParameterDetails(List<ParameterDetail> parameterDetails) {
        this.parameterDetails = parameterDetails;
    }

    public List<String> getMissingRequired() {
        return missingRequired;
    }

    public void setMissingRequired(List<String> missingRequired) {
        this.missingRequired = missingRequired;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
