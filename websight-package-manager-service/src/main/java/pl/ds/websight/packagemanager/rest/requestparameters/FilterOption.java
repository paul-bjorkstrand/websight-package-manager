package pl.ds.websight.packagemanager.rest.requestparameters;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.util.JcrConstants;
import org.apache.sling.event.jobs.JobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ds.websight.packagemanager.JobProperties;
import pl.ds.websight.packagemanager.util.JcrPackageStatusUtil;
import pl.ds.websight.packagemanager.util.JobUtil;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

public enum FilterOption {

    CREATED_BY_ME("createdByMe", (jcrPackage, userId, jobManager) -> FilterOption.isPackageCreator(jcrPackage, userId)),
    BUILT("built", (jcrPackage, userId, jobManager) -> JcrPackageStatusUtil.isBuilt(jcrPackage)),
    NOT_BUILT("notBuilt", (jcrPackage, userId, jobManager) -> !JcrPackageStatusUtil.isBuilt(jcrPackage)),
    INSTALLED("installed", (jcrPackage, userId, jobManager) -> JcrPackageStatusUtil.isInstalled(jcrPackage)),
    NOT_INSTALLED("notInstalled", (jcrPackage, userId, jobManager) -> !JcrPackageStatusUtil.isInstalled(jcrPackage)),
    MODIFIED("modified", (jcrPackage, userId, jobManager) -> JcrPackageStatusUtil.isModified(jcrPackage)),
    SCHEDULED("scheduled", (jcrPackage, userId, jobManager) -> FilterOption.containsAnySchedule(jcrPackage, jobManager));

    private static final Logger LOG = LoggerFactory.getLogger(FilterOption.class);

    private final String paramName;

    private final PackageFilter filter;

    FilterOption(String paramName, PackageFilter filter) {
        this.paramName = paramName;
        this.filter = filter;
    }

    private static boolean isPackageCreator(JcrPackage jcrPackage, String userId) {
        try {
            Node packageNode = jcrPackage.getNode();
            if (packageNode != null) {
                Property createdByVal = packageNode.getProperty(JcrConstants.JCR_CREATED_BY);
                String createdBy = createdByVal.getString();
                return StringUtils.equals(userId, createdBy);
            }
        } catch (RepositoryException e) {
            LOG.warn(String.format("Could not check if user with id: %s was a creator of package", userId), e);
        }
        return false;
    }

    private static boolean containsAnySchedule(JcrPackage jcrPackage, JobManager jobManager) {
        Node packageNode = jcrPackage.getNode();
        try {
            String path = packageNode != null ? packageNode.getPath() : null;
            return path != null && !JobUtil.findAllScheduledJobs(jobManager, JobProperties.asQueryMap(path)).isEmpty();
        } catch (RepositoryException e) {
            LOG.warn("Could not check if package contains any schedules", e);
        }
        return false;
    }

    @Override
    public String toString() {
        return paramName;
    }

    public boolean matches(JcrPackage jcrPackage, String userId, JobManager jobManager) {
        return filter.test(jcrPackage, userId, jobManager);
    }

    private interface PackageFilter {
        boolean test(JcrPackage jcrPackage, String userId, JobManager jobManager);
    }
}
