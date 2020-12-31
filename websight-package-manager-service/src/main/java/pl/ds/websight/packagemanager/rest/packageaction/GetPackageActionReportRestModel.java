package pl.ds.websight.packagemanager.rest.packageaction;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import pl.ds.websight.packagemanager.rest.PackagePathValidatable;
import pl.ds.websight.request.parameters.support.annotations.RequestParameter;

import javax.jcr.Session;
import javax.validation.constraints.NotBlank;

import static pl.ds.websight.packagemanager.rest.Messages.PACKAGE_PATH_VALIDATION_ERROR_BLANK_PATH;

@Model(adaptables = SlingHttpServletRequest.class)
public class GetPackageActionReportRestModel extends PackagePathValidatable {

    @SlingObject
    private ResourceResolver resolver;

    @RequestParameter
    @NotBlank(message = PACKAGE_PATH_VALIDATION_ERROR_BLANK_PATH)
    private String path;

    public Session getSession() {
        return resolver.adaptTo(Session.class);
    }

    @Override
    public String getPath() {
        return path;
    }
}
