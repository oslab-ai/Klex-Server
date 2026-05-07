package com.Klex.reportingService.scheduler.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quartz.CalendarIntervalScheduleBuilder;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.DailyTimeIntervalScheduleBuilder;
import org.quartz.DailyTimeIntervalTrigger;
import org.quartz.CalendarIntervalTrigger;
import org.quartz.DateBuilder;
import org.quartz.TimeOfDay;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * Calendar Trigger implementation for calendar-based scheduling
 * Extends BaseTrigger to inherit common properties
 * Supports Cron expressions, Calendar intervals, and Daily time intervals
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CalendarTrigger extends BaseTrigger {

    private String cronExpression;
    private String calendarIntervalUnit; // DAY, WEEK, MONTH, YEAR
    private int calendarInterval = 1;
    private boolean preserveHourOfDayAcrossDaylightSavings = true;
    private boolean skipDayIfHourDoesNotExist = false;

    // Daily time interval properties
    private String startTimeOfDay; // Format: "HH:mm:ss"
    private String endTimeOfDay; // Format: "HH:mm:ss"
    private List<Integer> daysOfWeek; // 1=Sunday, 2=Monday, etc.
    private int repeatInterval = 1;
    private String repeatIntervalUnit = "HOUR"; // SECOND, MINUTE, HOUR

    @Override
    @SuppressWarnings("unchecked")
    public Trigger buildQuartzTrigger(String jobId, String groupId) {
        TriggerBuilder<?> triggerBuilder = TriggerBuilder.newTrigger()
                .withIdentity(jobId + "-calendar-trigger", groupId)
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

        // Build schedule based on available properties
        if (cronExpression != null && !cronExpression.trim().isEmpty()) {
            // Use Cron expression
            return ((TriggerBuilder<CronTrigger>) triggerBuilder)
                    .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression)
                            .inTimeZone(java.util.TimeZone.getTimeZone(timezone)))
                    .build();
        } else if (startTimeOfDay != null && endTimeOfDay != null) {
            // Use Daily time interval
            DailyTimeIntervalScheduleBuilder scheduleBuilder = DailyTimeIntervalScheduleBuilder
                    .dailyTimeIntervalSchedule()
                    .startingDailyAt(toTimeOfDay(startTimeOfDay))
                    .endingDailyAt(toTimeOfDay(endTimeOfDay));

            if (daysOfWeek != null && !daysOfWeek.isEmpty()) {
                scheduleBuilder.onDaysOfTheWeek(daysOfWeek.toArray(new Integer[0]));
            }

            switch (repeatIntervalUnit.toUpperCase()) {
                case "SECOND":
                    scheduleBuilder.withIntervalInSeconds(repeatInterval);
                    break;
                case "MINUTE":
                    scheduleBuilder.withIntervalInMinutes(repeatInterval);
                    break;
                case "HOUR":
                    scheduleBuilder.withIntervalInHours(repeatInterval);
                    break;
                default:
                    scheduleBuilder.withIntervalInHours(repeatInterval);
            }

            return ((TriggerBuilder<DailyTimeIntervalTrigger>) triggerBuilder).withSchedule(scheduleBuilder).build();
        } else if (calendarIntervalUnit != null && !calendarIntervalUnit.trim().isEmpty()) {
            // Use Calendar interval
            CalendarIntervalScheduleBuilder scheduleBuilder = CalendarIntervalScheduleBuilder
                    .calendarIntervalSchedule()
                    .withInterval(calendarInterval, getCalendarIntervalUnit());

            if (preserveHourOfDayAcrossDaylightSavings) {
                scheduleBuilder.preserveHourOfDayAcrossDaylightSavings(preserveHourOfDayAcrossDaylightSavings);
            }

            if (skipDayIfHourDoesNotExist) {
                scheduleBuilder.skipDayIfHourDoesNotExist(skipDayIfHourDoesNotExist);
            }

            return ((TriggerBuilder<CalendarIntervalTrigger>) triggerBuilder).withSchedule(scheduleBuilder).build();
        } else {
            // Default to daily at midnight
            return ((TriggerBuilder<CronTrigger>) triggerBuilder)
                    .withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(0, 0)
                            .inTimeZone(java.util.TimeZone.getTimeZone(timezone)))
                    .build();
        }
    }

    @Override
    public String getTriggerType() {
        return "CALENDAR";
    }

    /**
     * Get the Quartz IntervalUnit enum for CalendarIntervalScheduleBuilder
     */
    private DateBuilder.IntervalUnit getCalendarIntervalUnit() {
        switch (calendarIntervalUnit.toUpperCase()) {
            case "DAY":
                return DateBuilder.IntervalUnit.DAY;
            case "WEEK":
                return DateBuilder.IntervalUnit.WEEK;
            case "MONTH":
                return DateBuilder.IntervalUnit.MONTH;
            case "YEAR":
                return DateBuilder.IntervalUnit.YEAR;
            default:
                return DateBuilder.IntervalUnit.DAY;
        }
    }

    private TimeOfDay toTimeOfDay(String time) {
        LocalTime lt = LocalTime.parse(time);
        return new TimeOfDay(lt.getHour(), lt.getMinute(), lt.getSecond());
    }

    /**
     * Convenience methods for common calendar trigger patterns
     */
    public static CalendarTrigger dailyAt(String time) {
        CalendarTrigger trigger = new CalendarTrigger();
        trigger.setCronExpression("0 0 " + time + " * * ?");
        trigger.setTimezone("UTC");
        return trigger;
    }

    public static CalendarTrigger weeklyOn(int dayOfWeek, String time) {
        CalendarTrigger trigger = new CalendarTrigger();
        trigger.setCronExpression("0 0 " + time + " ? * " + dayOfWeek);
        trigger.setTimezone("UTC");
        return trigger;
    }

    public static CalendarTrigger monthlyOn(int dayOfMonth, String time) {
        CalendarTrigger trigger = new CalendarTrigger();
        trigger.setCronExpression("0 0 " + time + " " + dayOfMonth + " * ?");
        trigger.setTimezone("UTC");
        return trigger;
    }

    public static CalendarTrigger yearlyOn(int month, int dayOfMonth, String time) {
        CalendarTrigger trigger = new CalendarTrigger();
        trigger.setCronExpression("0 0 " + time + " " + dayOfMonth + " " + month + " ?");
        trigger.setTimezone("UTC");
        return trigger;
    }

    public static CalendarTrigger businessHours() {
        CalendarTrigger trigger = new CalendarTrigger();
        trigger.setStartTimeOfDay("09:00:00");
        trigger.setEndTimeOfDay("17:00:00");
        trigger.setRepeatInterval(1);
        trigger.setRepeatIntervalUnit("HOUR");
        trigger.setDaysOfWeek(Arrays.asList(2, 3, 4, 5, 6)); // Monday to Friday
        trigger.setTimezone("UTC");
        return trigger;
    }
}
