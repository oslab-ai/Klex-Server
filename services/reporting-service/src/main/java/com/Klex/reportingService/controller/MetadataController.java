package com.Klex.reportingService.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Klex.reportingService.dto.MetadataRequest;
import com.Klex.reportingService.dto.MetadataResponse;
import com.Klex.reportingService.service.MetadataService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/metadata")
@Tag(name = "Metadata", description = "JRXML Metadata Extraction API")
public class MetadataController {

    @Autowired
    private MetadataService metadataService;

    @PostMapping("/extract")
    @Operation(summary = "Extract Metadata", description = "Extracts structural metadata (parameters, fields, query, bands, charts, sub-reports, etc.) from a JRXML file.")
    @ApiResponse(responseCode = "200", description = "Metadata extracted successfully")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<?> extractMetadata(@RequestBody MetadataRequest request) {
        try {
            MetadataResponse response = metadataService.extractMetadata(
                    request.getSourceType(), request.getPath());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error extracting metadata: " + e.getMessage());
        }
    }
}
