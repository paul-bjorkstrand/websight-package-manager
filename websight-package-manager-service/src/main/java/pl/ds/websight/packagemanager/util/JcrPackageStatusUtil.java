package pl.ds.websight.packagemanager.util;

import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.Calendar;

public final class JcrPackageStatusUtil {

    private static final Logger LOG = LoggerFactory.getLogger(JcrPackageStatusUtil.class);

    private JcrPackageStatusUtil() {
        // no instance
    }

    public static boolean isInstalled(JcrPackage jcrPackage) {
        return isBuilt(jcrPackage) &&
                (hasSnapshot(jcrPackage) ||
                        JcrPackageUtil.fetchDefinition(jcrPackage)
                                .map(packageDefinition -> isNewer(packageDefinition.getLastUnpacked(),
                                        packageDefinition.getLastUnwrapped(), 1))
                                .orElse(false));
    }

    private static boolean hasSnapshot(JcrPackage jcrPackage) {
        try {
            return jcrPackage.getSnapshot() != null;
        } catch (RepositoryException e) {
            LOG.warn("Could not check whether the package has local snapshot", e);
        }
        return false;
    }

    public static boolean isModified(JcrPackage jcrPackage) {
        return isBuilt(jcrPackage) &&
                JcrPackageUtil.fetchDefinition(jcrPackage)
                        .map(packageDefinition -> wasModifiedAfterCreation(packageDefinition) && notUnwrappedAfterModification(packageDefinition))
                        .orElse(false);
    }

    public static boolean isBuilt(JcrPackage jcrPackage) {
        return jcrPackage.getSize() > 0 || hasBuilds(jcrPackage);
    }

    private static boolean hasBuilds(JcrPackage jcrPackage) {
        return JcrPackageUtil.fetchDefinition(jcrPackage)
                .map(jcrPackageDefinition -> jcrPackageDefinition.getBuildCount() > 0)
                .orElse(false);
    }

    private static boolean wasModifiedAfterCreation(JcrPackageDefinition jcrPackageDefinition) {
        return isNewer(jcrPackageDefinition.getLastModified(), jcrPackageDefinition.getCreated(), 1000);
    }

    private static boolean notUnwrappedAfterModification(JcrPackageDefinition jcrPackageDefinition) {
        return !isNewer(jcrPackageDefinition.getLastUnwrapped(), jcrPackageDefinition.getLastModified(), 1);
    }

    private static boolean isNewer(Calendar firstDate, Calendar secondDate, long tolerance) {
        long secondDateInMillis = secondDate != null ? secondDate.getTimeInMillis() : 0L;
        return firstDate != null && (firstDate.getTimeInMillis() - secondDateInMillis) >= tolerance;
    }

    public static boolean hasUnresolvedDependencies(JcrPackage jcrPackage) {
        try {
            return jcrPackage.getUnresolvedDependencies().length == 0;
        } catch (RepositoryException e) {
            LOG.warn("Could not check unresolved dependencies", e);
            return false;
        }
    }

}
