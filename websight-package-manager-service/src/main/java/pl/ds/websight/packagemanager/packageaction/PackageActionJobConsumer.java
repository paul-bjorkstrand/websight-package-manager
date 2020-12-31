package pl.ds.websight.packagemanager.packageaction;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.jackrabbit.vault.fs.api.ProgressTrackerListener;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ds.websight.packagemanager.rest.requestparameters.PackageActionCommand;
import pl.ds.websight.packagemanager.util.JcrPackageUtil;
import pl.ds.websight.packagemanager.util.JobUtil;
import pl.ds.websight.packagemanager.util.PackageLogUtil;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Calendar;

@Component(
        service = JobConsumer.class,
        property = {
                JobConsumer.PROPERTY_TOPICS + '=' + PackageActionJobConsumer.INSTALL_TOPIC,
                JobConsumer.PROPERTY_TOPICS + '=' + PackageActionJobConsumer.UNINSTALL_TOPIC,
                JobConsumer.PROPERTY_TOPICS + '=' + PackageActionJobConsumer.BUILD_TOPIC,
                JobConsumer.PROPERTY_TOPICS + '=' + PackageActionJobConsumer.COVERAGE_TOPIC
        })
public class PackageActionJobConsumer implements JobConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(PackageActionJobConsumer.class);

    public static final String INSTALL_TOPIC = JobUtil.PKG_MANAGER_JOB_TOPIC_PREFIX + "install";
    public static final String UNINSTALL_TOPIC = JobUtil.PKG_MANAGER_JOB_TOPIC_PREFIX + "uninstall";
    public static final String BUILD_TOPIC = JobUtil.PKG_MANAGER_JOB_TOPIC_PREFIX + "build";
    public static final String COVERAGE_TOPIC = JobUtil.PKG_MANAGER_JOB_TOPIC_PREFIX + "coverage";

    private static final String START_DATE_PATTERN = "EEE MMM dd HH:mm:ss 'GMT'Z '('zzzz')'";

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private SlingRepository repository;

    @Reference
    private Packaging packaging;

    @Reference
    private DynamicClassLoaderManager classLoaderManager;

    @Override
    public JobResult process(Job job) {
        PackageActionJobProperties properties = PackageActionJobProperties.fetch(job);
        if (properties == null) {
            LOG.warn("Invalid properties structure for {}", job.getTopic());
            return JobResult.CANCEL;
        }
        PackageActionCommand command = PackageActionCommand.fetchByJobTopic(job.getTopic());
        String applicantId = properties.getApplicantId();
        String packageReference = properties.getPackageReference();
        LOG.debug("Acquired action job properties for package: {} and user: {}, starting processing action pre operations",
                packageReference, applicantId);
        PackageActionJobFinishedHandler actionFinishedHandler =
                new PackageActionJobFinishedHandler(resolverFactory, properties, job.getId(), command);
        try (ResourceResolver applicantResolver = JobUtil.getImpersonatedResolver(resolverFactory, applicantId)) {
            if (applicantResolver == null) {
                LOG.warn("Failed to use impersonated resource resolver for applicant: {}", applicantId);
                handleFailedFinish(actionFinishedHandler, command, "Failed to get " + applicantId + " resource resolver");
                return JobResult.FAILED;
            }
            Session applicantSession = applicantResolver.adaptTo(Session.class);
            if (applicantSession == null) {
                handleFailedFinish(actionFinishedHandler, command, "Could not access session for applicant: " + applicantId);
                return JobResult.FAILED;
            } else if (!applicantSession.nodeExists(packageReference)) {
                handleFailedFinish(actionFinishedHandler, command, "Package: " + packageReference + " was not found");
                return JobResult.FAILED;
            }
            String logNodePath = properties.getLogPath();
            deleteLog(applicantSession, logNodePath);
            applicantSession.save();
            LOG.debug("Removed successfully log for package: {}", logNodePath);

            ModifiableValueMap logValueMap = PackageLogUtil.getOrCreatePackageLogMap(applicantResolver, logNodePath);
            if (logValueMap == null) {
                handleFailedFinish(actionFinishedHandler, command, "Could not adapt action log under " + logNodePath + " to " +
                        ModifiableValueMap.class.getSimpleName());
                return JobResult.FAILED;
            }
            PackageLogUtil.updateActionInfo(command.toString(), applicantId, logValueMap);
            LOG.debug("Processed all action pre operations for package: {}, starting action: {}", packageReference, command);
            PackageActionLogProgressListener progressListener = new PackageActionLogProgressListener(logValueMap, applicantResolver,
                    resolverFactory.getSearchPath(), job.getId());
            return processPackageAction(actionFinishedHandler, properties, job.getProcessingStarted(), progressListener, applicantSession,
                    command);
        } catch (Exception e) {
            LOG.warn("Could not perform action: {} on package: {}", command, packageReference, e);
            handleFailedFinish(actionFinishedHandler, command, e);
        }
        return JobResult.FAILED;
    }

    private static void handleFailedFinish(PackageActionJobFinishedHandler handler, PackageActionCommand command, String exceptionMsg) {
        handler.handleFinish(String.format("Package %s failed.", command.toString()), exceptionMsg);
    }

    private static void deleteLog(Session applicantSession, String logNodePath) throws RepositoryException {
        if (applicantSession.nodeExists(logNodePath)) {
            applicantSession.removeItem(logNodePath);
        }
    }

    private JobResult processPackageAction(PackageActionJobFinishedHandler handler, PackageActionJobProperties properties, Calendar jobStart,
            PackageActionLogProgressListener listener, Session applicantSession,
            PackageActionCommand commandToExecute) {
        String packageReference = properties.getPackageReference();
        boolean isDryRun = properties.isDryRun();
        boolean isActionExecutionSuccessful = false;
        JcrPackageManager packageManager = packaging.getPackageManager(applicantSession);
        try (JcrPackage packageToProcess = JcrPackageUtil.open(packageReference, applicantSession, packageManager)) {
            appendPackageActionHeader(listener, packageReference, isDryRun, jobStart, commandToExecute.getLogPrefix(),
                    commandToExecute.getDescription());
            commandToExecute.executeCommand(packageToProcess, listener, classLoaderManager.getDynamicClassLoader(), packageManager,
                    isDryRun);
            isActionExecutionSuccessful = true;
            LOG.debug("{} of package {} finished successfully", commandToExecute, packageReference);
            return JobResult.OK;
        } catch (Exception e) {
            listener.flushUnsavedData();
            LOG.warn("Could not perform action: {} on package: {}", commandToExecute, packageReference, e);
            handleFailedFinish(handler, commandToExecute, e);
            return JobResult.FAILED;
        } finally {
            if (isActionExecutionSuccessful) {
                listener.flushUnsavedData();
                handler.handleFinish(String.format("%s in %dms.", commandToExecute.getLogSuffix(),
                        Math.abs(System.currentTimeMillis() - jobStart.getTimeInMillis())));
            }
        }
    }

    private static void appendPackageActionHeader(ProgressTrackerListener listener, String packageReference, boolean dryRun,
            Calendar jobStart, String commandPrefix, String commandDescription) {
        String commandLogPrefix = (dryRun ? "Test " : "") + commandPrefix;
        listener.onMessage(ProgressTrackerListener.Mode.TEXT, commandLogPrefix, packageReference);
        listener.onMessage(ProgressTrackerListener.Mode.TEXT,
                DateFormatUtils.format(jobStart, START_DATE_PATTERN, jobStart.getTimeZone(), JcrPackageUtil.DEFAULT_LOCALE), null);
        // add one empty line between logs for package action and log header:
        listener.onMessage(ProgressTrackerListener.Mode.TEXT, "", null);
        listener.onMessage(ProgressTrackerListener.Mode.TEXT, commandDescription + (dryRun ? " (dry run)" : ""), null);
    }

    private static void handleFailedFinish(PackageActionJobFinishedHandler handler, PackageActionCommand command, Exception e) {
        handler.handleFinish(String.format("Package %s failed.", command.toString()), ExceptionUtils.getStackTrace(e));
    }
}
