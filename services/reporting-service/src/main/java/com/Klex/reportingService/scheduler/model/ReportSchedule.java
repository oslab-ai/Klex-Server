package com.Klex.reportingService.scheduler.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ReportSchedule {

    private String jobId;
    private String reportUnitUri;
    private String scheduleName;

    // Quartz trigger metadata (populated for list/detail responses)
    private String triggerState;
    private String nextFireTime;
    private String previousFireTime;
    private String cronExpression;
    private Map<String, Object> parameters;
    private OutputFormat outputFormat;
    private DeliveryMethod deliveryMethod;
    private String emailTo;

    // New fields to match JSON structure
    private MailNotification mailNotification;
    private OutputFormats outputFormats;
    private String outputTimeZone;
    private BaseTrigger trigger;
    private Source source;
    private RepositoryDestination repositoryDestination;
    private Map<String, Object> dataAdapter;

    // Nested classes
    @Data
    public static class MailNotification {
        private String messageText;
        private String subject;
        private ToAddresses toAddresses;

        @Data
        public static class ToAddresses {
            private List<String> address;
        }
    }

    @Data
    public static class OutputFormats {
        private List<String> outputFormat;
    }

    @Data
    public static class Source {
        private String reportUnitURI;
        private Parameters parameters;

        @Data
        public static class Parameters {
            private ParameterValues parameterValues;

            @Data
            public static class ParameterValues {
                private List<String> ProductFamily;
            }
        }
    }

    @Data
    public static class RepositoryDestination {
        private boolean overwriteFiles;
        private boolean sequentialFilenames;
        private String folderURI;
        private boolean saveToRepository;
        private String timestampPattern;
        private OutputFTPInfo outputFTPInfo;

        @Data
        public static class OutputFTPInfo {
            private String type;
            private int port;
            private String folderPath;
            private String password;
            private Map<String, Object> propertiesMap;
            private String serverName;
            private String userName;
        }
    }

    public CalendarTrigger getAsCalendarTrigger() {
        return (trigger instanceof CalendarTrigger) ? (CalendarTrigger) trigger : null;
    }

    public SimpleTrigger getAsSimpleTrigger() {
        return (trigger instanceof SimpleTrigger) ? (SimpleTrigger) trigger : null;
    }

    @SuppressWarnings("unchecked")
    public <T extends BaseTrigger> T getTriggerAs(Class<T> type) {
        return type.isInstance(trigger) ? (T) type.cast(trigger) : null;
    }
}
