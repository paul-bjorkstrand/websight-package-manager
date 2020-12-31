package pl.ds.websight.packagemanager.dto;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.event.jobs.ScheduleInfo;
import org.apache.sling.event.jobs.ScheduledJobInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ds.websight.packagemanager.JobProperties;
import pl.ds.websight.packagemanager.rest.schedule.ScheduleActionType;
import pl.ds.websight.packagemanager.util.DateUtil;
import pl.ds.websight.packagemanager.util.JobUtil;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

public class PackageScheduleActionInfoDto {

    private static final Logger LOG = LoggerFactory.getLogger(PackageScheduleActionInfoDto.class);

    private String id;
    private final ScheduleActionType action;
    private final String nextExecution;
    private final String applicantId;
    private boolean suspended;
    private List<PackageActionScheduleDto> schedules;

    private PackageScheduleActionInfoDto(String id, ScheduleActionType action, String nextExecution, String applicantId,
            boolean suspended, List<PackageActionScheduleDto> schedules) {
        this(action, nextExecution, applicantId);
        this.id = id;
        this.suspended = suspended;
        this.schedules = schedules;
    }

    private PackageScheduleActionInfoDto(ScheduleActionType action, String nextExecution, String applicantId) {
        this.action = action;
        this.nextExecution = nextExecution;
        this.applicantId = applicantId;
    }

    public static PackageScheduleActionInfoDto asBasicInfo(ScheduledJobInfo scheduledJobInfo) {
        String jobTopic = scheduledJobInfo.getJobTopic();
        ScheduleActionType action = ScheduleActionType.from(jobTopic);
        if (action == null) {
            LOG.warn("Could not recognize type of scheduled action for job topic: {}", jobTopic);
            return null;
        }
        String applicantId = JobProperties.getApplicantId(scheduledJobInfo);
        if (applicantId == null) {
            LOG.warn("Could not recognize applicant of scheduled action: {}", action);
            return null;
        }
        return new PackageScheduleActionInfoDto(action, getFormattedNextScheduleDate(scheduledJobInfo), applicantId);
    }

    public static PackageScheduleActionInfoDto asFullInfo(ScheduledJobInfo scheduledJobInfo) {
        String jobTopic = scheduledJobInfo.getJobTopic();
        ScheduleActionType action = ScheduleActionType.from(jobTopic);
        if (action == null) {
            LOG.warn("Could not recognize type of scheduled action for job topic: {}", jobTopic);
            return null;
        }
        String applicantId = JobProperties.getApplicantId(scheduledJobInfo);
        if (applicantId == null) {
            LOG.warn("Could not recognize applicant of scheduled action: {}", action);
            return null;
        }
        List<PackageActionScheduleDto> schedules = scheduledJobInfo.getSchedules().stream()
                .map(PackageActionScheduleDto::forSchedule)
                .filter(Objects::nonNull)
                .collect(toList());
        if (schedules.isEmpty()) {
            LOG.warn("Invalid state of schedule job: no valid schedules for action: {} requested by: {}", action, applicantId);
            return null;
        }
        return new PackageScheduleActionInfoDto(JobUtil.getScheduleId(scheduledJobInfo), action,
                getFormattedNextScheduleDate(scheduledJobInfo), applicantId, scheduledJobInfo.isSuspended(), schedules);
    }

    private static String getFormattedNextScheduleDate(ScheduledJobInfo scheduledJobInfo) {
        Date nextScheduledExecution = scheduledJobInfo.getNextScheduledExecution();
        return DateUtil.format(nextScheduledExecution);
    }

    public String getId() {
        return id;
    }

    public ScheduleActionType getAction() {
        return action;
    }

    public String getNextExecution() {
        return nextExecution;
    }

    public String getApplicantId() {
        return applicantId;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public List<PackageActionScheduleDto> getSchedules() {
        return schedules;
    }

    private static class PackageActionScheduleDto {

        private static final Logger LOG = LoggerFactory.getLogger(PackageActionScheduleDto.class);

        private final String at;
        private final String cron;

        private PackageActionScheduleDto(String at, String cron) {
            this.at = at;
            this.cron = cron;
        }

        private static PackageActionScheduleDto forSchedule(ScheduleInfo scheduleInfo) {
            String expression = scheduleInfo.getExpression();
            if (StringUtils.isNotBlank(expression)) {
                return new PackageActionScheduleDto(null, expression);
            } else if (scheduleInfo.getAt() != null) {
                return new PackageActionScheduleDto(DateUtil.format(scheduleInfo.getAt()), null);
            }
            LOG.warn("Invalid state of {}: there is no expression nor execution date for schedule with type: {}",
                    ScheduleInfo.class.getName(), scheduleInfo.getType());
            return null;
        }

        public String getAt() {
            return at;
        }

        public String getCron() {
            return cron;
        }
    }
}
