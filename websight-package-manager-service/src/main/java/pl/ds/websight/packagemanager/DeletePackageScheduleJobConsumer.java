package pl.ds.websight.packagemanager;

import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ds.websight.packagemanager.rest.DeletePackageRestAction;
import pl.ds.websight.packagemanager.util.JobUtil;
import pl.ds.websight.packagemanager.util.OpenPackageException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

@Component(service = JobConsumer.class,
        property = {JobConsumer.PROPERTY_TOPICS + '=' + DeletePackageScheduleJobConsumer.TOPIC})
public class DeletePackageScheduleJobConsumer implements JobConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(DeletePackageScheduleJobConsumer.class);

    public static final String TOPIC = JobUtil.PKG_MANAGER_JOB_TOPIC_PREFIX + "delete";

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private Packaging packaging;

    @Override
    public JobResult process(Job job) {
        JobProperties jobProperties = JobProperties.fetch(job);
        if (jobProperties == null) {
            LOG.warn("Invalid properties structure for {}", job.getTopic());
            return JobResult.CANCEL;
        }
        String packageReference = jobProperties.getPackageReference();
        String applicantId = jobProperties.getApplicantId();
        LOG.debug("Acquired job properties for package deletion on path: {}, requested by user: {}, starting processing package operations",
                packageReference, applicantId);

        try (ResourceResolver applicantResolver = JobUtil.getImpersonatedResolver(resolverFactory, applicantId)) {
            if (applicantResolver == null) {
                LOG.warn("Failed to use impersonated resource resolver for applicant: {}", applicantId);
                return JobResult.FAILED;
            }
            Session applicantSession = applicantResolver.adaptTo(Session.class);
            if (applicantSession == null) {
                LOG.warn("Could not access session for applicant: {}", applicantId);
                return JobResult.FAILED;
            } else if (!applicantSession.nodeExists(packageReference)) {
                LOG.warn("Package: {} was not found, so it cannot be deleted", packageReference);
                return JobResult.FAILED;
            }
            JcrPackageManager packageManager = packaging.getPackageManager(applicantSession);
            DeletePackageRestAction.processDelete(packageReference, applicantSession, packageManager);
        } catch (RepositoryException | OpenPackageException e) {
            LOG.warn("Could not perform scheduled delete for package: {}, requested by user: {}", packageReference, applicantId, e);
            return JobResult.FAILED;
        }
        return JobResult.OK;
    }
}
