package pl.ds.websight.packagemanager.dto;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.packaging.Dependency;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.util.JcrConstants;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.ScheduledJobInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ds.websight.packagemanager.util.JcrPackageUtil;
import pl.ds.websight.packagemanager.util.JobUtil;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class PackageDto {

    private static final Logger LOG = LoggerFactory.getLogger(PackageDto.class);

    private String path;
    private String parentPath;
    private String description;
    private PackageActionInfoDto modification;
    private PackageActionInfoDto unwrap;
    private PackageActionInfoDto wrap;
    private long buildCount;
    private long size;
    private PackageId packageInfo;
    private final boolean isJcrPackageValid;
    private boolean requiresRestart;
    private long timestamp;
    private PackageThumbnailDto thumbnail;
    private PackageStatusDto status;
    private List<PackageFilterDto> filters;
    private PackageActionDto lastAction;
    private String acHandling;
    private List<PackageDependencyDto> dependencies;
    private PackageScheduleActionInfoDto nextScheduledAction;

    private PackageDto() {
        this.isJcrPackageValid = false;
    }

    private PackageDto(JcrPackage jcrPackage) throws RepositoryException {
        JcrPackageDefinition packageDefinition = jcrPackage.getDefinition();
        this.packageInfo = packageDefinition != null ? packageDefinition.getId() : null;
        this.description = packageDefinition != null ? packageDefinition.getDescription() : null;
        this.buildCount = packageDefinition != null ? packageDefinition.getBuildCount() : 0L;
        this.modification = PackageActionInfoDto.create(packageDefinition, JcrConstants.JCR_LASTMODIFIED);
        this.unwrap = PackageActionInfoDto.create(packageDefinition, JcrPackageDefinition.PN_LAST_UNWRAPPED);
        this.wrap = PackageActionInfoDto.create(packageDefinition, JcrPackageDefinition.PN_LAST_WRAPPED);
        this.isJcrPackageValid = jcrPackage.isValid();
        this.size = jcrPackage.getSize();
        this.requiresRestart = packageDefinition != null && packageDefinition.requiresRestart();
        this.status = PackageStatusDto.fetchPackageStatus(jcrPackage);
        this.path = "";
        this.acHandling = packageDefinition != null ? parseAcHandling(packageDefinition.getAccessControlHandling()) : null;
        this.dependencies = parsePackageDependencies(jcrPackage);
    }

    private static String parseAcHandling(AccessControlHandling acHandling) {
        if (acHandling == null) {
            return null;
        }
        String formattedAcHandling = StringUtils.replace(acHandling.toString(), "_", " ");
        formattedAcHandling = formattedAcHandling.toLowerCase(JcrPackageUtil.DEFAULT_LOCALE);
        formattedAcHandling = StringUtils.capitalize(formattedAcHandling);
        return formattedAcHandling;
    }

    private static List<PackageDependencyDto> parsePackageDependencies(JcrPackage jcrPackage) throws RepositoryException {
        JcrPackageDefinition packageDefinition = jcrPackage.getDefinition();
        if (packageDefinition == null) {
            return Collections.emptyList();
        }
        Dependency[] allDependencies = packageDefinition.getDependencies();
        Dependency[] unresolvedDependencies;
        try {
            unresolvedDependencies = jcrPackage.getUnresolvedDependencies();
        } catch (RepositoryException e) {
            LOG.warn("Could not get unresolved dependencies from package", e);
            unresolvedDependencies = allDependencies;
        }
        Dependency[] packageUnresolvedDependencies = unresolvedDependencies;
        return allDependencies.length != 0 ?
                Arrays.stream(allDependencies)
                        .map(dependency -> new PackageDependencyDto(dependency.toString(),
                                !ArrayUtils.contains(packageUnresolvedDependencies, dependency)))
                        .collect(toList()) :
                Collections.emptyList();
    }

    public String getPath() {
        return path;
    }

    public String getParentPath() {
        return parentPath;
    }

    public String getDescription() {
        return description;
    }

    public PackageActionInfoDto getModification() {
        return modification;
    }

    public PackageActionInfoDto getUnwrap() {
        return unwrap;
    }

    public PackageActionInfoDto getWrap() {
        return wrap;
    }

    public long getBuildCount() {
        return buildCount;
    }

    public long getSize() {
        return size;
    }

    public PackageId getPackageInfo() {
        return packageInfo;
    }

    public boolean isJcrPackageValid() {
        return isJcrPackageValid;
    }

    public boolean isRequiresRestart() {
        return requiresRestart;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public PackageThumbnailDto getThumbnail() {
        return thumbnail;
    }

    public PackageStatusDto getStatus() {
        return status;
    }

    public List<PackageFilterDto> getFilters() {
        return filters;
    }

    public PackageActionDto getLastAction() {
        return lastAction;
    }

    public String getAcHandling() {
        return acHandling;
    }

    public List<PackageDependencyDto> getDependencies() {
        return dependencies;
    }

    public PackageScheduleActionInfoDto getNextScheduledAction() {
        return nextScheduledAction;
    }

    public void setLastAction(PackageActionDto lastAction) {
        this.lastAction = lastAction;
    }

    public void setNextScheduledAction(PackageScheduleActionInfoDto nextScheduledAction) {
        this.nextScheduledAction = nextScheduledAction;
    }

    public static PackageDto wrap(JcrPackage jcrPackage, Session session, JobManager jobManager) {
        PackageDto packageDto = initPackageDto(jcrPackage, session);
        if (packageDto == null) {
            LOG.debug("Passed invalid package to output, creating placeholder");
            return createInvalidPackage(jcrPackage.getNode());
        }
        packageDto.lastAction = PackageActionDto.forPackagePath(jobManager, session, packageDto.path);
        packageDto.nextScheduledAction = fetchNearestScheduledAction(jobManager, packageDto.path);
        return packageDto;
    }

    public static PackageDto wrapWithoutJobsData(JcrPackage jcrPackage, Session session) {
        PackageDto packageDto = initPackageDto(jcrPackage, session);
        if (packageDto == null) {
            LOG.debug("Passed invalid package to output, creating placeholder");
            return createInvalidPackage(jcrPackage.getNode());
        }
        return packageDto;
    }

    private static PackageDto initPackageDto(JcrPackage jcrPackage, Session session) {
        try {
            PackageDto packageDto = new PackageDto(jcrPackage);
            Node packageNode = jcrPackage.getNode();
            if (packageNode != null) {
                packageDto.path = packageNode.getPath();
                packageDto.parentPath = packageNode.getParent().getPath();
                if (JcrPackageUtil.hasValidFilters(session, packageDto.path)) {
                    packageDto.filters = fetchPackageFilters(packageDto.path, session);
                }
                packageDto.timestamp = fetchPackageModificationTimestamp(jcrPackage);
                packageDto.thumbnail = PackageThumbnailDto.forPackagePath(packageDto.path, session);
                return packageDto;
            }
        } catch (RepositoryException e) {
            LOG.warn("Could not assign particular properties from JCR Package to Package DTO", e);
        }
        return null;
    }

    private static List<PackageFilterDto> fetchPackageFilters(String packageNodePath, Session session) throws RepositoryException {
        String filterRootPath = packageNodePath + JcrPackageUtil.PACKAGE_FILTERS_REL_PATH;
        NodeIterator filterNodes = session.getNode(filterRootPath).getNodes();
        List<PackageFilterDto> packageFilters = new ArrayList<>();
        while (filterNodes.hasNext()) {
            PackageFilterDto packageFilter = PackageFilterDto.create(filterNodes.nextNode());
            if (packageFilter != null) {
                packageFilters.add(packageFilter);
            }
        }
        return packageFilters;
    }

    private static PackageScheduleActionInfoDto fetchNearestScheduledAction(JobManager jobManager, String packagePath) {
        ScheduledJobInfo nearestScheduledJobInfo = JobUtil.getNearestScheduledJob(jobManager, packagePath);
        if (nearestScheduledJobInfo != null) {
            return PackageScheduleActionInfoDto.asBasicInfo(nearestScheduledJobInfo);
        }
        return null;
    }

    private static long fetchPackageModificationTimestamp(JcrPackage jcrPackage) throws RepositoryException {
        long packageModificationTimestamp = JcrPackageUtil.fetchDefinition(jcrPackage)
                .map(JcrPackageDefinition::getLastModified)
                .map(Calendar::getTimeInMillis)
                .orElse(0L);
        if (packageModificationTimestamp != 0L) {
            return packageModificationTimestamp;
        }
        Node packageNode = jcrPackage.getNode();
        if (packageNode == null) {
            return packageModificationTimestamp;
        }
        if (packageNode.hasNode(JcrConstants.JCR_CONTENT)) {
            Node packageContentNode = packageNode.getNode(JcrConstants.JCR_CONTENT);
            if (packageContentNode.hasProperty(JcrConstants.JCR_LASTMODIFIED)) {
                LOG.warn("Could not get last modification date of package definition for: {}, using package content modification date",
                        packageNode.getPath());
                return packageContentNode.getProperty(JcrConstants.JCR_LASTMODIFIED).getDate().getTimeInMillis();
            }
        }
        LOG.warn("Could not get last modification date of package definition for: {}, using package upload date", packageNode.getPath());
        return packageNode.hasProperty(JcrConstants.JCR_CREATED) ?
                packageNode.getProperty(JcrConstants.JCR_CREATED).getDate().getTimeInMillis() :
                packageModificationTimestamp;
    }

    private static PackageDto createInvalidPackage(Node invalidPackageNode) {
        PackageDto invalidPackage = new PackageDto();
        try {
            invalidPackage.path = invalidPackageNode != null ? invalidPackageNode.getPath() : "Unresolvable path";
        } catch (RepositoryException e) {
            invalidPackage.path = "Unresolvable path";
        }
        return invalidPackage;
    }
}
