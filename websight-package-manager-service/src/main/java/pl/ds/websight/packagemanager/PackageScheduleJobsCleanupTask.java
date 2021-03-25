package pl.ds.websight.packagemanager;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.ScheduleInfo;
import org.apache.sling.event.jobs.ScheduledJobInfo;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ds.websight.packagemanager.rest.schedule.ScheduleActionType;
import pl.ds.websight.packagemanager.util.DateUtil;
import pl.ds.websight.packagemanager.util.JcrPackageUtil;
import pl.ds.websight.packagemanager.util.JobUtil;

import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

@Component(service = Runnable.class, immediate = true)
@Designate(ocd = PackageScheduleJobsCleanupTask.Config.class)
public final class PackageScheduleJobsCleanupTask implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(PackageScheduleJobsCleanupTask.class);

    @Reference
    private JobManager jobManager;

    @Activate
    protected void activate(final Config config) {
        // required
    }

    @Override
    public void run() {
        for (ScheduledJobInfo scheduledJobInfo : JobUtil.findAllScheduledJobs(jobManager)) {
            if (JobUtil.isScheduleObsolete(scheduledJobInfo)) {
                if (LOG.isDebugEnabled()) {
                    logUnscheduling(scheduledJobInfo);
                }
                scheduledJobInfo.unschedule();
            }
        }
    }

    private static void logUnscheduling(ScheduledJobInfo scheduledJobInfo) {
        LOG.debug("No next execution date for schedule before now: {}, so scheduled job won't be ever executed",
                Calendar.getInstance().getTime());
        String scheduleActionName = ScheduleActionType.getFullName(scheduledJobInfo.getJobTopic())
                .toLowerCase(JcrPackageUtil.DEFAULT_LOCALE);
        JobProperties jobProperties = JobProperties.fetch(scheduledJobInfo);
        if (jobProperties == null) {
            LOG.debug("Deleting scheduled {} with unknown properties", scheduleActionName);
            return;
        }
        List<String> schedules = scheduledJobInfo.getSchedules().stream()
                .map(PackageScheduleJobsCleanupTask::getScheduleValue)
                .filter(Objects::nonNull)
                .collect(toList());
        LOG.debug("Deleting scheduled {} for package: {}, requested by user: {} with schedules: {}", scheduleActionName,
                jobProperties.getPackageReference(), jobProperties.getApplicantId(), schedules);
    }

    private static String getScheduleValue(ScheduleInfo scheduleInfo) {
        return StringUtils.isNotBlank(scheduleInfo.getExpression()) ?
                scheduleInfo.getExpression() :
                DateUtil.format(scheduleInfo.getAt());
    }

    @ObjectClassDefinition(name = "Websight Package Schedules Cleanup Job",
            description = "Periodic Package Schedules Cleanup Job")
    @interface Config {

        @AttributeDefinition(description = "Cron expression scheduling this job. Default is daily after 00:00:00." +
                "See http://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html " +
                "for a description of the format for this value.")
        String scheduler_expression() default "0 0 0 * * ?";

        @AttributeDefinition(name = "Concurrent task",
                description = "Allow Package Action Schedule Cleanup Task to run concurrently")
        boolean scheduler_concurrent() default false;
    }
}
