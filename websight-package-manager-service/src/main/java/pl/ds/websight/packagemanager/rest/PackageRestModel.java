package pl.ds.websight.packagemanager.rest;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.fs.api.ImportMode;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.filter.DefaultPathFilter;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.packaging.Dependency;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ds.websight.packagemanager.util.JcrPackageUtil;
import pl.ds.websight.request.parameters.support.annotations.RequestParameter;
import pl.ds.websight.rest.framework.Errors;
import pl.ds.websight.rest.framework.Validatable;

import javax.annotation.PostConstruct;
import javax.jcr.Session;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.jackrabbit.vault.fs.api.ImportMode.REPLACE;
import static pl.ds.websight.packagemanager.util.JcrPackageUtil.PACKAGES_ROOT_PATH;

@Model(adaptables = SlingHttpServletRequest.class)
public class PackageRestModel implements Validatable {

    private static final Logger LOG = LoggerFactory.getLogger(PackageRestModel.class);
    private static final String THUMBNAIL_PARAM_NAME = "thumbnail";
    private static final long THUMBNAIL_SIZE_LIMIT_IN_KB = 400;
    private static final long THUMBNAIL_SIZE_LIMIT_IN_BYTES = THUMBNAIL_SIZE_LIMIT_IN_KB * 1024;
    private static final List<String> SUPPORTED_PACKAGE_EXTENSIONS = asList(".zip", ".jar");
    private static final List<String> ALLOWED_THUMBNAIL_MIME_TYPES =
            asList("image/bmp", "image/gif", "image/jpeg", "image/jpg", "image/png");
    private static final CollectionType LIST_STRING_COLLECTION_TYPE = TypeFactory.defaultInstance()
            .constructCollectionType(List.class, String.class);
    private static final CollectionType LIST_FILTER_COLLECTION_TYPE = TypeFactory.defaultInstance()
            .constructCollectionType(List.class, FilterInput.class);

    private static final ObjectReader LIST_READER = new ObjectMapper().readerFor(LIST_STRING_COLLECTION_TYPE);
    private static final ObjectReader FILTERS_INPUT_READER = new ObjectMapper().readerFor(LIST_FILTER_COLLECTION_TYPE);

    @Self
    private SlingHttpServletRequest request;

    @RequestParameter
    @NotBlank(message = "Package name cannot be blank")
    private String name;

    @RequestParameter
    @NotNull(message = "Package group cannot be null")
    private String group;

    @RequestParameter
    private String description;

    @RequestParameter
    @Default(values = "")
    private String version;

    @RequestParameter(name = "filters")
    private String filtersJson;

    @RequestParameter(name = "acHandling")
    private String acHandlingStr;

    @RequestParameter(name = "dependencies")
    private String dependenciesJson;

    @RequestParameter
    @Default(booleanValues = false)
    private Boolean requiresRestart;

    private List<PathFilterSet> filters;

    private List<Dependency> dependencies;

    private org.apache.sling.api.request.RequestParameter thumbnail;

    private AccessControlHandling acHandling;

    @PostConstruct
    protected void init() {
        filters = StringUtils.isBlank(filtersJson) ?
                Collections.emptyList() :
                readFiltersInput(filtersJson)
                        .stream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .map(PackageRestModel::mapToPathFilterSet)
                        .collect(toList());
        dependencies = StringUtils.isBlank(dependenciesJson) ?
                Collections.emptyList() :
                readJsonList(dependenciesJson)
                        .stream()
                        .distinct()
                        .filter(StringUtils::isNotBlank)
                        .map(Dependency::fromString)
                        .filter(Objects::nonNull)
                        .collect(toList());
        acHandling = JcrPackageUtil.toAcHandling(acHandlingStr);
        thumbnail = request.getRequestParameter(THUMBNAIL_PARAM_NAME);
    }

    private static List<String> readJsonList(String json) {
        try {
            return LIST_READER.readValue(json);
        } catch (IOException e) {
            LOG.warn("Could not read filter parameter", e);
        }
        return Collections.emptyList();
    }

    private static List<FilterInput> readFiltersInput(String filtersInputJson) {
        try {
            return FILTERS_INPUT_READER.readValue(filtersInputJson);
        } catch (IOException e) {
            LOG.warn("Could not read filters input", e);
        }
        return Collections.emptyList();
    }

    public String getName() {
        return name;
    }

    public String getGroup() {
        return group;
    }

    public String getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }

    public SlingHttpServletRequest getRequest() {
        return request;
    }

    public Session getSession() {
        return request.getResourceResolver().adaptTo(Session.class);
    }

    public ResourceResolver getResourceResolver() {
        return request.getResourceResolver();
    }

    public List<PathFilterSet> getFilters() {
        return filters;
    }

    public AccessControlHandling getAcHandling() {
        return acHandling;
    }

    public boolean requiresRestart() {
        return Boolean.TRUE.equals(requiresRestart);
    }

    public org.apache.sling.api.request.RequestParameter getThumbnail() {
        return thumbnail;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    private static PathFilterSet mapToPathFilterSet(FilterInput filterInput) {
        PathFilterSet pathFilterSet = new PathFilterSet(filterInput.root);
        pathFilterSet.setImportMode(filterInput.mode);

        if (filterInput.rules == null || filterInput.rules.isEmpty()) {
            return pathFilterSet;
        }
        filterInput.rules.stream()
                .filter(ruleInput -> StringUtils.isNotBlank(ruleInput.path))
                .forEach(ruleInput -> {
                    if (ruleInput.include) {
                        pathFilterSet.addInclude(new DefaultPathFilter(ruleInput.path));
                    } else {
                        pathFilterSet.addExclude(new DefaultPathFilter(ruleInput.path));
                    }
                });
        return pathFilterSet;
    }

    @Override
    public Errors validate() {
        Errors errors = Errors.createErrors();
        if (!group.isEmpty() && StringUtils.isBlank(group)) {
            errors.add("group", group, "Package group cannot contain only whitespaces");
        }
        if (!PackageId.isValid(group, name, version)) {
            errors.add("name", name, Messages.PACKAGE_ID_VALIDATION_ERROR_INVALID_COMBINATION);
            errors.add("group", group, Messages.PACKAGE_ID_VALIDATION_ERROR_INVALID_COMBINATION);
            errors.add("version", version, Messages.PACKAGE_ID_VALIDATION_ERROR_INVALID_COMBINATION);
        }
        validatePackagePath(errors);
        validateThumbnail(thumbnail, errors);
        return errors;
    }

    private void validatePackagePath(Errors errors) {
        ResourceResolver resourceResolver = this.getResourceResolver();
        String packagePath = PACKAGES_ROOT_PATH + (group.isEmpty() ? "" : group + '/') + name;
        String groupPath = PACKAGES_ROOT_PATH + group;
        if (!group.isEmpty()) {
            for (String extension : SUPPORTED_PACKAGE_EXTENSIONS) {
                if (resourceResolver.getResource(groupPath + extension) != null) {
                    errors.add("group", group, String.format(Messages.PACKAGE_NAME_VALIDATION_ERROR_PATH_ALREADY_EXISTS, groupPath + extension));
                }
            }
        }
        if (resourceResolver.getResource(packagePath) != null) {
            errors.add("name", name, String.format(Messages.PACKAGE_NAME_VALIDATION_ERROR_PATH_ALREADY_EXISTS, packagePath));
        }
    }

    private void validateThumbnail(org.apache.sling.api.request.RequestParameter thumbnail, Errors errors) {
        if (thumbnail == null) {
            return;
        }
        String mimeType = thumbnail.getContentType();
        if (!ALLOWED_THUMBNAIL_MIME_TYPES.contains(mimeType)) {
            errors.add(THUMBNAIL_PARAM_NAME, mimeType, "Allowed image types are: BMP, GIF, JPG, JPEG, PNG");
            return;
        }
        long thumbnailSize = thumbnail.getSize();
        if (thumbnailSize > THUMBNAIL_SIZE_LIMIT_IN_BYTES) {
            errors.add(THUMBNAIL_PARAM_NAME, thumbnailSize, "Maximum file size is " + THUMBNAIL_SIZE_LIMIT_IN_KB + " KB");
        }
    }

    private static final class FilterInput {

        private static final String DEFAULT_PATH = "/"; //NOSONAR

        private String root = DEFAULT_PATH;
        private ImportMode mode = REPLACE;
        private List<RuleInput> rules;

        @JsonSetter
        public void setRoot(String root) {
            this.root = StringUtils.defaultIfBlank(root, DEFAULT_PATH).trim();
        }

        @JsonSetter
        public void setMode(String mode) {
            this.mode = JcrPackageUtil.toImportMode(mode);
        }

        public String getRoot() {
            return root;
        }

        public ImportMode getMode() {
            return mode;
        }

        public List<RuleInput> getRules() {
            return rules;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            FilterInput that = (FilterInput) o;
            return Objects.equals(root, that.root) &&
                    Objects.equals(rules, that.rules);
        }

        @Override
        public int hashCode() {
            return Objects.hash(root, rules);
        }
    }

    private static final class RuleInput {

        private String path;
        private boolean include;

        public String getPath() {
            return path;
        }

        public boolean isInclude() {
            return include;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            RuleInput ruleInput = (RuleInput) o;
            return include == ruleInput.include &&
                    Objects.equals(path, ruleInput.path);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path, include);
        }
    }
}
