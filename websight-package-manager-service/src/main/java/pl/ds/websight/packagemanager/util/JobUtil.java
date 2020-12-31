package pl.ds.websight.packagemanager.util;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobBuilder;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.JobManager.QueryType;
import org.apache.sling.event.jobs.ScheduleInfo;
import org.apache.sling.event.jobs.ScheduledJobInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ds.websight.packagemanager.JobProperties;
import pl.ds.websight.packagemanager.rest.schedule.Schedule;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.minBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.sling.api.resource.ResourceResolverFactory.USER_IMPERSONATION;

public final class JobUtil {

    private static final Logger LOG = LoggerFactory.getLogger(JobUtil.class);

    public static final String PKG_MANAGER_JOB_TOPIC_PREFIX = "pl/ds/websight/packagemanager/packageaction/";

    private JobUtil() {
        // no instance
    }

    public static ResourceResolver getImpersonatedResolver(ResourceResolverFactory factory, String applicantId) {
        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put(USER_IMPERSONATION, applicantId);
            return factory.getAdministrativeResourceResolver(properties);
        } catch (LoginException e) {
            LOG.error("Could not impersonate resource resolver using applicant user ID: {}", applicantId, e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static Job findFirstRunningOrQueuedJob(JobManager jobManager, Map<String, Object> jobProperties) {
        // QueryType.ALL surprisingly returns only running and queued jobs
        return jobManager.findJobs(QueryType.ALL, null, 1, jobProperties).stream()
                .filter(job -> isPackageManagerJob(job.getTopic()))
                .findFirst()
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    public static List<Job> findAllQueuedJobs(JobManager jobManager, Map<String, Object> jobProperties) {
        return jobManager.findJobs(QueryType.QUEUED, null, -1, jobProperties).stream()
                .filter(job -> isPackageManagerJob(job.getTopic()))
                .collect(toList());
    }

    public static List<Job> findRunningAndQueuedJobs(JobManager jobManager, String... packagesPaths) {
        @SuppressWarnings("unchecked")
        Map<String, Object>[] packagePathsQueryTemplates = Stream.of(packagesPaths)
                .map(JobProperties::asQueryMap)
                .toArray(Map[]::new);
        // QueryType.ALL surprisingly returns only running and queued jobs
        return jobManager.findJobs(QueryType.ALL, null, -1, packagePathsQueryTemplates).stream()
                .filter(job -> isPackageManagerJob(job.getTopic()))
                .collect(toList());
    }

    @SuppressWarnings("unchecked")
    public static ScheduledJobInfo findScheduledJobWithSchedules(JobManager jobManager, Map<String, Object> scheduleJobProps,
            List<Schedule> schedules, String jobTopic) {
        return jobManager.getScheduledJobs(jobTopic, 0, scheduleJobProps).stream()
                .filter(scheduledJobInfo -> containsEqualSchedules(scheduledJobInfo, schedules))
                .findFirst()
                .orElse(null);
    }

    public static ScheduledJobInfo getNearestScheduledJob(JobManager jobManager, String packagePath) {
        return getNearestScheduledJobs(jobManager, packagePath).get(packagePath);
    }

    public static Map<String, ScheduledJobInfo> getNearestScheduledJobs(JobManager jobManager, String... packagesPaths) {
        @SuppressWarnings("unchecked")
        Map<String, Object>[] packagePathsQueryTemplates = Stream.of(packagesPaths)
                .map(JobProperties::asQueryMap)
                .toArray(Map[]::new);
        return jobManager.getScheduledJobs(null, -1, packagePathsQueryTemplates).stream()
                .filter(jobInfo -> isPackageManagerJob(jobInfo.getJobTopic()))
                .filter(jobInfo -> !jobInfo.isSuspended() && !isScheduleObsolete(jobInfo))
                .collect(groupingBy(
                        JobProperties::getPackagePath,
                        minBy(comparing(ScheduledJobInfo::getNextScheduledExecution)))).entrySet().stream()
                .filter(entry -> entry.getValue().isPresent())
                .collect(toMap(Map.Entry::getKey, entry -> entry.getValue().get()));
    }

    private static boolean containsEqualSchedules(ScheduledJobInfo scheduledJobInfo, List<Schedule> schedules) {
        Collection<ScheduleInfo> jobInfoSchedules = scheduledJobInfo.getSchedules();
        return (jobInfoSchedules.size() >= schedules.size()) && jobInfoSchedules.stream().noneMatch(scheduleInfo ->
                schedules.stream().noneMatch(schedule -> schedule.matches(scheduleInfo)));
    }

    @SuppressWarnings("unchecked")
    public static ScheduledJobInfo findFirstScheduledJob(JobManager jobManager, Map<String, Object> scheduleJobProps, String scheduleId) {
        return jobManager.getScheduledJobs(null, 0, scheduleJobProps).stream()
                .filter(scheduledJobInfo -> isPackageManagerJob(scheduledJobInfo.getJobTopic()))
                .filter(scheduledJobInfo -> getScheduleId(scheduledJobInfo).equals(scheduleId))
                .findFirst()
                .orElse(null);
    }

    public static String getScheduleId(ScheduledJobInfo scheduledJobInfo) {
        return scheduledJobInfo.hashCode() + "";
    }

    public static List<ScheduledJobInfo> findAllScheduledJobs(JobManager jobManager) {
        return findAllScheduledJobs(jobManager, null).stream()
                .filter(scheduledJobInfo -> isPackageManagerJob(scheduledJobInfo.getJobTopic()))
                .filter(JobUtil::hasValidProperties)
                .collect(toList());
    }

    @SuppressWarnings("unchecked")
    public static List<ScheduledJobInfo> findAllScheduledJobs(JobManager jobManager, Map<String, Object> scheduledJobProperties) {
        return jobManager.getScheduledJobs(null, 0, scheduledJobProperties).stream()
                .filter(scheduledJobInfo -> isPackageManagerJob(scheduledJobInfo.getJobTopic()))
                .collect(toList());
    }

    private static boolean isPackageManagerJob(String jobTopic) {
        return jobTopic.startsWith(PKG_MANAGER_JOB_TOPIC_PREFIX);
    }

    public static JobBuilder.ScheduleBuilder createScheduleBuilder(String jobTopic, Map<String, Object> properties, JobManager jobManager) {
        return jobManager.createJob(jobTopic)
                .properties(properties)
                .schedule();
    }

    public static boolean addSchedule(JobBuilder.ScheduleBuilder scheduleBuilder) {
        List<String> errorMessages = new ArrayList<>();
        ScheduledJobInfo addedSchedule = scheduleBuilder.add(errorMessages);
        if (addedSchedule == null) {
            LOG.warn("Could not add schedule due to: {}", errorMessages);
            return false;
        }
        return true;
    }

    private static boolean hasValidProperties(ScheduledJobInfo scheduledJobInfo) {
        return JobProperties.fetch(scheduledJobInfo) != null;
    }

    public static boolean isScheduleObsolete(ScheduledJobInfo scheduledJobInfo) {
        Date nextScheduledExecution = scheduledJobInfo.getNextScheduledExecution();
        return nextScheduledExecution == null ||
                (!scheduledJobInfo.isSuspended() && nextScheduledExecution.before(Calendar.getInstance().getTime())); // prevention against
        // date schedules that due to some unknown circumstances didn't run as expected
    }
}
