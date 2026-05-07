package com.Klex.reportingService.scheduler.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Klex.reportingService.scheduler.model.CalendarTrigger;
import com.Klex.reportingService.scheduler.model.ReportSchedule;
import com.Klex.reportingService.scheduler.model.SimpleTrigger;
import com.Klex.reportingService.scheduler.service.ReportSchedulerService;
import com.Klex.reportingService.scheduler.util.TriggerBuilderUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for users to create schedules with trigger configurations
 * Single API endpoint that automatically detects trigger type from request body
 */
@Slf4j
@RestController
@RequestMapping("/api/user-schedule")
public class UserTriggerController {

    private final ReportSchedulerService reportSchedulerService;

    public UserTriggerController(ReportSchedulerService reportSchedulerService) {
        this.reportSchedulerService = reportSchedulerService;
    }

    /**
     * Single API endpoint for creating schedules with automatic trigger type
     * detection
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createSchedule(@RequestBody ReportSchedule schedule) {
        try {
            // Validate required fields
            if (schedule.getReportUnitUri() == null || schedule.getReportUnitUri().trim().isEmpty()) {
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("error", "reportUnitUri is required");
                return ResponseEntity.badRequest().body(errorMap);
            }

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

            String jobId = reportSchedulerService.scheduleReport(schedule);

            Map<String, Object> response = new HashMap<>();
            response.put("jobId", jobId);
            response.put("message", "Schedule created successfully with " + triggerType + " Trigger");
            response.put("triggerType", triggerType);
            response.put("autoDetected", schedule.getTrigger() == null);

            // Add trigger configuration details to response
            response.put("triggerConfig", extractTriggerConfig(schedule.getTrigger()));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating schedule", e);
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorMap);
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

    /**
     * Get available trigger types and their configurations
     */
    @GetMapping("/trigger-types")
    public ResponseEntity<Map<String, Object>> getAvailableTriggerTypes() {
        Map<String, Object> response = new HashMap<>();

        // Simple Trigger configuration
        Map<String, Object> simpleTrigger = new HashMap<>();
        simpleTrigger.put("description", "Basic interval-based scheduling");
        simpleTrigger.put("class", "SimpleTrigger");
        simpleTrigger.put("fields", Arrays.asList(
                "timezone", "recurrenceInterval", "recurrenceIntervalUnit", "occurrenceCount",
                "startDate", "endDate", "description", "priority"));
        simpleTrigger.put("intervalUnits", Arrays.asList("MINUTE", "HOUR", "DAY", "WEEK", "MONTH", "YEAR"));

        Map<String, Object> simpleExample = new HashMap<>();
        simpleExample.put("recurrenceInterval", 30);
        simpleExample.put("recurrenceIntervalUnit", "MINUTE");
        simpleExample.put("occurrenceCount", 10);
        simpleExample.put("timezone", "UTC");
        simpleTrigger.put("example", simpleExample);

        // Calendar Trigger configuration
        Map<String, Object> calendarTrigger = new HashMap<>();
        calendarTrigger.put("description", "Calendar-aware scheduling with cron expressions");
        calendarTrigger.put("class", "CalendarTrigger");
        calendarTrigger.put("fields", Arrays.asList(
                "timezone", "cronExpression", "startTimeOfDay", "endTimeOfDay",
                "daysOfWeek", "repeatInterval", "repeatIntervalUnit",
                "startDate", "endDate", "description", "priority"));

        Map<String, Object> calendarExample = new HashMap<>();
        calendarExample.put("cronExpression", "0 0 9 * * ?");
        calendarExample.put("timezone", "UTC");
        calendarTrigger.put("example", calendarExample);

        response.put("SIMPLE", simpleTrigger);
        response.put("CALENDAR", calendarTrigger);

        return ResponseEntity.ok(response);
    }

    /**
     * Get utility methods for creating triggers
     */
    @GetMapping("/utility-methods")
    public ResponseEntity<Map<String, Object>> getUtilityMethods() {
        Map<String, Object> response = new HashMap<>();

        // Simple Trigger utilities
        Map<String, Object> simpleUtilities = new HashMap<>();
        simpleUtilities.put("everyMinutes", "TriggerBuilderUtil.everyMinutes(interval, count)");
        simpleUtilities.put("everyHours", "TriggerBuilderUtil.everyHours(interval, count)");
        simpleUtilities.put("everyDays", "TriggerBuilderUtil.everyDays(interval, count)");
        simpleUtilities.put("repeatForever", "TriggerBuilderUtil.repeatForever(interval, unit)");

        // Calendar Trigger utilities
        Map<String, Object> calendarUtilities = new HashMap<>();
        calendarUtilities.put("dailyAt", "TriggerBuilderUtil.dailyAt(time)");
        calendarUtilities.put("weeklyOn", "TriggerBuilderUtil.weeklyOn(dayOfWeek, time)");
        calendarUtilities.put("monthlyOn", "TriggerBuilderUtil.monthlyOn(dayOfMonth, time)");
        calendarUtilities.put("businessHours", "TriggerBuilderUtil.businessHours()");
        calendarUtilities.put("cronExpression", "TriggerBuilderUtil.cronExpression(cron)");

        response.put("SIMPLE", simpleUtilities);
        response.put("CALENDAR", calendarUtilities);

        return ResponseEntity.ok(response);
    }
}
