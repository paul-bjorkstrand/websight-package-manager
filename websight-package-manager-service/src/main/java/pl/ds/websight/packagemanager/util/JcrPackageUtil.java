package pl.ds.websight.packagemanager.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.fs.api.ImportMode;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.util.JcrConstants;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;

import static java.util.Locale.ENGLISH;
import static org.apache.jackrabbit.vault.fs.api.ImportMode.REPLACE;
import static org.apache.jackrabbit.vault.packaging.JcrPackage.NN_VLT_DEFINITION;
import static org.apache.jackrabbit.vault.packaging.JcrPackageDefinition.NN_FILTER;
import static org.apache.jackrabbit.vault.packaging.JcrPackageDefinition.PN_MODE;
import static org.apache.jackrabbit.vault.packaging.JcrPackageDefinition.PN_ROOT;
import static org.apache.jackrabbit.vault.util.JcrConstants.JCR_CONTENT;

public final class JcrPackageUtil {

    private static final Logger LOG = LoggerFactory.getLogger(JcrPackageUtil.class);
    private static final String PACKAGE_VLT_REL_PATH = '/' + JCR_CONTENT + '/' + NN_VLT_DEFINITION;

    public static final Locale DEFAULT_LOCALE = ENGLISH;
    public static final String NN_PACKAGE_THUMBNAIL = "thumbnail.png";
    public static final String THUMBNAIL_REL_PATH = PACKAGE_VLT_REL_PATH + '/' + NN_PACKAGE_THUMBNAIL;
    public static final String PACKAGE_FILTERS_REL_PATH = PACKAGE_VLT_REL_PATH + '/' + NN_FILTER;
    public static final String PACKAGES_ROOT_PATH = "/etc/packages/"; //NOSONAR

    public static final String NO_GROUP = ":no_group";

    private JcrPackageUtil() {
        // no instances
    }

    public static boolean hasValidFilters(Session session, String jcrPackagePath) {
        String filtersPath = jcrPackagePath + PACKAGE_FILTERS_REL_PATH;
        try {
            if (!session.nodeExists(filtersPath)) {
                return false;
            }
            NodeIterator filterNodes = session.getNode(filtersPath).getNodes();
            if (!filterNodes.hasNext()) {
                return false;
            }
            while (filterNodes.hasNext()) {
                Node filterNode = filterNodes.nextNode();
                if (!(filterNode.hasProperty(PN_ROOT) && filterNode.hasProperty(PN_MODE))) {
                    return false;
                }
            }
            return true;
        } catch (RepositoryException e) {
            LOG.warn("Could not check package filters", e);
        }
        return false;
    }

    public static boolean hasValidWorkspaceFilter(JcrPackage jcrPackage) {
        return JcrPackageUtil.fetchDefinition(jcrPackage)
                .map(definition -> {
                    try {
                        return definition.getMetaInf();
                    } catch (RepositoryException e) {
                        return null;
                    }
                })
                .map(metaInf -> metaInf.getFilter() != null)
                .orElse(false);
    }

    public static boolean isValidPackageNode(Node packageNode) {
        try {
            if (packageNode.isNodeType(JcrConstants.NT_HIERARCHYNODE) && packageNode.hasNode(JcrConstants.JCR_CONTENT)) {
                return packageNode.getNode(JcrConstants.JCR_CONTENT).isNodeType(JcrPackage.NT_VLT_PACKAGE);
            }
        } catch (RepositoryException e) {
            LOG.warn("Error during node validation", e);
        }
        return false;
    }

    public static ImportMode toImportMode(String importModeName) {
        return getEnum(importModeName, ImportMode.class, REPLACE);
    }

    public static AccessControlHandling toAcHandling(String acHandlingName) {
        String formattedAcHandlingName = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(acHandlingName), "_");
        formattedAcHandlingName = StringUtils.lowerCase(formattedAcHandlingName, DEFAULT_LOCALE);
        return getEnum(formattedAcHandlingName, AccessControlHandling.class, null);
    }

    private static <E extends Enum<E>> E getEnum(String enumName, Class<E> enumClass, E defaultValue) {
        return EnumSet.allOf(enumClass)
                .stream()
                .filter(enumVal -> enumVal.name().equalsIgnoreCase(enumName))
                .findFirst()
                .orElseGet(() -> {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Could not recognize value: {} in Enum class: '{}'", enumName, enumClass.getSimpleName());
                    }
                    return defaultValue;
                });
    }

    public static String getSimplePackageName(String name, String version) {
        return name +
                (!version.isEmpty() ? '-' + version : "") +
                ".zip";
    }

    public static String getSimplePackageName(JcrPackage jcrPackage) {
        return fetchDefinition(jcrPackage)
                .map(JcrPackageDefinition::getId)
                .map(PackageId::getDownloadName)
                .orElse(null);
    }

    @NotNull
    public static String getGroupIdFromNode(Node packageRoot, Node groupNode) throws RepositoryException {
        return groupNode.getPath().substring(packageRoot.getPath().length() + 1);
    }

    public static Optional<JcrPackageDefinition> fetchDefinition(JcrPackage jcrPackage) {
        try {
            return Optional.ofNullable(jcrPackage.getDefinition());
        } catch (RepositoryException e) {
            LOG.warn("Cannot get package definition", e);
        }
        return Optional.empty();
    }

    public static void close(JcrPackage jcrPackage) {
        if (jcrPackage != null) {
            jcrPackage.close();
        }
    }

    public static JcrPackage open(String packagePath, Session session, JcrPackageManager packageManager)
            throws RepositoryException, OpenPackageException {
        Node packageNode = session.getNode(packagePath);
        final JcrPackage openedPackage = packageManager.open(packageNode);
        if (openedPackage == null) { // NOSONAR
            throw new OpenPackageException(packagePath);
        }
        return openedPackage;
    }

    public static long countPackages(Node root, long limit, boolean deep) throws RepositoryException {
        long packages = 0;
        NodeIterator nodeIterator = root.getNodes();
        while (nodeIterator.hasNext() && packages <= limit) {
            Node child = nodeIterator.nextNode();
            if (".snapshot".equals(child.getName())) {
                continue;
            }
            if (JcrPackageUtil.isValidPackageNode(child)) {
                packages++;
            } else if (deep && child.hasNodes()) {
                packages += countPackages(child, limit - packages, true);
            }
        }
        return packages;
    }

}
