package com.Klex.reportingService.scheduler.model;

import lombok.Data;
import org.quartz.Trigger;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Base abstract class for all trigger types
 * Provides common properties and methods that all triggers should have
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CalendarTrigger.class, name = "calendarTrigger"),
        @JsonSubTypes.Type(value = SimpleTrigger.class, name = "simpleTrigger")
})
public abstract class BaseTrigger {

    protected String timezone;
    protected String startDate;
    protected String endDate;
    protected String description;
    protected int priority;
    protected String calendarName;

    /**
     * Abstract method that subclasses must implement
     * to create the actual Quartz Trigger
     */
    public abstract Trigger buildQuartzTrigger(String jobId, String groupId);

    /**
     * Common validation method
     */
    public boolean isValid() {
        return timezone != null && !timezone.trim().isEmpty();
    }

    /**
     * Get trigger type name
     */
    public abstract String getTriggerType();
}
