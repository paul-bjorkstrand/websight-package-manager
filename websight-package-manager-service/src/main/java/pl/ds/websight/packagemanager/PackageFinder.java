package pl.ds.websight.packagemanager;

import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.List;
import java.util.OptionalLong;

public class PackageFinder {

    private static final Logger LOG = LoggerFactory.getLogger(PackageFinder.class);

    public static final int PACKAGES_PER_PAGE = 25;

    private PackageFinder() {
        // no instance
    }

    public static long getNumberOfPages(long allPackagesAmount) {
        return allPackagesAmount % PACKAGES_PER_PAGE == 0 ?
                allPackagesAmount / PACKAGES_PER_PAGE :
                allPackagesAmount / PACKAGES_PER_PAGE + 1;
    }

    public static long getOffset(long pageNumber) {
        long validatedPageNr = pageNumber - 1 > 0 ? pageNumber - 1 : 0L;
        return validatedPageNr * PACKAGES_PER_PAGE;
    }

    public static OptionalLong findPageWithPackage(String path, List<JcrPackage> packages) {
        long packageIndex = -1;
        for (int i = 0; i < packages.size(); i++) {
            try {
                JcrPackage jcrPackage = packages.get(i);
                Node packageNode = jcrPackage.getNode();
                if (packageNode != null && path.equals(packageNode.getPath())) {
                    packageIndex = i;
                    break;
                }
            } catch (RepositoryException e) {
                LOG.warn("Could not get Node for JCR package", e);
            }
        }
        return packageIndex > -1
                ? OptionalLong.of(getNumberOfPages(packageIndex + 1))
                : OptionalLong.empty();
    }

}
