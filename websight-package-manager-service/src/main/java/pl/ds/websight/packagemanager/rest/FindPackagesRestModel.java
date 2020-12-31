package pl.ds.websight.packagemanager.rest;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import pl.ds.websight.packagemanager.rest.requestparameters.FilterOption;
import pl.ds.websight.packagemanager.rest.requestparameters.SortBy;
import pl.ds.websight.request.parameters.support.annotations.RequestParameter;

import javax.annotation.PostConstruct;
import javax.jcr.Session;
import java.util.Collections;
import java.util.List;

@Model(adaptables = SlingHttpServletRequest.class)
public class FindPackagesRestModel {

    @RequestParameter
    private SortBy sortBy;

    @RequestParameter
    private List<FilterOption> filterOptions;

    @RequestParameter(name = "filter")
    private String packageNameFilter;

    @RequestParameter
    private String group;

    @RequestParameter
    private String path;

    @RequestParameter
    @Default(longValues = 1L)
    private Long pageNumber;

    @Self
    private SlingHttpServletRequest request;

    private Session session;

    @PostConstruct
    protected void init() {
        session = request.getResourceResolver().adaptTo(Session.class);
        sortBy = ObjectUtils.defaultIfNull(sortBy, SortBy.LAST_USED_DESC);
        filterOptions = ObjectUtils.defaultIfNull(filterOptions, Collections.emptyList());
        packageNameFilter = StringUtils.defaultString(packageNameFilter, "");
    }

    public SortBy getSortBy() {
        return sortBy;
    }

    public List<FilterOption> getFilterOptions() {
        return filterOptions;
    }

    public String getPackageNameFilter() {
        return packageNameFilter;
    }

    public String getGroup() {
        return group;
    }

    public String getPath() {
        return path;
    }

    public Session getSession() {
        return session;
    }

    public String getUserID() {
        return getSession().getUserID();
    }

    public Long getPageNumber() {
        return pageNumber >= 0 ? pageNumber : 0;
    }
}
