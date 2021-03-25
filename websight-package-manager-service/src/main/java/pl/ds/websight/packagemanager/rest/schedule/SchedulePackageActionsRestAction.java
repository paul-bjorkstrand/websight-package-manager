package pl.ds.websight.packagemanager.rest.schedule;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.event.jobs.JobBuilder.ScheduleBuilder;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.ScheduledJobInfo;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ds.websight.packagemanager.JobProperties;
import pl.ds.websight.packagemanager.rest.AbstractRestAction;
import pl.ds.websight.packagemanager.rest.Messages;
import pl.ds.websight.packagemanager.rest.PackagePrerequisiteValidator;
import pl.ds.websight.packagemanager.rest.schedule.SchedulePackageActionsRestModel.ScheduleAction;
import pl.ds.websight.packagemanager.util.JcrPackageUtil;
import pl.ds.websight.packagemanager.util.JobUtil;
import pl.ds.websight.packagemanager.util.OpenPackageException;
import pl.ds.websight.rest.framework.RestAction;
import pl.ds.websight.rest.framework.RestActionResult;
import pl.ds.websight.rest.framework.annotations.SlingAction;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@SlingAction
public class SchedulePackageActionsRestAction extends AbstractRestAction<SchedulePackageActionsRestModel, Void>
        implements RestAction<SchedulePackageActionsRestModel, Void> {

    private static final Logger LOG = LoggerFactory.getLogger(SchedulePackageActionsRestAction.class);

    @Reference
    private JobManager jobManager;

    @Reference
    private Packaging packaging;

    @Override
    protected RestActionResult<Void> performAction(SchedulePackageActionsRestModel model) throws RepositoryException {
        String path = model.getPath();
        Session session = model.getSession();
        Map<String, Object> packageRefMap = JobProperties.asQueryMap(path);
        try (JcrPackage packageToAddSchedules = JcrPackageUtil.open(path, session, packaging.getPackageManager(session))) {
            List<ScheduledJobInfo> updatedScheduledJobs = new ArrayList<>();
            RestActionResult<Void> updateScheduleJobsResult =
                    updateEditedSchedulesJobs(model.getScheduledActionsToUpdate(), path, packageRefMap, updatedScheduledJobs);
            if (updateScheduleJobsResult != null) {
                return updateScheduleJobsResult;
            }
            deleteNotUpdatedScheduleJobs(path, packageRefMap, updatedScheduledJobs);
            RestActionResult<Void> addScheduleJobsResult =
                    addNewScheduleActions(model.getNewActionsToSchedule(), path, session, packageRefMap, packageToAddSchedules);
            if (addScheduleJobsResult != null) {
                return addScheduleJobsResult;
            }
        } catch (OpenPackageException e) {
            LOG.warn("Could not open package: {}", path, e);
            return RestActionResult.failure(e.getSimplifiedMessage(), e.getMessage());
        }
        return RestActionResult.success(Messages.SCHEDULE_PACKAGE_ACTIONS_SUCCESS,
                Messages.formatMessage(Messages.SCHEDULE_PACKAGE_ACTIONS_SUCCESS_DETAILS, path));
    }

    private RestActionResult<Void> updateEditedSchedulesJobs(List<ScheduleAction> actionsToUpdate, String path,
            Map<String, Object> packageRefMap, List<ScheduledJobInfo> updatedScheduledJobs) {
        for (ScheduleAction actionToUpdate : actionsToUpdate) {
            String scheduleId = actionToUpdate.getScheduleId();
            ScheduledJobInfo scheduledJobToUpdate = JobUtil.findFirstScheduledJob(jobManager, packageRefMap, scheduleId);
            if (scheduledJobToUpdate == null) {
                return failure(Messages.EDIT_SCHEDULED_PACKAGE_ACTIONS_ERROR_NO_SCHEDULE_DETAILS, scheduleId, path);
            }
            ScheduleActionType scheduleAction = ScheduleActionType.from(scheduledJobToUpdate.getJobTopic());
            if (!actionToUpdate.getActionType().equals(scheduleAction)) {
                return failure(Messages.SCHEDULE_PACKAGE_ACTIONS_ERROR_CHANGED_ACTION_IN_SCHEDULED_JOB_DETAILS, path);
            }
            String scheduleActionName = ScheduleActionType.getFullName(scheduledJobToUpdate.getJobTopic());

            ScheduleBuilder scheduleEditBuilder = scheduledJobToUpdate.reschedule();
            if (actionToUpdate.isSuspended()) {
                scheduleEditBuilder.suspend();
            }
            List<Schedule> schedules = actionToUpdate.getSchedules();
            schedules.forEach(schedule -> schedule.addToBuilder(scheduleEditBuilder));
            List<String> errorMessages = new ArrayList<>();
            ScheduledJobInfo updatedScheduleJob = scheduleEditBuilder.add(errorMessages);
            if (updatedScheduleJob == null) {
                LOG.warn("Could not reschedule {} for package {} due to: {}", scheduleActionName, path, errorMessages);
                return failure(Messages.EDIT_SCHEDULED_PACKAGE_ACTIONS_ERROR_NOT_SCHEDULED_DETAILS, scheduleActionName, path);
            }
            if (!actionToUpdate.isSuspended()) {
                updatedScheduleJob.resume();
            }
            updatedScheduledJobs.add(updatedScheduleJob);
            LOG.debug("Successfully rescheduled {} with updated schedules {} for package {}", scheduleActionName, schedules, path);
        }
        return null;
    }

    private void deleteNotUpdatedScheduleJobs(String path, Map<String, Object> queryPropsMap, List<ScheduledJobInfo> updatedScheduledJobs) {
        for (ScheduledJobInfo scheduledJobInfo : JobUtil.findAllScheduledJobs(jobManager, queryPropsMap)) {
            if (!updatedScheduledJobs.contains(scheduledJobInfo)) {
                scheduledJobInfo.unschedule();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Successfully deleted scheduled {} with schedules: {} for package: {}",
                            ScheduleActionType.getFullName(scheduledJobInfo.getJobTopic()).toLowerCase(JcrPackageUtil.DEFAULT_LOCALE),
                            scheduledJobInfo.getSchedules(), path);
                }
            }
        }
    }

    private RestActionResult<Void> addNewScheduleActions(List<ScheduleAction> actionsToAdd, String path, Session session,
            Map<String, Object> queryPropsMap, JcrPackage packageToAddSchedules) {
        for (ScheduleAction scheduleActionToAdd : actionsToAdd) {
            ScheduleActionType scheduleActionType = scheduleActionToAdd.getActionType();
            Pair<String, String> validationResult = PackagePrerequisiteValidator.getValidationResult(scheduleActionType.getValidators(),
                    packageToAddSchedules, path);
            if (!validationResult.equals(ImmutablePair.nullPair())) {
                return RestActionResult.failure(validationResult.getKey(), validationResult.getValue());
            }
            String jobTopic = scheduleActionType.getJobTopic();
            List<Schedule> schedules = scheduleActionToAdd.getSchedules();
            ScheduledJobInfo alreadyScheduledJob = JobUtil.findScheduledJobWithSchedules(jobManager, queryPropsMap, schedules, jobTopic);
            String scheduleActionFullName = scheduleActionType.getFullName();
            if (alreadyScheduledJob != null) {
                return failure(Messages.SCHEDULE_PACKAGE_ACTIONS_ERROR_ALREADY_SCHEDULED_DETAILS,
                        StringUtils.capitalize(scheduleActionFullName), path);
            }
            ScheduleBuilder scheduleBuilder = JobUtil.createScheduleBuilder(jobTopic,
                    scheduleActionType.getJobProperties(path, session.getUserID()), jobManager);
            schedules.forEach(schedule -> schedule.addToBuilder(scheduleBuilder));
            if (scheduleActionToAdd.isSuspended()) {
                scheduleBuilder.suspend();
            }
            String formattedFullName = scheduleActionFullName.toLowerCase(JcrPackageUtil.DEFAULT_LOCALE);
            if (!JobUtil.addSchedule(scheduleBuilder)) {
                LOG.warn("Could not schedule {} for package: {}", formattedFullName, path);
                return failure(Messages.SCHEDULE_PACKAGE_ACTIONS_ERROR_NOT_SCHEDULED_DETAILS, formattedFullName, path);
            }
            LOG.debug("Scheduled {} of package: {}, used schedules: {}", formattedFullName, path, schedules);
        }
        return null;
    }

    private RestActionResult<Void> failure(String message, Object... args) {
        return RestActionResult.failure(Messages.SCHEDULE_PACKAGE_ACTIONS_ERROR, Messages.formatMessage(message, args));
    }

    @Override
    protected String getUnexpectedErrorMessage() {
        return Messages.SCHEDULE_PACKAGE_ACTIONS_ERROR;
    }
}
