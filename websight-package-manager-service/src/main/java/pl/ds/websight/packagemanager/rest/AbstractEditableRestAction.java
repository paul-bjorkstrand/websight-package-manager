package pl.ds.websight.packagemanager.rest;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.event.jobs.JobBuilder;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.ScheduleInfo;
import org.apache.sling.event.jobs.ScheduledJobInfo;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ds.websight.packagemanager.JcrPackageEditFacade;
import pl.ds.websight.packagemanager.JobProperties;
import pl.ds.websight.packagemanager.dto.PackageDto;
import pl.ds.websight.packagemanager.rest.schedule.Schedule;
import pl.ds.websight.packagemanager.rest.schedule.ScheduleActionType;
import pl.ds.websight.packagemanager.util.DateUtil;
import pl.ds.websight.packagemanager.util.JcrPackageUtil;
import pl.ds.websight.packagemanager.util.JobUtil;
import pl.ds.websight.packagemanager.util.OpenPackageException;
import pl.ds.websight.packagemanager.util.PackageLogUtil;
import pl.ds.websight.rest.framework.RestAction;
import pl.ds.websight.rest.framework.RestActionResult;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.io.InputStream;

import static pl.ds.websight.packagemanager.util.PackageLogUtil.LOG_PATH_PREFIX;

public abstract class AbstractEditableRestAction extends AbstractRestAction<EditPackageRestModel, PackageDto>
        implements RestAction<EditPackageRestModel, PackageDto> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractEditableRestAction.class);

    protected Packaging packaging;

    protected JobManager jobManager;

    protected abstract void setPackaging(Packaging packaging);

    protected abstract void setJobManager(JobManager jobManager);

    protected RestActionResult<PackageDto> performEdit(EditPackageRestModel model) throws IOException, RepositoryException {
        return performEdit(model, null);
    }

    protected RestActionResult<PackageDto> performEdit(EditPackageRestModel model, PackagePathSaveHelper pathSaveHelper)
            throws RepositoryException, IOException {
        Session session = model.getSession();
        String packageToEditPath = model.getEditedPackagePath();
        JcrPackage packageToEdit = null;
        JcrPackageManager packageManager = packaging.getPackageManager(session);
        try {
            packageToEdit = JcrPackageUtil.open(packageToEditPath, session, packageManager);
            JcrPackageEditFacade editFacade = JcrPackageEditFacade.forPackage(packageToEdit);
            if (editFacade == null) {
                LOG.warn("Could not edit properties in package: {}", packageToEditPath);
                return RestActionResult.failure(
                        Messages.EDIT_PACKAGE_ERROR,
                        Messages.formatMessage(Messages.EDIT_PACKAGE_ERROR_DETAILS, packageToEditPath));
            }
            editFacade.setFilters(model.getFilters());
            editFacade.setDescription(model.getDescription());
            editFacade.setAcHandling(model.getAcHandling());
            editFacade.setRequiresRestart(model.requiresRestart());
            editFacade.setDependencies(model.getDependencies());
            if (model.getDeleteThumbnail()) {
                editFacade.deleteThumbnail();
            }
            if (model.getThumbnail() != null) {
                try (InputStream thumbnailStream = model.getThumbnail().getInputStream()) {
                    editFacade.setThumbnail(thumbnailStream, model.getResourceResolver());
                }
            }

            String newName = model.getName();
            String newGroup = model.getGroup();
            String newVersion = model.getVersion();
            if (hasPackageIdChanged(packageToEdit, newName, newGroup, newVersion)) {
                packageToEdit = packageManager.rename(packageToEdit, newGroup, newName, newVersion);
                movePackageLogs(packageToEdit, packageToEditPath, session);
                if (!updatePackageActionsSchedules(packageToEdit, packageToEditPath, jobManager)) {
                    return RestActionResult.failure(Messages.EDIT_PACKAGE_ERROR,
                            Messages.formatMessage(Messages.EDIT_PACKAGE_ERROR_CAN_NOT_UPDATE_SCHEDULED_ACTIONS_DETAILS,
                                    packageToEditPath));
                }
            } else {
                session.save();
            }
            if (pathSaveHelper != null) {
                pathSaveHelper.setPathRequestAttribute(packageToEdit.getNode());
            }
            return RestActionResult.success(
                    Messages.EDIT_PACKAGE_SUCCESS,
                    Messages.formatMessage(Messages.EDIT_PACKAGE_SUCCESS_DETAILS, packageToEditPath),
                    PackageDto.wrap(packageToEdit, session, jobManager));
        } catch (AccessDeniedException e) {
            LOG.debug("User with id: {} is not allowed to edit a package on: {}", session.getUserID(), packageToEditPath, e);
            return RestActionResult.failure(
                    Messages.EDIT_PACKAGE_ERROR,
                    Messages.formatMessage(Messages.EDIT_PACKAGE_ERROR_ACCESS_DENIED_DETAILS, packageToEditPath));
        } catch (PackageException e) {
            LOG.warn("Could not rename a package in: {}", packageToEditPath, e);
            return RestActionResult.failure(
                    Messages.EDIT_PACKAGE_ERROR,
                    Messages.formatMessage(Messages.EDIT_PACKAGE_ERROR_CAN_NOT_RENAME_DETAILS, packageToEditPath));
        } catch (OpenPackageException e) {
            LOG.warn("Could not open package: {}", packageToEditPath, e);
            return RestActionResult.failure(e.getSimplifiedMessage(), e.getMessage());
        } finally {
            JcrPackageUtil.close(packageToEdit);
        }
    }

    private static boolean hasPackageIdChanged(JcrPackage jcrPackage, String newName, String newGroup, String newVersion) {
        return JcrPackageUtil.fetchDefinition(jcrPackage)
                .map(JcrPackageDefinition::getId)
                .map(packageId -> !packageId.getName().equals(newName) ||
                        !packageId.getGroup().equals(newGroup) ||
                        !packageId.getVersionString().equals(newVersion))
                .orElse(false);
    }

    private static void movePackageLogs(JcrPackage packageRenamed, String oldPackagePath, Session session) throws RepositoryException {
        String oldLogPath = PackageLogUtil.getLogPath(oldPackagePath);
        if (session.nodeExists(oldLogPath)) {
            Node renamedPackageNode = packageRenamed.getNode();
            if (renamedPackageNode != null) {
                String logPath = PackageLogUtil.getLogPath(renamedPackageNode.getPath());
                if (anyIntermediateLogNodeNotExist(logPath, session)) {
                    createAllIntermediateLogNodes(logPath, session);
                } else if (session.nodeExists(logPath)) {
                    session.removeItem(logPath);
                    session.save();
                }
                session.move(oldLogPath, logPath);
                session.save();
            }
        }
    }

    private static boolean anyIntermediateLogNodeNotExist(String absLogPath, Session session) throws RepositoryException {
        String logPathWithoutPackageNode = absLogPath.substring(0, absLogPath.lastIndexOf('/'));
        return !session.nodeExists(logPathWithoutPackageNode);
    }

    private static void createAllIntermediateLogNodes(String absLogPath, Session session) throws RepositoryException {
        String relLogPath = absoluteToRelativeNodePath(absLogPath);
        if (!relLogPath.contains("/")) {
            return;
        }
        String[] subNodeNames = splitToIntermediateNodeNames(relLogPath);
        Node node = session.getNode(LOG_PATH_PREFIX);
        for (String subNodeName : subNodeNames) {
            if (node.hasNode(subNodeName)) {
                node = node.getNode(subNodeName);
            } else {
                node = node.addNode(subNodeName);
                node.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, PackageLogUtil.RT_LOG_INTERMEDIATE_NODES);
            }
        }
        session.save();
    }

    private static String absoluteToRelativeNodePath(String absLogPath) {
        return StringUtils.removeStart(absLogPath, LOG_PATH_PREFIX);
    }

    private static String[] splitToIntermediateNodeNames(String relLogPath) {
        String[] subNodeNames = relLogPath.split("/");
        return ArrayUtils.remove(subNodeNames, subNodeNames.length - 1);
    }

    private static boolean updatePackageActionsSchedules(JcrPackage packageRenamed, String oldPackagePath, JobManager jobManager)
            throws RepositoryException {
        Node renamedPackageNode = packageRenamed.getNode();
        if (renamedPackageNode == null) {
            LOG.warn("Could not access renamed package node, so update of scheduled jobs will be skipped");
            return true;
        }
        String newPackagePath = renamedPackageNode.getPath();
        for (ScheduledJobInfo scheduledJobToUpdate : JobUtil.findAllScheduledJobs(jobManager,
                JobProperties.asQueryMap(oldPackagePath))) {
            ScheduleActionType scheduleActionType = ScheduleActionType.from(scheduledJobToUpdate.getJobTopic());
            if (scheduleActionType == null) {
                LOG.warn("Could not update scheduled unknown action for package: {}", newPackagePath);
                return false;
            }
            String scheduleActionName = scheduleActionType.getFullName().toLowerCase(JcrPackageUtil.DEFAULT_LOCALE);
            String applicantId = JobProperties.getApplicantId(scheduledJobToUpdate);
            if (applicantId == null) {
                LOG.warn("Could not update scheduled {} for package: {} due to lack of info about applicant Id", scheduleActionName,
                        oldPackagePath);
                return false;
            }
            JobBuilder.ScheduleBuilder updatedScheduleBuilder = JobUtil.createScheduleBuilder(scheduledJobToUpdate.getJobTopic(),
                    scheduleActionType.getJobProperties(newPackagePath, applicantId), jobManager);

            if (scheduledJobToUpdate.isSuspended()) {
                updatedScheduleBuilder.suspend();
            }
            for (ScheduleInfo scheduleInfo : scheduledJobToUpdate.getSchedules()) {
                Schedule schedule = Schedule.createSchedule(DateUtil.format(scheduleInfo.getAt()), scheduleInfo.getExpression());
                if (schedule.getError() != null) {
                    LOG.warn("Could not update one of schedules of scheduled {} for package: {}", scheduleActionName, newPackagePath);
                    return false;
                }
                schedule.addToBuilder(updatedScheduleBuilder);
            }
            scheduledJobToUpdate.unschedule();
            LOG.debug("Successfully unscheduled {} with deprecated package path: {} instead of new one: {}",
                    scheduleActionName, oldPackagePath, newPackagePath);

            if (!JobUtil.addSchedule(updatedScheduleBuilder)) {
                LOG.warn("Could not schedule {} for package {}", scheduleActionName, newPackagePath);
                return false;
            }
        }
        return true;
    }

    @Override
    protected String getUnexpectedErrorMessage() {
        return Messages.EDIT_PACKAGE_ERROR;
    }
}
