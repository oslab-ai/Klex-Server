package com.Klex.reportingService.service;

import com.Klex.reportingService.dto.ValidationRequest;
import com.Klex.reportingService.dto.ValidationResponse;
import com.Klex.reportingService.dto.ValidationResponse.ParameterDetail;
import net.sf.jasperreports.engine.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class ParameterValidationService {

    @Autowired
    private List<InputSourceService> inputSourceServices;

    // Common date formats to try when validating java.util.Date
    private static final String[] DATE_FORMATS = {
            "yyyy-MM-dd", "yyyy-MM-dd'T'HH:mm:ss", "MM/dd/yyyy", "dd/MM/yyyy",
            "yyyy-MM-dd HH:mm:ss", "dd-MM-yyyy"
    };

    /**
     * Validate user-supplied parameter values against the JRXML parameter
     * definitions.
     *
     * 1. Compile JRXML via KlexCompileManager
     * 2. Extract JRParameter[] (skip system parameters)
     * 3. For each user parameter: validate type compatibility
     * 4. Check for missing required parameters (forPrompting=true, no default, not
     * supplied)
     */
    public ValidationResponse validate(ValidationRequest request) throws Exception {
        InputSourceType sourceType = request.getSourceType();
        String path = request.getPath();
        Map<String, String> suppliedParams = request.getParameters();
        if (suppliedParams == null) {
            suppliedParams = Collections.emptyMap();
        }

        // Resolve input source
        InputSourceService selectedService = null;
        for (InputSourceService service : inputSourceServices) {
            if (service.getSourceType() == sourceType) {
                selectedService = service;
                break;
            }
        }
        if (selectedService == null) {
            throw new IllegalArgumentException("No service found for source type: " + sourceType);
        }

        // Compile JRXML via JRL
        JasperReport jasperReport;
        try (InputStream inputStream = selectedService.getInputStream(path)) {
            jasperReport = JasperCompileManager.compileReport(inputStream);
        }

        // Extract user-defined parameters
        JRParameter[] allParams = jasperReport.getParameters();
        List<JRParameter> userParams = new ArrayList<>();
        if (allParams != null) {
            for (JRParameter p : allParams) {
                if (!p.isSystemDefined()) {
                    userParams.add(p);
                }
            }
        }

        // Validate
        List<ParameterDetail> details = new ArrayList<>();
        List<String> missingRequired = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        boolean allValid = true;

        for (JRParameter param : userParams) {
            String name = param.getName();
            String expectedType = param.getValueClassName();
            boolean forPrompting = param.isForPrompting();

            String defaultExpr = null;
            if (param.getDefaultValueExpression() != null) {
                defaultExpr = param.getDefaultValueExpression().getText();
            }

            String suppliedValue = suppliedParams.get(name);

            // Check if required but missing
            if (suppliedValue == null || suppliedValue.trim().isEmpty()) {
                if (forPrompting && defaultExpr == null) {
                    // Required and no default — this is missing
                    missingRequired.add(name);
                    errors.add("Parameter '" + name
                            + "' is required (forPrompting=true) but not supplied and has no default");
                    details.add(new ParameterDetail(name, expectedType, null, false, null, forPrompting,
                            "Required parameter not supplied"));
                    allValid = false;
                } else {
                    // Not required or has default — OK
                    details.add(new ParameterDetail(name, expectedType, null, true, defaultExpr, forPrompting, null));
                }
                continue;
            }

            // Validate type compatibility
            String typeError = validateType(expectedType, suppliedValue);
            if (typeError != null) {
                errors.add("Parameter '" + name + "': " + typeError);
                details.add(new ParameterDetail(name, expectedType, suppliedValue, false, defaultExpr, forPrompting,
                        typeError));
                allValid = false;
            } else {
                details.add(
                        new ParameterDetail(name, expectedType, suppliedValue, true, defaultExpr, forPrompting, null));
            }
        }

        // Check for unknown parameters (supplied but not declared in JRXML)
        Set<String> declaredNames = new HashSet<>();
        for (JRParameter p : userParams) {
            declaredNames.add(p.getName());
        }
        for (String suppliedKey : suppliedParams.keySet()) {
            if (!declaredNames.contains(suppliedKey)) {
                errors.add("Parameter '" + suppliedKey + "' is not declared in the JRXML");
                details.add(new ParameterDetail(suppliedKey, null, suppliedParams.get(suppliedKey),
                        false, null, false, "Unknown parameter — not declared in JRXML"));
                allValid = false;
            }
        }

        ValidationResponse response = new ValidationResponse();
        response.setValid(allValid);
        response.setParameterDetails(details);
        response.setMissingRequired(missingRequired);
        response.setErrors(errors);
        return response;
    }

    /**
     * Validate that a string value can be converted to the expected Java type.
     * Returns null if valid, or an error message if invalid.
     */
    private String validateType(String expectedType, String value) {
        if (expectedType == null) {
            return null; // No type constraint
        }

        try {
            switch (expectedType) {
                case "java.lang.String":
                    return null; // Always valid

                case "java.lang.Integer":
                case "int":
                    Integer.parseInt(value);
                    return null;

                case "java.lang.Long":
                case "long":
                    Long.parseLong(value);
                    return null;

                case "java.lang.Double":
                case "double":
                    Double.parseDouble(value);
                    return null;

                case "java.lang.Float":
                case "float":
                    Float.parseFloat(value);
                    return null;

                case "java.lang.Short":
                case "short":
                    Short.parseShort(value);
                    return null;

                case "java.lang.Byte":
                case "byte":
                    Byte.parseByte(value);
                    return null;

                case "java.lang.Boolean":
                case "boolean":
                    if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                        return null;
                    }
                    return "Expected boolean (true/false), got '" + value + "'";

                case "java.math.BigDecimal":
                    new BigDecimal(value);
                    return null;

                case "java.util.Date":
                case "java.sql.Date":
                case "java.sql.Timestamp":
                    return validateDate(value);

                default:
                    // For complex types (List, Map, custom classes), we can only check non-null
                    return null;
            }
        } catch (NumberFormatException e) {
            return "Cannot convert '" + value + "' to " + expectedType;
        }
    }

    private String validateDate(String value) {
        for (String format : DATE_FORMATS) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                sdf.setLenient(false);
                sdf.parse(value);
                return null; // Successfully parsed
            } catch (Exception ignored) {
            }
        }
        return "Cannot parse '" + value + "' as a date. Supported formats: "
                + Arrays.toString(DATE_FORMATS);
    }
}
