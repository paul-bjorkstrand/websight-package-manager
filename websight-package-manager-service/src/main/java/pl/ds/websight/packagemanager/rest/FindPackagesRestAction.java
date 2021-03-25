package pl.ds.websight.packagemanager.rest;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.ScheduledJobInfo;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ds.websight.packagemanager.PackageFinder;
import pl.ds.websight.packagemanager.dto.PackageActionDto;
import pl.ds.websight.packagemanager.dto.PackageDto;
import pl.ds.websight.packagemanager.dto.PackageListDto;
import pl.ds.websight.packagemanager.dto.PackageScheduleActionInfoDto;
import pl.ds.websight.packagemanager.rest.requestparameters.FilterOption;
import pl.ds.websight.packagemanager.util.JcrPackageUtil;
import pl.ds.websight.packagemanager.util.JobUtil;
import pl.ds.websight.rest.framework.RestAction;
import pl.ds.websight.rest.framework.RestActionResult;
import pl.ds.websight.rest.framework.annotations.SlingAction;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;

import static java.util.stream.Collectors.toList;
import static pl.ds.websight.packagemanager.PackageFinder.PACKAGES_PER_PAGE;
import static pl.ds.websight.rest.framework.annotations.SlingAction.HttpMethod.GET;

@Component
@Designate(ocd = FindPackagesRestAction.Config.class)
@SlingAction(GET)
public class FindPackagesRestAction extends AbstractRestAction<FindPackagesRestModel, PackageListDto>
        implements RestAction<FindPackagesRestModel, PackageListDto> {

    private static final Logger LOG = LoggerFactory.getLogger(FindPackagesRestAction.class);

    private static final int LIMITED_NEXT_PAGES = 3;

    @Reference
    private Packaging packaging;

    @Reference
    private JobManager jobManager;

    private Config config;

    @Override
    protected RestActionResult<PackageListDto> performAction(FindPackagesRestModel model) throws RepositoryException {
        return RestActionResult.success(findPackages(model));
    }

    private PackageListDto findPackages(FindPackagesRestModel model) throws RepositoryException {
        Session session = model.getSession();
        if (session == null || !session.nodeExists(JcrPackageUtil.PACKAGES_ROOT_PATH)) {
            LOG.debug("User is not allowed to read packages");
            return PackageListDto.EMPTY;
        }
        List<JcrPackage> fetchedPackages = new LinkedList<>();
        try {
            JcrPackageManager packageManager = packaging.getPackageManager(session);
            String group = getRequestedOrPackageGroupIfBothRequested(model, packageManager);
            boolean groupRequested = !JcrPackageUtil.NO_GROUP.equals(group);
            String packagesSearchPath = getPackagesSearchPath(group, groupRequested);
            boolean limitExceeded = false;
            if (session.nodeExists(packagesSearchPath)) {
                Node searchRootNode = session.getNode(packagesSearchPath);
                long allPackagesCount = JcrPackageUtil.countPackages(searchRootNode, config.count_limit(), groupRequested);
                if (allPackagesCount > config.count_limit()) {
                    limitExceeded = true;
                    long limit = PackageFinder.getOffset(model.getPageNumber()) + PACKAGES_PER_PAGE * LIMITED_NEXT_PAGES;
                    fetchedPackages =
                            fetchPackagesLimited(new ArrayList<>(), model, packageManager, searchRootNode, limit, groupRequested,
                                    jobManager);
                } else {
                    fetchedPackages = fetchPackages(searchRootNode, model.getPackageNameFilter(), groupRequested).stream()
                            .sorted(model.getSortBy().getComparator())
                            .map(definition -> openPackage(packageManager, definition.getPackageNode()))
                            .filter(Objects::nonNull)
                            .collect(toList());
                }
            }
            List<JcrPackage> foundPackages = filterPackagesIfNeeded(fetchedPackages, model, limitExceeded, session);
            long foundPackagesCount = foundPackages.size();
            long numberOfPages = PackageFinder.getNumberOfPages(foundPackagesCount);
            long pageNumber = getPageNumber(model, foundPackages);
            long offset = PackageFinder.getOffset(pageNumber);
            List<PackageDto> packages = getPackagesForRequestedPage(foundPackages, session, offset);
            return new PackageListDto(foundPackagesCount, limitExceeded, numberOfPages, pageNumber, config.count_limit(), group, packages);
        } finally {
            fetchedPackages.forEach(JcrPackageUtil::close);
        }
    }

    private static String getRequestedOrPackageGroupIfBothRequested(FindPackagesRestModel model, JcrPackageManager packageManager) {
        String requestedPackagePath = model.getPath();
        String requestedGroup = model.getGroup();
        if (StringUtils.isNotBlank(requestedPackagePath) && StringUtils.isNotBlank(requestedGroup)) {
            try {
                Node requestedPackageNode = model.getSession().getNode(requestedPackagePath);
                if (requestedPackageNode != null) {
                    Node requestedPackageGroupNode = requestedPackageNode.getParent();
                    String groupIdFromNode = JcrPackageUtil.getGroupIdFromNode(packageManager.getPackageRoot(), requestedPackageGroupNode);
                    return StringUtils.isNotBlank(groupIdFromNode) ? groupIdFromNode : JcrPackageUtil.NO_GROUP;
                }
            } catch (RepositoryException e) {
                LOG.warn("Could not open JCR package for path {}", requestedPackagePath, e);
            }
        }
        return requestedGroup;
    }

    private static String getPackagesSearchPath(String group, boolean hasGroup) {
        if (hasGroup) {
            return JcrPackageUtil.PACKAGES_ROOT_PATH + StringUtils.defaultString(group, "");
        }
        return JcrPackageUtil.PACKAGES_ROOT_PATH;
    }

    private static List<JcrPackage> fetchPackagesLimited(List<String> paths, FindPackagesRestModel model,
            JcrPackageManager packageManager, Node root, long limit, boolean deep, JobManager jobManager) throws RepositoryException {
        List<JcrPackage> packages = new ArrayList<>();
        NodeIterator nodeIterator = root.getNodes();
        while (nodeIterator.hasNext() && isBelowLimitOrShouldSearch(paths, model.getPath(), limit)) {
            Node child = nodeIterator.nextNode();
            String name = child.getName();
            if (".snapshot".equals(name)) {
                continue;
            }
            if (JcrPackageUtil.isValidPackageNode(child) && StringUtils.containsIgnoreCase(name, model.getPackageNameFilter())) {
                JcrPackage jcrPackage = openPackage(packageManager, child);
                if (jcrPackage != null && matchesAllFilters(jcrPackage, model.getFilterOptions(), model.getUserID(), jobManager)) {
                    packages.add(jcrPackage);
                    paths.add(child.getPath());
                }
            } else if (deep && child.hasNodes()) {
                packages.addAll(fetchPackagesLimited(paths, model, packageManager, child, limit, true, jobManager));
            }
        }
        return packages;
    }

    private static boolean isBelowLimitOrShouldSearch(List<String> paths, String searchedPackagePath, long limit) {
        if (StringUtils.isBlank(searchedPackagePath)) {
            return paths.size() < limit;
        }
        int indexOfSearchedPackage = paths.indexOf(searchedPackagePath);
        return indexOfSearchedPackage == -1 || paths.size() - indexOfSearchedPackage < PACKAGES_PER_PAGE * LIMITED_NEXT_PAGES;
    }

    private static List<PackageDefinition> fetchPackages(Node root, String filterPhrase, boolean deep) throws RepositoryException {
        List<PackageDefinition> packages = new ArrayList<>();
        NodeIterator nodeIterator = root.getNodes();
        while (nodeIterator.hasNext()) {
            Node child = nodeIterator.nextNode();
            String name = child.getName();
            if (".snapshot".equals(name)) {
                continue;
            }
            if (JcrPackageUtil.isValidPackageNode(child) && StringUtils.containsIgnoreCase(name, filterPhrase)) {
                packages.add(new PackageDefinition(child));
            } else if (deep && child.hasNodes()) {
                packages.addAll(fetchPackages(child, filterPhrase, true));
            }
        }
        return packages;
    }

    private static JcrPackage openPackage(JcrPackageManager packageManager, Node packageNode) {
        try {
            return packageManager.open(packageNode);
        } catch (RepositoryException e) {
            LOG.warn("Error while opening package", e);
        }
        return null;
    }

    private List<JcrPackage> filterPackagesIfNeeded(List<JcrPackage> packages, FindPackagesRestModel model, boolean limitExceeded,
            Session session) {
        List<FilterOption> filterOptions = model.getFilterOptions();
        if (limitExceeded || filterOptions.isEmpty()) {
            return packages;
        }
        String userId = session.getUserID();
        return packages.stream()
                .filter(jcrPackage -> matchesAllFilters(jcrPackage, filterOptions, userId, jobManager))
                .collect(toList());
    }

    private static boolean matchesAllFilters(JcrPackage jcrPackage, List<FilterOption> filterOptions, String userID,
            JobManager jobManager) {
        return filterOptions == null ||
                filterOptions.stream().allMatch(filterOption -> filterOption.matches(jcrPackage, userID, jobManager));
    }

    private static long getPageNumber(FindPackagesRestModel model, List<JcrPackage> filteredPackages) {
        long pageNumber = model.getPageNumber();
        String packagePath = model.getPath();
        if (StringUtils.isNotBlank(packagePath)) {
            OptionalLong pageWithPackage = PackageFinder.findPageWithPackage(packagePath, filteredPackages);
            pageNumber = pageWithPackage.orElse(pageNumber);
        }
        return pageNumber;
    }

    private List<PackageDto> getPackagesForRequestedPage(List<JcrPackage> packages, Session session, long offset) {
        List<PackageDto> packagesDtos = packages.stream()
                .skip(offset)
                .limit(PACKAGES_PER_PAGE)
                .map(jcrPackage -> PackageDto.wrapWithoutJobsData(jcrPackage, session))
                .collect(toList());
        String[] packagesPaths = packagesDtos.stream().map(PackageDto::getPath).toArray(String[]::new);
        Map<String, ScheduledJobInfo> nextExecutionDateByPath = JobUtil.getNearestScheduledJobs(jobManager, packagesPaths);
        Map<String, PackageActionDto> packagesActionsByPath = PackageActionDto.forPackagePaths(jobManager, session, packagesPaths);
        for (PackageDto packageDto : packagesDtos) {
            String packagePath = packageDto.getPath();
            ScheduledJobInfo nextSchedule = nextExecutionDateByPath.get(packagePath);
            if (nextSchedule != null) {
                packageDto.setNextScheduledAction(PackageScheduleActionInfoDto.asBasicInfo(nextSchedule));
            }
            packageDto.setLastAction(packagesActionsByPath.getOrDefault(packagePath, PackageActionDto.UNKNOWN));
        }
        return packagesDtos;
    }

    @Override
    protected String getUnexpectedErrorMessage() {
        return Messages.FIND_PACKAGES_ERROR;
    }

    @Activate
    private void activate(FindPackagesRestAction.Config config) {
        this.config = config;
    }

    @ObjectClassDefinition(name = "WebSight Package Manager: Find Packages Rest Action Configuration")
    public @interface Config {

        @AttributeDefinition(
                name = "Package count limit",
                description = "Maximum number of packages traversed by 'find-packages' action.",
                type = AttributeType.INTEGER
        )
        int count_limit() default 10000; // NOSONAR

    }

}
