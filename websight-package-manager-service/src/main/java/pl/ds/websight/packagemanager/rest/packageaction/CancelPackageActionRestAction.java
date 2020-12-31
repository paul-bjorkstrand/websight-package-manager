package pl.ds.websight.packagemanager.rest.packageaction;

import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ds.websight.packagemanager.packageaction.PackageActionJobProperties;
import pl.ds.websight.packagemanager.rest.AbstractRestAction;
import pl.ds.websight.packagemanager.rest.Messages;
import pl.ds.websight.packagemanager.rest.requestparameters.PackageActionCommand;
import pl.ds.websight.packagemanager.util.JcrPackageUtil;
import pl.ds.websight.packagemanager.util.JobUtil;
import pl.ds.websight.rest.framework.RestAction;
import pl.ds.websight.rest.framework.RestActionResult;
import pl.ds.websight.rest.framework.annotations.SlingAction;

import java.util.List;

@Component
@SlingAction
public class CancelPackageActionRestAction extends AbstractRestAction<PackageActionRestModel, Void>
        implements RestAction<PackageActionRestModel, Void> {

    private static final Logger LOG = LoggerFactory.getLogger(CancelPackageActionRestAction.class);

    @Reference
    private JobManager jobManager;

    @Override
    protected RestActionResult<Void> performAction(PackageActionRestModel model) {
        String packagePath = model.getPath();
        List<Job> allQueuedJobs = JobUtil.findAllQueuedJobs(jobManager, PackageActionJobProperties.asQueryMap(packagePath));
        if (allQueuedJobs.isEmpty()) {
            LOG.warn("Could not cancel jobs for package {} due to lack of queued actions", packagePath);
            return RestActionResult.failure(Messages.CANCEL_PACKAGE_ACTION_ERROR_NO_ACTIONS,
                    Messages.formatMessage(Messages.CANCEL_PACKAGE_ACTION_ERROR_NO_ACTIONS_DETAILS, packagePath));
        }
        Job lastQueuedJob = null;
        for (Job queuedJob : allQueuedJobs) {
            lastQueuedJob = queuedJob;
            String jobId = queuedJob.getId();
            LOG.debug("Trying to delete a job for package {} with jobId {} and topic {}", packagePath, jobId, queuedJob.getTopic());
            if (!jobManager.removeJobById(jobId)) {
                LOG.warn("Could not delete a job for package {} with jobId {} and topic {}", packagePath, jobId, queuedJob.getTopic());
                String formattedActionTitle = getFormattedActionTitle(queuedJob);
                return RestActionResult.failure(
                        Messages.formatMessage(Messages.CANCEL_PACKAGE_ACTION_ERROR_UNSUCCESSFUL_DELETE,
                                formattedActionTitle.toLowerCase(JcrPackageUtil.DEFAULT_LOCALE)),
                        Messages.formatMessage(Messages.CANCEL_PACKAGE_ACTION_ERROR_UNSUCCESSFUL_DELETE_DETAILS,
                                formattedActionTitle.toLowerCase(JcrPackageUtil.DEFAULT_LOCALE), packagePath));
            }
        }
        String formattedLastActionTitle = getFormattedActionTitle(lastQueuedJob);
        // Please bear in mind, that deleting a job doesn't mean the processing of consumer/handler is stopped when job is active.
        // The work requested by job will be fully executed, but the job will not be visible in persistence layer, so in some edge cases
        // related to JCR thread race condition action cancellation could provide some guise, that cancelled job won't be executed, but
        // it's too late to stop processing job
        return RestActionResult.success(
                Messages.formatMessage(Messages.CANCEL_PACKAGE_ACTION_SUCCESS, formattedLastActionTitle, packagePath),
                Messages.formatMessage(Messages.CANCEL_PACKAGE_ACTION_SUCCESS_DETAILS,
                        formattedLastActionTitle.toLowerCase(JcrPackageUtil.DEFAULT_LOCALE), packagePath));
    }

    private static String getFormattedActionTitle(Job job) {
        if (job != null) {
            PackageActionCommand packageActionCommand = PackageActionCommand.fetchByJobTopic(job.getTopic());
            if (packageActionCommand != null) {
                return packageActionCommand.getActionTitle();
            }
        }
        return "'unknown action'";
    }

    @Override
    protected String getUnexpectedErrorMessage() {
        return Messages.CANCEL_PACKAGE_ACTION_ERROR;
    }

}
