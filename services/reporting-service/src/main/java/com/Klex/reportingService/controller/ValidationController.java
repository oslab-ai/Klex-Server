package com.Klex.reportingService.controller;

import com.Klex.reportingService.dto.ValidationRequest;
import com.Klex.reportingService.dto.ValidationResponse;
import com.Klex.reportingService.service.ParameterValidationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/validation")
@Tag(name = "Validation", description = "Parameter Validation API")
public class ValidationController {

    @Autowired
    private ParameterValidationService validationService;

    @PostMapping("/parameters")
    @Operation(summary = "Validate Parameters", description = "Validates user-supplied parameter values against the JRXML parameter definitions. "
            + "Checks type compatibility, required parameters, and flags unknown parameters.")
    @ApiResponse(responseCode = "200", description = "Validation result returned")
    @ApiResponse(responseCode = "400", description = "Bad request")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<?> validateParameters(@RequestBody ValidationRequest request) {
        try {
            ValidationResponse response = validationService.validate(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error validating parameters: " + e.getMessage());
        }
    }
}
