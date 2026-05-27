package com.Klex.reportingService.scheduler.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Klex.reportingService.scheduler.model.CalendarTrigger;
import com.Klex.reportingService.scheduler.model.ReportSchedule;
import com.Klex.reportingService.scheduler.model.SimpleTrigger;
import com.Klex.reportingService.scheduler.service.ReportSchedulerService;
import com.Klex.reportingService.scheduler.util.TriggerBuilderUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Example controller demonstrating how to use both Simple and Calendar triggers
 * This shows different ways to schedule reports with various trigger types
 */
@Slf4j
@RestController
@RequestMapping("/api/examples")
public class TriggerExampleController {

    private final ReportSchedulerService reportSchedulerService;

    public TriggerExampleController(ReportSchedulerService reportSchedulerService) {
        this.reportSchedulerService = reportSchedulerService;
    }

    /**
     * Example 1: Simple Trigger - Run every 5 minutes, 10 times
     */
    @PostMapping("/simple-trigger")
    public ResponseEntity<Map<String, String>> scheduleWithSimpleTrigger() {
        try {
            ReportSchedule schedule = createBasicReportSchedule();

            SimpleTrigger trigger = TriggerBuilderUtil.everyMinutes(5, 10);
            trigger.setTimezone("America/New_York");
            trigger.setDescription("Run every 5 minutes, 10 times");

            schedule.setTrigger(trigger);

            String jobId = reportSchedulerService.scheduleReport(schedule);

            Map<String, String> response = new HashMap<>();
            response.put("jobId", jobId);
            response.put("message", "Scheduled with Simple Trigger - every 5 minutes, 10 times");
            response.put("triggerType", "SIMPLE");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error scheduling with simple trigger", e);
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorMap);
        }
    }

    /**
     * Example 2: Simple Trigger - Run every hour, forever
     */
    @PostMapping("/simple-trigger-forever")
    public ResponseEntity<Map<String, String>> scheduleWithSimpleTriggerForever() {
        try {
            ReportSchedule schedule = createBasicReportSchedule();

            SimpleTrigger trigger = TriggerBuilderUtil.repeatForever(1, "HOUR");
            trigger.setTimezone("UTC");
            trigger.setDescription("Run every hour, forever");

            schedule.setTrigger(trigger);

            String jobId = reportSchedulerService.scheduleReport(schedule);

            Map<String, String> response = new HashMap<>();
            response.put("jobId", jobId);
            response.put("message", "Scheduled with Simple Trigger - every hour, forever");
            response.put("triggerType", "SIMPLE");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error scheduling with simple trigger forever", e);
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorMap);
        }
    }

    /**
     * Example 3: Calendar Trigger - Run daily at 9:00 AM
     */
    @PostMapping("/calendar-trigger-daily")
    public ResponseEntity<Map<String, String>> scheduleWithCalendarTriggerDaily() {
        try {
            ReportSchedule schedule = createBasicReportSchedule();

            CalendarTrigger trigger = TriggerBuilderUtil.dailyAt("09:00:00");
            trigger.setTimezone("America/Los_Angeles");
            trigger.setDescription("Run daily at 9:00 AM");

            schedule.setTrigger(trigger);

            String jobId = reportSchedulerService.scheduleReport(schedule);

            Map<String, String> response = new HashMap<>();
            response.put("jobId", jobId);
            response.put("message", "Scheduled with Calendar Trigger - daily at 9:00 AM");
            response.put("triggerType", "CALENDAR");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error scheduling with calendar trigger daily", e);
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorMap);
        }
    }

    /**
     * Example 4: Calendar Trigger - Run weekly on Monday at 8:00 AM
     */
    @PostMapping("/calendar-trigger-weekly")
    public ResponseEntity<Map<String, String>> scheduleWithCalendarTriggerWeekly() {
        try {
            ReportSchedule schedule = createBasicReportSchedule();

            CalendarTrigger trigger = TriggerBuilderUtil.weeklyOn(2, "08:00:00");
            trigger.setTimezone("America/Chicago");
            trigger.setDescription("Run weekly on Monday at 8:00 AM");

            schedule.setTrigger(trigger);

            String jobId = reportSchedulerService.scheduleReport(schedule);

            Map<String, String> response = new HashMap<>();
            response.put("jobId", jobId);
            response.put("message", "Scheduled with Calendar Trigger - weekly on Monday at 8:00 AM");
            response.put("triggerType", "CALENDAR");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error scheduling with calendar trigger weekly", e);
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorMap);
        }
    }

    /**
     * Example 5: Calendar Trigger - Business hours (9 AM - 5 PM, every hour)
     */
    @PostMapping("/calendar-trigger-business-hours")
    public ResponseEntity<Map<String, String>> scheduleWithCalendarTriggerBusinessHours() {
        try {
            ReportSchedule schedule = createBasicReportSchedule();

            CalendarTrigger trigger = TriggerBuilderUtil.businessHoursWithInterval("09:00:00", "17:00:00", 1, "HOUR");
            trigger.setTimezone("America/New_York");
            trigger.setDescription("Run during business hours, every hour");

            schedule.setTrigger(trigger);

            String jobId = reportSchedulerService.scheduleReport(schedule);

            Map<String, String> response = new HashMap<>();
            response.put("jobId", jobId);
            response.put("message", "Scheduled with Calendar Trigger - business hours, every hour");
            response.put("triggerType", "CALENDAR");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error scheduling with calendar trigger business hours", e);
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorMap);
        }
    }

    /**
     * Example 6: Calendar Trigger - Custom Cron expression
     */
    @PostMapping("/calendar-trigger-cron")
    public ResponseEntity<Map<String, String>> scheduleWithCalendarTriggerCron() {
        try {
            ReportSchedule schedule = createBasicReportSchedule();

            String cronExpression = "0 */15 * ? * MON-FRI";
            CalendarTrigger trigger = TriggerBuilderUtil.cronExpression(cronExpression);
            trigger.setTimezone("UTC");
            trigger.setDescription("Run every 15 minutes on weekdays");

            schedule.setTrigger(trigger);

            String jobId = reportSchedulerService.scheduleReport(schedule);

            Map<String, String> response = new HashMap<>();
            response.put("jobId", jobId);
            response.put("message", "Scheduled with Calendar Trigger - every 15 minutes on weekdays");
            response.put("triggerType", "CALENDAR");
            response.put("cronExpression", cronExpression);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error scheduling with calendar trigger cron", e);
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorMap);
        }
    }

    /**
     * Example 7: Calendar Trigger - Weekend schedule
     */
    @PostMapping("/calendar-trigger-weekend")
    public ResponseEntity<Map<String, String>> scheduleWithCalendarTriggerWeekend() {
        try {
            ReportSchedule schedule = createBasicReportSchedule();

            CalendarTrigger trigger = TriggerBuilderUtil.weekendSchedule("10:00:00", 2, "HOUR");
            trigger.setTimezone("America/Denver");
            trigger.setDescription("Run on weekends, every 2 hours starting at 10 AM");

            schedule.setTrigger(trigger);

            String jobId = reportSchedulerService.scheduleReport(schedule);

            Map<String, String> response = new HashMap<>();
            response.put("jobId", jobId);
            response.put("message", "Scheduled with Calendar Trigger - weekends, every 2 hours");
            response.put("triggerType", "CALENDAR");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error scheduling with calendar trigger weekend", e);
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorMap);
        }
    }

    /**
     * Helper method to create a basic report schedule
     */
    private ReportSchedule createBasicReportSchedule() {
        ReportSchedule schedule = new ReportSchedule();
        schedule.setReportUnitUris(Arrays.asList("/public/Samples/Reports/RevenueDetailReport"));
        schedule.setScheduleName("Example Report Schedule");
        schedule.setOutputTimeZone("UTC");

        // Set output formats
        List<ReportSchedule.OutputFormats> outputFormatsList = new ArrayList<>();
        ReportSchedule.OutputFormats outputFormats = new ReportSchedule.OutputFormats();
        outputFormats.setOutputFormat(Arrays.asList("PDF"));
        outputFormatsList.add(outputFormats);
        schedule.setOutputFormats(outputFormatsList);

        // Set mail notification
        ReportSchedule.MailNotification mailNotification = new ReportSchedule.MailNotification();
        mailNotification.setSubject("Example Report");
        mailNotification.setMessageText("This is an example report generated by the scheduler");

        ReportSchedule.MailNotification.ToAddresses toAddresses = new ReportSchedule.MailNotification.ToAddresses();
        toAddresses.setAddress(Arrays.asList("user@example.com"));
        mailNotification.setToAddresses(toAddresses);

        schedule.setMailNotification(mailNotification);

        return schedule;
    }
}
