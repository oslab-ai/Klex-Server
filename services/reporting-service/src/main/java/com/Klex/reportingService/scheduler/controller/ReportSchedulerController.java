package com.Klex.reportingService.scheduler.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Klex.reportingService.scheduler.model.CalendarTrigger;
import com.Klex.reportingService.scheduler.model.ReportSchedule;
import com.Klex.reportingService.scheduler.model.SimpleTrigger;
import com.Klex.reportingService.scheduler.service.ReportSchedulerService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({ "/api/reports" })
@Tag(name = "Report Scheduler", description = "API for scheduling KlexReports")
public class ReportSchedulerController {

    @Autowired
    ReportSchedulerService schedulerService;

    @PostMapping(value = { "/scheduleReport" }, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Schedule a new report", description = "Creates a new scheduled report job with automatic trigger type detection. "
            +
            "Supports both Simple and Calendar triggers based on the request body.")
    public ResponseEntity<Map<String, Object>> scheduleReport(
            @Parameter(description = "Report schedule details with trigger configuration", required = true) @RequestBody ReportSchedule schedule)
            throws SchedulerException {

        try {
            // Auto-detect trigger type and create appropriate trigger if not provided
            if (schedule.getTrigger() == null) {
                schedule.setTrigger(createDefaultTrigger(schedule));
            } else {
                // Set timezone from schedule if not already set in trigger
                if (schedule.getOutputTimeZone() != null && schedule.getTrigger().getTimezone() == null) {
                    schedule.getTrigger().setTimezone(schedule.getOutputTimeZone());
                }
            }

            // Detect trigger type for logging and response
            String triggerType = schedule.getTrigger().getTriggerType();

            String jobId = schedulerService.scheduleReport(schedule);

            Map<String, Object> response = new HashMap<>();
            response.put("jobId", jobId);
            response.put("message", "Report scheduled successfully with " + triggerType + " Trigger");
            response.put("triggerType", triggerType);
            response.put("autoDetected", schedule.getTrigger() == null);
            response.put("status", "SUCCESS");

            // Add trigger configuration details to response
            response.put("triggerConfig", extractTriggerConfig(schedule.getTrigger()));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", "Failed to schedule report: " + e.getMessage());
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Create a default trigger based on the schedule configuration
     */
    private com.Klex.reportingService.scheduler.model.BaseTrigger createDefaultTrigger(ReportSchedule schedule) {
        SimpleTrigger defaultTrigger = new SimpleTrigger();
        defaultTrigger.setTimezone(schedule.getOutputTimeZone() != null ? schedule.getOutputTimeZone() : "UTC");
        defaultTrigger.setRecurrenceInterval(5);
        defaultTrigger.setRecurrenceIntervalUnit("MINUTE");
        defaultTrigger.setOccurrenceCount(3);
        defaultTrigger.setDescription("Default simple trigger");

        return defaultTrigger;
    }

    /**
     * Extract trigger configuration for response
     */
    private Map<String, Object> extractTriggerConfig(com.Klex.reportingService.scheduler.model.BaseTrigger trigger) {
        Map<String, Object> config = new HashMap<>();
        config.put("timezone", trigger.getTimezone());
        config.put("description", trigger.getDescription());

        if (trigger instanceof SimpleTrigger) {
            SimpleTrigger simpleTrigger = (SimpleTrigger) trigger;
            config.put("type", "SIMPLE");
            config.put("interval", simpleTrigger.getRecurrenceInterval());
            config.put("intervalUnit", simpleTrigger.getRecurrenceIntervalUnit());
            config.put("occurrenceCount", simpleTrigger.getOccurrenceCount());
            config.put("startDate", simpleTrigger.getStartDate());
            config.put("endDate", simpleTrigger.getEndDate());
        } else if (trigger instanceof CalendarTrigger) {
            CalendarTrigger calendarTrigger = (CalendarTrigger) trigger;
            config.put("type", "CALENDAR");
            config.put("cronExpression", calendarTrigger.getCronExpression());
            config.put("startTimeOfDay", calendarTrigger.getStartTimeOfDay());
            config.put("endTimeOfDay", calendarTrigger.getEndTimeOfDay());
            config.put("daysOfWeek", calendarTrigger.getDaysOfWeek());
            config.put("repeatInterval", calendarTrigger.getRepeatInterval());
            config.put("repeatIntervalUnit", calendarTrigger.getRepeatIntervalUnit());
            config.put("startDate", calendarTrigger.getStartDate());
            config.put("endDate", calendarTrigger.getEndDate());
        }

        return config;
    }

    @DeleteMapping(value = "/schedule/{jobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete a scheduled report", description = "Removes a scheduled report job")
    public ResponseEntity<Map<String, Object>> deleteSchedule(
            @Parameter(description = "Job ID of the scheduled report", required = true) @PathVariable String jobId)
            throws SchedulerException {

        try {
            schedulerService.deleteSchedule(jobId);

            Map<String, Object> response = new HashMap<>();
            response.put("jobId", jobId);
            response.put("message", "Schedule deleted successfully");
            response.put("status", "SUCCESS");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("jobId", jobId);
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", "Failed to delete schedule: " + e.getMessage());
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping(value = "/schedule/{jobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get schedule details", description = "Retrieves details of a scheduled report")
    public ResponseEntity<Map<String, Object>> getSchedule(
            @PathVariable String jobId) throws SchedulerException {

        try {
            ReportSchedule schedule = schedulerService.getSchedule(jobId);

            Map<String, Object> response = new HashMap<>();
            response.put("jobId", jobId);
            response.put("status", "SUCCESS");
            response.put("schedule", schedule);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("jobId", jobId);
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", "Failed to retrieve schedule: " + e.getMessage());
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping(value = "/schedules", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List all schedules", description = "Retrieves all scheduled reports with trigger metadata")
    public ResponseEntity<Map<String, Object>> listSchedules() throws SchedulerException {
        try {
            List<ReportSchedule> schedules = schedulerService.getAllSchedules();
            Map<String, Object> response = new HashMap<>();
            response.put("count", schedules.size());
            response.put("schedules", schedules);
            response.put("status", "SUCCESS");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", "Failed to list schedules: " + e.getMessage());
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
