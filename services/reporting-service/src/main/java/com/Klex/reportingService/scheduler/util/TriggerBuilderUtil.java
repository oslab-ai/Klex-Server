package com.Klex.reportingService.scheduler.util;

import com.Klex.reportingService.scheduler.model.BaseTrigger;
import com.Klex.reportingService.scheduler.model.CalendarTrigger;
import com.Klex.reportingService.scheduler.model.SimpleTrigger;

import java.util.Arrays;

/**
 * Utility class for building different types of triggers easily
 * Provides factory methods for common trigger patterns
 */
public class TriggerBuilderUtil {

    /**
     * Create a simple trigger that repeats every X minutes
     */
    public static SimpleTrigger everyMinutes(int minutes, int count) {
        SimpleTrigger trigger = new SimpleTrigger();
        trigger.setRecurrenceInterval(minutes);
        trigger.setRecurrenceIntervalUnit("MINUTE");
        trigger.setOccurrenceCount(count);
        trigger.setTimezone("UTC");
        return trigger;
    }

    /**
     * Create a simple trigger that repeats every X hours
     */
    public static SimpleTrigger everyHours(int hours, int count) {
        SimpleTrigger trigger = new SimpleTrigger();
        trigger.setRecurrenceInterval(hours);
        trigger.setRecurrenceIntervalUnit("HOUR");
        trigger.setOccurrenceCount(count);
        trigger.setTimezone("UTC");
        return trigger;
    }

    /**
     * Create a simple trigger that repeats every X days
     */
    public static SimpleTrigger everyDays(int days, int count) {
        SimpleTrigger trigger = new SimpleTrigger();
        trigger.setRecurrenceInterval(days);
        trigger.setRecurrenceIntervalUnit("DAY");
        trigger.setOccurrenceCount(count);
        trigger.setTimezone("UTC");
        return trigger;
    }

    /**
     * Create a simple trigger that repeats forever
     */
    public static SimpleTrigger repeatForever(int interval, String unit) {
        SimpleTrigger trigger = new SimpleTrigger();
        trigger.setRecurrenceInterval(interval);
        trigger.setRecurrenceIntervalUnit(unit);
        trigger.setOccurrenceCount(-1); // -1 means repeat forever
        trigger.setTimezone("UTC");
        return trigger;
    }

    /**
     * Create a calendar trigger that runs daily at specific time
     */
    public static CalendarTrigger dailyAt(String time) {
        return CalendarTrigger.dailyAt(time);
    }

    /**
     * Create a calendar trigger that runs weekly on specific day and time
     */
    public static CalendarTrigger weeklyOn(int dayOfWeek, String time) {
        return CalendarTrigger.weeklyOn(dayOfWeek, time);
    }

    /**
     * Create a calendar trigger that runs monthly on specific day and time
     */
    public static CalendarTrigger monthlyOn(int dayOfMonth, String time) {
        return CalendarTrigger.monthlyOn(dayOfMonth, time);
    }

    /**
     * Create a calendar trigger that runs yearly on specific date and time
     */
    public static CalendarTrigger yearlyOn(int month, int dayOfMonth, String time) {
        return CalendarTrigger.yearlyOn(month, dayOfMonth, time);
    }

    /**
     * Create a calendar trigger for business hours (Monday-Friday, 9 AM - 5 PM)
     */
    public static CalendarTrigger businessHours() {
        return CalendarTrigger.businessHours();
    }

    /**
     * Create a calendar trigger with custom cron expression
     */
    public static CalendarTrigger cronExpression(String cronExpression) {
        CalendarTrigger trigger = new CalendarTrigger();
        trigger.setCronExpression(cronExpression);
        trigger.setTimezone("UTC");
        return trigger;
    }

    /**
     * Create a calendar trigger for specific time intervals during business hours
     */
    public static CalendarTrigger businessHoursWithInterval(String startTime, String endTime, int interval,
            String intervalUnit) {
        CalendarTrigger trigger = new CalendarTrigger();
        trigger.setStartTimeOfDay(startTime);
        trigger.setEndTimeOfDay(endTime);
        trigger.setRepeatInterval(interval);
        trigger.setRepeatIntervalUnit(intervalUnit);
        trigger.setDaysOfWeek(Arrays.asList(2, 3, 4, 5, 6)); // Monday to Friday
        trigger.setTimezone("UTC");
        return trigger;
    }

    /**
     * Create a calendar trigger for weekend scheduling
     */
    public static CalendarTrigger weekendSchedule(String time, int interval, String intervalUnit) {
        CalendarTrigger trigger = new CalendarTrigger();
        trigger.setStartTimeOfDay(time);
        trigger.setEndTimeOfDay("23:59:59");
        trigger.setRepeatInterval(interval);
        trigger.setRepeatIntervalUnit(intervalUnit);
        trigger.setDaysOfWeek(Arrays.asList(1, 7)); // Sunday and Saturday
        trigger.setTimezone("UTC");
        return trigger;
    }

    /**
     * Set timezone for any trigger
     */
    public static <T extends BaseTrigger> T withTimezone(T trigger, String timezone) {
        trigger.setTimezone(timezone);
        return trigger;
    }

    /**
     * Set start date for any trigger
     */
    public static <T extends BaseTrigger> T withStartDate(T trigger, String startDate) {
        trigger.setStartDate(startDate);
        return trigger;
    }

    /**
     * Set end date for any trigger
     */
    public static <T extends BaseTrigger> T withEndDate(T trigger, String endDate) {
        trigger.setEndDate(endDate);
        return trigger;
    }

    /**
     * Set description for any trigger
     */
    public static <T extends BaseTrigger> T withDescription(T trigger, String description) {
        trigger.setDescription(description);
        return trigger;
    }

    /**
     * Set priority for any trigger
     */
    public static <T extends BaseTrigger> T withPriority(T trigger, int priority) {
        trigger.setPriority(priority);
        return trigger;
    }
}
