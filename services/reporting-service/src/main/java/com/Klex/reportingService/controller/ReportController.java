package com.Klex.reportingService.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Klex.reportingService.dto.ReportRequest;
import com.Klex.reportingService.service.ReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports", description = "Report Generation API")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @PostMapping("/generate")
    @Operation(summary = "Generate Report", description = "Generates a report based on the provided request parameters.")
    @ApiResponse(responseCode = "200", description = "Report generated successfully")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<String> generateReport(@RequestBody ReportRequest request) {
        String result = reportService.generateReport(request);
        if (result.startsWith("Error")) {
            return ResponseEntity.internalServerError().body(result);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload Test File", description = "Upload a local file (CSV, JSON, JRXML) from your device. Returns the server path to be used in the /generate payload.")
    public ResponseEntity<String> uploadTestFile(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            java.io.File tempFile = java.io.File.createTempFile("Klex_upload_", "_" + file.getOriginalFilename());
            file.transferTo(tempFile);
            return ResponseEntity.ok(tempFile.getAbsolutePath());
        } catch (java.io.IOException e) {
            return ResponseEntity.internalServerError().body("Error saving file: " + e.getMessage());
        }
    }
}
