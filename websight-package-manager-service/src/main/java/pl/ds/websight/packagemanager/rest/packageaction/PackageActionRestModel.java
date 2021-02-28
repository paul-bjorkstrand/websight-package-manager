package pl.ds.websight.packagemanager.rest.packageaction;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import pl.ds.websight.packagemanager.packageoptions.PackageImportOptions;
import pl.ds.websight.packagemanager.rest.PackagePathValidatable;
import pl.ds.websight.packagemanager.util.JcrPackageUtil;
import pl.ds.websight.request.parameters.support.annotations.RequestParameter;

import javax.annotation.PostConstruct;
import javax.jcr.Session;
import javax.validation.constraints.NotBlank;

import static pl.ds.websight.packagemanager.rest.Messages.PACKAGE_PATH_VALIDATION_ERROR_BLANK_PATH;

@Model(adaptables = SlingHttpServletRequest.class)
public class PackageActionRestModel extends PackagePathValidatable {

    @Self
    private SlingHttpServletRequest request;

    @RequestParameter
    @NotBlank(message = PACKAGE_PATH_VALIDATION_ERROR_BLANK_PATH)
    private String path;

    @RequestParameter
    private String acHandling;

    @RequestParameter
    @Default(booleanValues = true)
    private Boolean extractSubpackages;

    private PackageImportOptions packageImportOptions = PackageImportOptions.DEFAULT;

    @PostConstruct
    protected void init() {
        org.apache.sling.api.request.RequestParameter dryRunParam = request.getRequestParameter("dryRun");
        packageImportOptions.setDryRun(dryRunParam != null && BooleanUtils.toBoolean(dryRunParam.getString()));
        if (StringUtils.isNotBlank(acHandling)) {
            packageImportOptions.setAcHandling(JcrPackageUtil.toAcHandling(acHandling));
        }
        packageImportOptions.setExtractSubpackages(extractSubpackages);
    }

    public Session getSession() {
        return request.getResourceResolver().adaptTo(Session.class);
    }

    public String getPath() {
        return path;
    }

    public PackageImportOptions getPackageImportOptions() {
        return packageImportOptions;
    }
}
