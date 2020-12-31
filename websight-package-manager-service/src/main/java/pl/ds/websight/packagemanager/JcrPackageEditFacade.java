package pl.ds.websight.packagemanager;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.packaging.Dependency;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.util.JcrConstants;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ds.websight.packagemanager.util.JcrPackageUtil;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class JcrPackageEditFacade {

    private static final Logger LOG = LoggerFactory.getLogger(JcrPackageEditFacade.class);
    private static final String THUMBNAIL_MIMETYPE = "image/png";

    private final JcrPackageDefinition packageDefinition;

    private JcrPackageEditFacade(JcrPackageDefinition packageDefinition) {
        this.packageDefinition = packageDefinition;
    }

    public static JcrPackageEditFacade forPackage(JcrPackage jcrPackage) {
        return JcrPackageUtil.fetchDefinition(jcrPackage)
                .map(JcrPackageEditFacade::new)
                .orElse(null);
    }

    public void setDescription(String newDescription) {
        String description = packageDefinition.getDescription();
        if (!StringUtils.equals(description, newDescription)) {
            packageDefinition.set(JcrConstants.JCR_DESCRIPTION, newDescription, false);
        }
        // we don't have to update last modification properties since description is in the same node as package vault definition
    }

    public void setFilters(List<PathFilterSet> newFilters) throws RepositoryException {
        MetaInf metaInfo = packageDefinition.getMetaInf();
        boolean isFilterNotReinitialized = true;
        WorkspaceFilter filter = metaInfo.getFilter();
        String downloadName = getPackageDownloadName();
        if (filter == null) {
            LOG.warn("Package {} doesn't contain its own Workspace filter, created new {}", downloadName,
                    DefaultWorkspaceFilter.class.getSimpleName());
            isFilterNotReinitialized = false;
            filter = new DefaultWorkspaceFilter();
            // setting new WorkspaceFilter won't guarantee, that WorkspaceFilter will be reinitialized correctly
        }
        List<PathFilterSet> packageFilters = filter.getFilterSets();
        if (isFilterNotReinitialized && arePathFilterSetsEqual(packageFilters, newFilters)) {
            return;
        }
        packageFilters.clear();
        packageFilters.addAll(newFilters);
        LOG.debug("Setting filters for package {}", downloadName);
        packageDefinition.setFilter(filter, false);
        LOG.debug("Updating last modification date of package {} due to setting filters", downloadName);
        updateLastModificationDate();
    }

    private boolean arePathFilterSetsEqual(List<PathFilterSet> list, List<PathFilterSet> compared) {
        if (list.size() != compared.size()) {
            return false;
        }
        for (int i = 0; i < list.size(); i++) {
            PathFilterSet pathFilterSet = list.get(i);
            PathFilterSet comparedPathFilterSet = compared.get(i);
            // TODO: Recheck after https://issues.apache.org/jira/browse/JCRVLT-454 is resolved
            if (!pathFilterSet.equals(comparedPathFilterSet) || !pathFilterSet.getImportMode().equals(comparedPathFilterSet.getImportMode())) {
                return false;
            }
        }
        return true;
    }

    public void deleteThumbnail() throws RepositoryException {
        Node packageNode = packageDefinition.getNode();
        if (!packageNode.hasNode(JcrPackageUtil.NN_PACKAGE_THUMBNAIL)) {
            LOG.debug("No thumbnail to remove for package definition: {}", getPackageDefinitionPath());
            return;
        }
        Session session = packageDefinition.getNode().getSession();
        String downloadName = getPackageDownloadName();
        LOG.debug("Deleting current thumbnail for package: {}", downloadName);
        session.removeItem(packageNode.getNode(JcrPackageUtil.NN_PACKAGE_THUMBNAIL).getPath());
        LOG.debug("Updating last modification date of package {} due to thumbnail removal", downloadName);
        updateLastModificationDate();
    }

    public void setThumbnail(InputStream newThumbnailStream, ResourceResolver resolver) throws RepositoryException, PersistenceException {
        if (newThumbnailStream == null) {
            LOG.warn("New thumbnail inputstream does not contain any data, skipping thumbnail update");
            return;
        }
        String packageDefPath = getPackageDefinitionPath();
        String downloadName = getPackageDownloadName();
        LOG.debug("Setting thumbnail for package {}", downloadName);
        Resource thumbnailFileResource = ResourceUtil.getOrCreateResource(resolver,
                packageDefPath + '/' + JcrPackageUtil.NN_PACKAGE_THUMBNAIL,
                Collections.singletonMap(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FILE), JcrConstants.NT_UNSTRUCTURED, false);
        Resource thumbnailContent = thumbnailFileResource.getChild(JcrConstants.JCR_CONTENT);
        if (thumbnailContent != null) {
            resolver.delete(thumbnailContent);
        }
        resolver.create(thumbnailFileResource, JcrConstants.JCR_CONTENT, getThumbnailContentProperties(newThumbnailStream));
        LOG.debug("Successfully updated thumbnail for package: {}", downloadName);
        LOG.debug("Updating last modification date of package {} due to thumbnail update", downloadName);
        updateLastModificationDate();
    }

    private String getPackageDefinitionPath() throws RepositoryException {
        return packageDefinition.getNode().getPath();
    }

    private String getPackageDownloadName() {
        return packageDefinition.getId().getDownloadName();
    }

    private static Map<String, Object> getThumbnailContentProperties(InputStream thumbnailStream) {
        Map<String, Object> contentProps = new HashMap<>();
        contentProps.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_RESOURCE);
        contentProps.put(JcrConstants.JCR_MIMETYPE, THUMBNAIL_MIMETYPE);
        contentProps.put(JcrConstants.JCR_DATA, thumbnailStream);
        return contentProps;
    }

    private void updateLastModificationDate() {
        packageDefinition.touch(null, false);
    }

    public void setDependencies(List<Dependency> newDependencies) {
        List<Dependency> oldDependencies = Arrays.asList(packageDefinition.getDependencies());
        if (oldDependencies.equals(newDependencies)) {
            return;
        }
        packageDefinition.setDependencies(newDependencies.toArray(new Dependency[]{}), false);
    }

    public void setAcHandling(AccessControlHandling newAcHandling) throws RepositoryException {
        AccessControlHandling oldAcHandling = packageDefinition.getAccessControlHandling();
        if (Objects.equals(oldAcHandling, newAcHandling)) {
            return;
        }
        if (newAcHandling == null) {
            removeAcHandlingProperty();
        } else {
            packageDefinition
                    .set(JcrPackageDefinition.PN_AC_HANDLING, newAcHandling.toString().toLowerCase(JcrPackageUtil.DEFAULT_LOCALE), false);
        }
    }

    private void removeAcHandlingProperty() throws RepositoryException {
        Node packageDefinitionNode = packageDefinition.getNode();
        Property acHandlingProp = packageDefinitionNode.getProperty(JcrPackageDefinition.PN_AC_HANDLING);
        if (acHandlingProp != null) {
            acHandlingProp.remove();
        }
    }

    public void setRequiresRestart(boolean newRequiredRestart) {
        boolean requiredRestart = packageDefinition.requiresRestart();
        if (requiredRestart == newRequiredRestart) {
            return;
        }
        packageDefinition.set(JcrPackageDefinition.PN_REQUIRES_RESTART, newRequiredRestart, false);
    }
}
