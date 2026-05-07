package com.Klex.reportingService.scheduler.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * Simple Trigger implementation for basic interval-based scheduling
 * Extends BaseTrigger to inherit common properties
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SimpleTrigger extends BaseTrigger {

    private int occurrenceCount = -1; // -1 means repeat forever
    private int startType = 1; // 1 = immediate start
    private int recurrenceInterval = 1;
    private String recurrenceIntervalUnit = "MINUTE"; // MINUTE, HOUR, DAY, WEEK, MONTH, YEAR

    @Override
    public Trigger buildQuartzTrigger(String jobId, String groupId) {
        TriggerBuilder<?> triggerBuilder = TriggerBuilder.newTrigger()
                .withIdentity(jobId + "-simple-trigger", groupId)
                .withDescription(description)
                .withPriority(priority);

        // Set start time
        if (startDate != null && !startDate.trim().isEmpty()) {
            try {
                LocalDateTime startDateTime = LocalDateTime.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                triggerBuilder
                        .startAt(java.util.Date.from(startDateTime.atZone(java.time.ZoneId.of(timezone)).toInstant()));
            } catch (Exception e) {
                // If parsing fails, start immediately
                triggerBuilder.startNow();
            }
        } else {
            triggerBuilder.startNow();
        }

        // Set end time
        if (endDate != null && !endDate.trim().isEmpty()) {
            try {
                LocalDateTime endDateTime = LocalDateTime.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                triggerBuilder
                        .endAt(java.util.Date.from(endDateTime.atZone(java.time.ZoneId.of(timezone)).toInstant()));
            } catch (Exception e) {
                // If parsing fails, ignore end date
            }
        }

        // Build schedule based on interval unit
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule();

        switch (recurrenceIntervalUnit.toUpperCase()) {
            case "MINUTE":
                if (occurrenceCount > 0) {
                    scheduleBuilder.withIntervalInMinutes(recurrenceInterval).withRepeatCount(occurrenceCount - 1);
                } else {
                    scheduleBuilder.withIntervalInMinutes(recurrenceInterval).repeatForever();
                }
                break;
            case "HOUR":
                if (occurrenceCount > 0) {
                    scheduleBuilder.withIntervalInHours(recurrenceInterval).withRepeatCount(occurrenceCount - 1);
                } else {
                    scheduleBuilder.withIntervalInHours(recurrenceInterval).repeatForever();
                }
                break;
            case "DAY":
                if (occurrenceCount > 0) {
                    scheduleBuilder.withIntervalInHours(recurrenceInterval * 24).withRepeatCount(occurrenceCount - 1);
                } else {
                    scheduleBuilder.withIntervalInHours(recurrenceInterval * 24).repeatForever();
                }
                break;
            case "WEEK":
                if (occurrenceCount > 0) {
                    scheduleBuilder.withIntervalInHours(recurrenceInterval * 24 * 7)
                            .withRepeatCount(occurrenceCount - 1);
                } else {
                    scheduleBuilder.withIntervalInHours(recurrenceInterval * 24 * 7).repeatForever();
                }
                break;
            case "MONTH":
                if (occurrenceCount > 0) {
                    scheduleBuilder.withIntervalInHours(recurrenceInterval * 24 * 30)
                            .withRepeatCount(occurrenceCount - 1);
                } else {
                    scheduleBuilder.withIntervalInHours(recurrenceInterval * 24 * 30).repeatForever();
                }
                break;
            case "YEAR":
                if (occurrenceCount > 0) {
                    scheduleBuilder.withIntervalInHours(recurrenceInterval * 24 * 365)
                            .withRepeatCount(occurrenceCount - 1);
                } else {
                    scheduleBuilder.withIntervalInHours(recurrenceInterval * 24 * 365).repeatForever();
                }
                break;
            default:
                // Default to minutes
                if (occurrenceCount > 0) {
                    scheduleBuilder.withIntervalInMinutes(recurrenceInterval).withRepeatCount(occurrenceCount - 1);
                } else {
                    scheduleBuilder.withIntervalInMinutes(recurrenceInterval).repeatForever();
                }
        }
        @SuppressWarnings("unchecked")
        Trigger trigger = ((TriggerBuilder<org.quartz.SimpleTrigger>) triggerBuilder).withSchedule(scheduleBuilder)
                .build();
        return trigger;
    }

    @Override
    public String getTriggerType() {
        return "SIMPLE";
    }

    /**
     * Convenience method for common simple trigger patterns
     */
    public static SimpleTrigger everyMinutes(int minutes, int count) {
        SimpleTrigger trigger = new SimpleTrigger();
        trigger.setRecurrenceInterval(minutes);
        trigger.setRecurrenceIntervalUnit("MINUTE");
        trigger.setOccurrenceCount(count);
        trigger.setTimezone("UTC");
        return trigger;
    }

    public static SimpleTrigger everyHours(int hours, int count) {
        SimpleTrigger trigger = new SimpleTrigger();
        trigger.setRecurrenceInterval(hours);
        trigger.setRecurrenceIntervalUnit("HOUR");
        trigger.setOccurrenceCount(count);
        trigger.setTimezone("UTC");
        return trigger;
    }

    public static SimpleTrigger everyDays(int days, int count) {
        SimpleTrigger trigger = new SimpleTrigger();
        trigger.setRecurrenceInterval(days);
        trigger.setRecurrenceIntervalUnit("DAY");
        trigger.setOccurrenceCount(count);
        trigger.setTimezone("UTC");
        return trigger;
    }
}
