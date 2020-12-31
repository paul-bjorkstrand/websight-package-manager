package pl.ds.websight.packagemanager.rest.packageaction;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import pl.ds.websight.packagemanager.rest.Messages;
import pl.ds.websight.packagemanager.util.JcrPackageUtil;
import pl.ds.websight.request.parameters.support.annotations.RequestParameter;
import pl.ds.websight.rest.framework.Errors;
import pl.ds.websight.rest.framework.Validatable;

import javax.jcr.Session;
import javax.validation.constraints.NotEmpty;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static pl.ds.websight.packagemanager.rest.requestparameters.CommonParameterConstants.PACKAGE_PATH_PARAM_NAME;

@Model(adaptables = SlingHttpServletRequest.class)
public class GetPackagesActionsStatesRestModel implements Validatable {

    @SlingObject
    private ResourceResolver resolver;

    @RequestParameter(name = PACKAGE_PATH_PARAM_NAME)
    @NotEmpty(message = "Packages paths cannot be empty")
    private List<String> paths;

    public Session getSession() {
        return resolver.adaptTo(Session.class);
    }

    public List<String> getPaths() {
        return paths;
    }

    @Override
    public Errors validate() {
        Errors errors = Errors.createErrors();
        List<String> invalidPaths = paths.stream()
                .filter(path -> !path.startsWith(JcrPackageUtil.PACKAGES_ROOT_PATH))
                .collect(toList());
        return invalidPaths.isEmpty() ?
                errors :
                errors.add(PACKAGE_PATH_PARAM_NAME, invalidPaths, Messages.PACKAGE_PATHS_VALIDATION_ERROR_INVALID_PATHS);
    }
}
