package com.Klex.reportingService.scheduler.service;

import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.Klex.reportingService.scheduler.job.ReportGenerationJob;
import com.Klex.reportingService.scheduler.model.BaseTrigger;
import com.Klex.reportingService.scheduler.model.CalendarTrigger;
import com.Klex.reportingService.scheduler.model.DeliveryMethod;
import com.Klex.reportingService.scheduler.model.OutputFormat;
import com.Klex.reportingService.scheduler.model.ReportSchedule;
import com.Klex.reportingService.scheduler.model.SimpleTrigger;

import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class ReportSchedulerService {

    private final Scheduler scheduler;
    private final ObjectMapper objectMapper;

    public ReportSchedulerService(Scheduler scheduler, ObjectMapper objectMapper) {
        this.scheduler = scheduler;
        this.objectMapper = objectMapper;
    }

    public String scheduleReport(ReportSchedule schedule) throws SchedulerException {
        String jobId = UUID.randomUUID().toString();

        // Extract output format (use local vars — not instance fields — for thread
        // safety)
        String outputFormat = null;
        List<String> outputFormats = null;
        if (schedule.getOutputFormats() != null && !schedule.getOutputFormats().isEmpty()
                && schedule.getOutputFormats().get(0).getOutputFormat() != null
                && !schedule.getOutputFormats().get(0).getOutputFormat().isEmpty()) {
            outputFormats = schedule.getOutputFormats().get(0).getOutputFormat();
            outputFormat = outputFormats.get(0);
        }

        // Extract email details
        String emailTo = null;
        if (schedule.getMailNotification() != null &&
                schedule.getMailNotification().getToAddresses() != null &&
                schedule.getMailNotification().getToAddresses().getAddress() != null &&
                !schedule.getMailNotification().getToAddresses().getAddress().isEmpty()) {
            emailTo = String.join(",", schedule.getMailNotification().getToAddresses().getAddress());
        }

        // Serialize data adapter
        String dataAdapterJson = null;
        if (schedule.getDataAdapter() != null) {
            try {
                dataAdapterJson = objectMapper.writeValueAsString(schedule.getDataAdapter());
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize data adapter", e);
            }
        }

        // Build JobDetail — store ALL fields needed to reconstruct the schedule later
        JobBuilder jobBuilder = JobBuilder.newJob(ReportGenerationJob.class)
                .withIdentity(jobId, "report-jobs")
                .usingJobData("reportUnitUri", String.join(",", schedule.getReportUnitUris()))
                .usingJobData("scheduleName", schedule.getScheduleName() != null ? schedule.getScheduleName() : "")
                .usingJobData("outputFormat", outputFormat)
                .usingJobData("outputFormats",
                        outputFormats != null ? String.join(",", outputFormats)
                                : (outputFormat != null ? outputFormat : "PDF"))
                .usingJobData("outputTimeZone",
                        schedule.getOutputTimeZone() != null ? schedule.getOutputTimeZone() : "UTC")
                .usingJobData("deliveryMethod", "EMAIL")
                .usingJobData("emailTo", emailTo != null ? emailTo : "")
                .usingJobData("emailSubject",
                        schedule.getMailNotification() != null && schedule.getMailNotification().getSubject() != null
                                ? schedule.getMailNotification().getSubject()
                                : "")
                .usingJobData("emailMessage",
                        schedule.getMailNotification() != null
                                && schedule.getMailNotification().getMessageText() != null
                                        ? schedule.getMailNotification().getMessageText()
                                        : "");

        // Serialize per-report output formats
        if (schedule.getReportOutputFormats() != null && !schedule.getReportOutputFormats().isEmpty()) {
            try {
                String reportFormatsJson = objectMapper.writeValueAsString(schedule.getReportOutputFormats());
                jobBuilder.usingJobData("reportFormatsJson", reportFormatsJson);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize report output formats", e);
            }
        }

        if (dataAdapterJson != null) {
            jobBuilder.usingJobData("dataAdapter", dataAdapterJson);
        }

        JobDetail jobDetail = jobBuilder.build();

        // Build Trigger based on the trigger configuration
        Trigger trigger = buildTriggerFromSchedule(schedule, jobId);

        // Schedule the job
        scheduler.scheduleJob(jobDetail, trigger);
        log.info("Scheduled report job with ID: {} using {} trigger", jobId,
                schedule.getTrigger() != null ? schedule.getTrigger().getTriggerType() : "DEFAULT");

        return jobId;
    }

    /**
     * Build trigger from schedule configuration
     */
    private Trigger buildTriggerFromSchedule(ReportSchedule schedule, String jobId) throws SchedulerException {
        if (schedule.getTriggerAs(CalendarTrigger.class) != null) {
            return schedule.getTriggerAs(CalendarTrigger.class).buildQuartzTrigger(jobId, "report-triggers");
        } else if (schedule.getTriggerAs(SimpleTrigger.class) != null) {
            return schedule.getTriggerAs(SimpleTrigger.class).buildQuartzTrigger(jobId, "report-triggers");
        }
        // Fallback to default simple trigger
        return buildDefaultTrigger(schedule, jobId);
    }

    /**
     * Build default trigger when none is provided
     */
    private Trigger buildDefaultTrigger(ReportSchedule schedule, String jobId) throws SchedulerException {
        SimpleTrigger defaultTrigger = new SimpleTrigger();
        defaultTrigger.setTimezone(schedule.getOutputTimeZone() != null ? schedule.getOutputTimeZone() : "UTC");
        defaultTrigger.setRecurrenceInterval(5);
        defaultTrigger.setRecurrenceIntervalUnit("MINUTE");
        defaultTrigger.setOccurrenceCount(3);
        defaultTrigger.setDescription("Default simple trigger");

        return defaultTrigger.buildQuartzTrigger(jobId, "report-triggers");
    }

    /**
     * Schedule a report with a Simple Trigger
     */
    public String scheduleReportWithSimpleTrigger(ReportSchedule schedule, int interval, String unit, int count)
            throws SchedulerException {
        SimpleTrigger simpleTrigger = new SimpleTrigger();
        simpleTrigger.setTimezone(schedule.getOutputTimeZone() != null ? schedule.getOutputTimeZone() : "UTC");
        simpleTrigger.setRecurrenceInterval(interval);
        simpleTrigger.setRecurrenceIntervalUnit(unit);
        simpleTrigger.setOccurrenceCount(count);

        schedule.setTrigger(simpleTrigger);
        return scheduleReport(schedule);
    }

    /**
     * Schedule a report with a Calendar Trigger using Cron expression
     */
    public String scheduleReportWithCronTrigger(ReportSchedule schedule, String cronExpression)
            throws SchedulerException {
        CalendarTrigger calendarTrigger = new CalendarTrigger();
        calendarTrigger.setTimezone(schedule.getOutputTimeZone() != null ? schedule.getOutputTimeZone() : "UTC");
        calendarTrigger.setCronExpression(cronExpression);

        schedule.setTrigger(calendarTrigger);
        return scheduleReport(schedule);
    }

    /**
     * Schedule a report with a Calendar Trigger for business hours
     */
    public String scheduleReportForBusinessHours(ReportSchedule schedule) throws SchedulerException {
        CalendarTrigger businessTrigger = CalendarTrigger.businessHours();
        businessTrigger.setTimezone(schedule.getOutputTimeZone() != null ? schedule.getOutputTimeZone() : "UTC");

        schedule.setTrigger(businessTrigger);
        return scheduleReport(schedule);
    }

    /**
     * Schedule a report to run daily at specific time
     */
    public String scheduleReportDailyAt(ReportSchedule schedule, String time) throws SchedulerException {
        CalendarTrigger dailyTrigger = CalendarTrigger.dailyAt(time);
        dailyTrigger.setTimezone(schedule.getOutputTimeZone() != null ? schedule.getOutputTimeZone() : "UTC");

        schedule.setTrigger(dailyTrigger);
        return scheduleReport(schedule);
    }

    public void deleteSchedule(String jobId) throws SchedulerException {
        scheduler.deleteJob(JobKey.jobKey(jobId, "report-jobs"));
        log.info("Deleted report job with ID: {}", jobId);
    }

    @SuppressWarnings("unchecked")
    public ReportSchedule getSchedule(String jobId) throws SchedulerException {
        JobDetail jobDetail = scheduler.getJobDetail(JobKey.jobKey(jobId, "report-jobs"));

        if (jobDetail == null) {
            throw new SchedulerException("Job not found: " + jobId);
        }

        JobDataMap jobDataMap = jobDetail.getJobDataMap();
        ReportSchedule schedule = new ReportSchedule();
        schedule.setJobId(jobId);

        // Core fields
        String reportUnitUri = jobDataMap.getString("reportUnitUri");
        if (reportUnitUri != null) {
            schedule.setReportUnitUris(Arrays.asList(reportUnitUri.split(",")));
        }

        String scheduleName = jobDataMap.getString("scheduleName");
        if (scheduleName != null && !scheduleName.isEmpty()) {
            schedule.setScheduleName(scheduleName);
        }

        String outputTimeZone = jobDataMap.getString("outputTimeZone");
        if (outputTimeZone != null && !outputTimeZone.isEmpty()) {
            schedule.setOutputTimeZone(outputTimeZone);
        }

        // Output formats
        String outputFormatsStr = jobDataMap.getString("outputFormats");
        if (outputFormatsStr != null && !outputFormatsStr.isEmpty()) {
            List<ReportSchedule.OutputFormats> formatsList = new ArrayList<>();
            ReportSchedule.OutputFormats formats = new ReportSchedule.OutputFormats();
            formats.setOutputFormat(java.util.Arrays.asList(outputFormatsStr.split(",")));
            formatsList.add(formats);
            schedule.setOutputFormats(formatsList);
        }

        String outputFormatStr = jobDataMap.getString("outputFormat");
        if (outputFormatStr != null) {
            try {
                schedule.setOutputFormat(OutputFormat.valueOf(outputFormatStr));
            } catch (IllegalArgumentException e) {
                // ignore invalid format
            }
        }

        // Delivery method
        String deliveryMethodStr = jobDataMap.getString("deliveryMethod");
        if (deliveryMethodStr != null) {
            try {
                schedule.setDeliveryMethod(DeliveryMethod.valueOf(deliveryMethodStr));
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }

        // Reconstruct mail notification
        String emailTo = jobDataMap.getString("emailTo");
        String emailSubject = jobDataMap.getString("emailSubject");
        String emailMessage = jobDataMap.getString("emailMessage");
        if (emailTo != null && !emailTo.isEmpty()) {
            schedule.setEmailTo(emailTo);
            ReportSchedule.MailNotification mail = new ReportSchedule.MailNotification();
            mail.setSubject(emailSubject != null ? emailSubject : "");
            mail.setMessageText(emailMessage != null ? emailMessage : "");
            ReportSchedule.MailNotification.ToAddresses toAddresses = new ReportSchedule.MailNotification.ToAddresses();
            toAddresses.setAddress(java.util.Arrays.asList(emailTo.split(",")));
            mail.setToAddresses(toAddresses);
            schedule.setMailNotification(mail);
        }

        // Per-report output formats
        String reportFormatsJson = jobDataMap.getString("reportFormatsJson");
        if (reportFormatsJson != null && !reportFormatsJson.isEmpty()) {
            try {
                Map<String, List<String>> reportOutputFormats = objectMapper.readValue(reportFormatsJson,
                        new TypeReference<Map<String, List<String>>>() {
                        });
                schedule.setReportOutputFormats(reportOutputFormats);
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize report output formats", e);
            }
        }

        // Data adapter
        String dataAdapterJson = jobDataMap.getString("dataAdapter");
        if (dataAdapterJson != null) {
            try {
                Map<String, Object> dataAdapter = objectMapper.readValue(dataAdapterJson,
                        new TypeReference<Map<String, Object>>() {
                        });
                schedule.setDataAdapter(dataAdapter);
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize data adapter", e);
            }
        }

        Object parameters = jobDataMap.get("parameters");
        if (parameters != null) {
            schedule.setParameters((Map<String, Object>) parameters);
        }

        // Quartz trigger metadata
        List<? extends org.quartz.Trigger> triggers = scheduler.getTriggersOfJob(JobKey.jobKey(jobId, "report-jobs"));
        if (triggers != null && !triggers.isEmpty()) {
            org.quartz.Trigger trigger = triggers.get(0);
            org.quartz.Trigger.TriggerState state = scheduler.getTriggerState(trigger.getKey());
            schedule.setTriggerState(state.name());

            if (trigger.getNextFireTime() != null) {
                schedule.setNextFireTime(trigger.getNextFireTime().toInstant().toString());
            }
            if (trigger.getPreviousFireTime() != null) {
                schedule.setPreviousFireTime(trigger.getPreviousFireTime().toInstant().toString());
            }

            // Reconstruct trigger info for the response
            if (trigger instanceof org.quartz.SimpleTrigger) {
                org.quartz.SimpleTrigger st = (org.quartz.SimpleTrigger) trigger;
                SimpleTrigger simpleTrigger = new SimpleTrigger();
                simpleTrigger.setTimezone(outputTimeZone != null ? outputTimeZone : "UTC");
                long intervalMs = st.getRepeatInterval();
                // Convert ms back to the most reasonable unit
                if (intervalMs >= 86400000L * 7) {
                    simpleTrigger.setRecurrenceInterval((int) (intervalMs / (86400000L * 7)));
                    simpleTrigger.setRecurrenceIntervalUnit("WEEK");
                } else if (intervalMs >= 86400000L) {
                    simpleTrigger.setRecurrenceInterval((int) (intervalMs / 86400000L));
                    simpleTrigger.setRecurrenceIntervalUnit("DAY");
                } else if (intervalMs >= 3600000L) {
                    simpleTrigger.setRecurrenceInterval((int) (intervalMs / 3600000L));
                    simpleTrigger.setRecurrenceIntervalUnit("HOUR");
                } else {
                    simpleTrigger.setRecurrenceInterval((int) (intervalMs / 60000L));
                    simpleTrigger.setRecurrenceIntervalUnit("MINUTE");
                }
                int repeatCount = st.getRepeatCount();
                simpleTrigger.setOccurrenceCount(
                        repeatCount == org.quartz.SimpleTrigger.REPEAT_INDEFINITELY ? -1 : repeatCount + 1);
                schedule.setTrigger(simpleTrigger);
            } else if (trigger instanceof org.quartz.CronTrigger) {
                org.quartz.CronTrigger ct = (org.quartz.CronTrigger) trigger;
                CalendarTrigger calendarTrigger = new CalendarTrigger();
                calendarTrigger.setTimezone(outputTimeZone != null ? outputTimeZone : "UTC");
                calendarTrigger.setCronExpression(ct.getCronExpression());
                schedule.setTrigger(calendarTrigger);
            }
        }

        return schedule;
    }

    public List<ReportSchedule> getAllSchedules() throws SchedulerException {
        Set<JobKey> jobKeys = scheduler.getJobKeys(org.quartz.impl.matchers.GroupMatcher.jobGroupEquals("report-jobs"));
        List<ReportSchedule> schedules = new ArrayList<>();
        for (JobKey jobKey : jobKeys) {
            try {
                schedules.add(getSchedule(jobKey.getName()));
            } catch (SchedulerException e) {
                log.warn("Failed to load schedule for job: {}", jobKey.getName(), e);
            }
        }
        return schedules;
    }
}
