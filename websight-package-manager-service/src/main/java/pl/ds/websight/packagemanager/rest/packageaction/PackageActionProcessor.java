package pl.ds.websight.packagemanager.rest.packageaction;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ds.websight.packagemanager.dto.CombinedActionsDto;
import pl.ds.websight.packagemanager.dto.PackageActionStateDto;
import pl.ds.websight.packagemanager.packageaction.PackageActionJobProperties;
import pl.ds.websight.packagemanager.rest.Messages;
import pl.ds.websight.packagemanager.packageoptions.PackageImportOptions;
import pl.ds.websight.packagemanager.rest.PackagePrerequisiteValidator;
import pl.ds.websight.packagemanager.rest.requestparameters.PackageActionCommand;
import pl.ds.websight.packagemanager.util.JcrPackageUtil;
import pl.ds.websight.packagemanager.util.JobUtil;
import pl.ds.websight.packagemanager.util.OpenPackageException;
import pl.ds.websight.rest.framework.RestActionResult;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Collections;
import java.util.Map;

@Component(service = PackageActionProcessor.class)
public class PackageActionProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(PackageActionProcessor.class);

    private static final Map<String, Boolean> FIRST_ACTION_DONE_FLAG = Collections.singletonMap("firstActionDone", true);

    @Reference
    private Packaging packaging;

    @Reference
    private JobManager jobManager;

    public RestActionResult<PackageActionStateDto> process(String packageToProcessPath, PackageImportOptions packageImportOptions,
            Session session, PackageActionCommand command, PackagePrerequisiteValidator... validators) throws RepositoryException {
        try (JcrPackage packageToProcess = JcrPackageUtil.open(packageToProcessPath, session, packaging.getPackageManager(session))) {
            Pair<String, String> validationResult =
                    PackagePrerequisiteValidator.getValidationResult(validators, packageToProcess, packageToProcessPath);
            if (!validationResult.equals(ImmutablePair.nullPair())) {
                return RestActionResult.failure(validationResult.getKey(), validationResult.getValue());
            }
            Job runningQueuedJob =
                    JobUtil.findFirstRunningOrQueuedJob(jobManager, PackageActionJobProperties.asQueryMap(packageToProcessPath));
            if (runningQueuedJob != null) {
                PackageActionCommand actionBlocker = PackageActionCommand.fetchByJobTopic(runningQueuedJob.getTopic());
                String actionBlockerName = actionBlocker != null ?
                        actionBlocker.getActionTitle().toLowerCase(JcrPackageUtil.DEFAULT_LOCALE) :
                        null;
                return RestActionResult.failure(
                        Messages.formatMessage(Messages.PACKAGE_ACTION_ERROR, command.toString()),
                        Messages.formatMessage(Messages.PACKAGE_ACTION_ERROR_ALREADY_USED_DETAILS,
                                StringUtils.capitalize(actionBlockerName), packageToProcessPath));
            }
            Job actionJob = jobManager.addJob(command.getJobTopic(),
                    PackageActionJobProperties.toMap(packageToProcessPath, packageImportOptions, session.getUserID()));
            if (actionJob == null) {
                LOG.warn("Could not queue package action: {} of package: {}", command, packageToProcessPath);
                return RestActionResult.failure(
                        Messages.formatMessage(Messages.PACKAGE_ACTION_ERROR, command),
                        Messages.formatMessage(Messages.PACKAGE_ACTION_ERROR_NOT_QUEUED_DETAILS,
                                command.getActionTitle().toLowerCase(JcrPackageUtil.DEFAULT_LOCALE)));
            }
            LOG.debug("Successfully queued action: {} of package: {}, Id: {}", command, packageToProcessPath, actionJob.getId());
            return RestActionResult.success(
                    Messages.formatMessage(Messages.PACKAGE_ACTION_SUCCESS, command.getActionTitle()),
                    Messages.formatMessage(Messages.PACKAGE_ACTION_SUCCESS_DETAILS, command.getActionTitle(),
                            JcrPackageUtil.getSimplePackageName(packageToProcess)),
                    PackageActionStateDto.getState(actionJob.getJobState()));
        } catch (OpenPackageException e) {
            LOG.warn("Could not open package: {}", packageToProcessPath, e);
            return RestActionResult.failure(e.getSimplifiedMessage(), e.getMessage());
        }
    }

    public RestActionResult processAfterPreviousAction(String packageToProcessPath, Session session,
            PackageActionCommand command, String previousDoneActionMsgText, PackagePrerequisiteValidator... validators)
            throws RepositoryException {
        String lowerCaseActionTitle = command.getActionTitle().toLowerCase(JcrPackageUtil.DEFAULT_LOCALE);
        if (StringUtils.isBlank(packageToProcessPath)) {
            LOG.warn("Path was not provided, could not queue {}", lowerCaseActionTitle);
            return RestActionResult.failure(
                    Messages.formatMessage(Messages.COMBINED_PACKAGE_ACTIONS_ERROR_NO_SAVED_STATE_FIRST_ACTION,
                            previousDoneActionMsgText, lowerCaseActionTitle),
                    Messages.formatMessage(Messages.COMBINED_PACKAGE_ACTIONS_NO_SAVED_STATE_FIRST_ACTION_DETAILS,
                            previousDoneActionMsgText, lowerCaseActionTitle),
                    FIRST_ACTION_DONE_FLAG);
        }
        RestActionResult<?> actionQueuedResult = process(packageToProcessPath, PackageImportOptions.DEFAULT, session, command, validators);
        if (RestActionResult.Status.FAILURE == actionQueuedResult.getStatus()) {
            LOG.warn("Previous action on package was successful, but {} queuing failed", lowerCaseActionTitle);
            return RestActionResult.failure(
                    Messages.formatMessage(Messages.COMBINED_PACKAGE_ACTIONS_ERROR_SECOND_ACTION, previousDoneActionMsgText,
                            lowerCaseActionTitle),
                    actionQueuedResult.getMessageDetails(),
                    FIRST_ACTION_DONE_FLAG);
        }
        return RestActionResult.success(
                Messages.formatMessage(Messages.COMBINED_PACKAGE_ACTIONS_SUCCESS, previousDoneActionMsgText, lowerCaseActionTitle),
                Messages.formatMessage(Messages.COMBINED_PACKAGE_ACTIONS_SUCCESS_DETAILS, previousDoneActionMsgText, lowerCaseActionTitle,
                        packageToProcessPath),
                new CombinedActionsDto(packageToProcessPath, PackageActionStateDto.getState(actionQueuedResult.getEntity().toString())));
    }

}
