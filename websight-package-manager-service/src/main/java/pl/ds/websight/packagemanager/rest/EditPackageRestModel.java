package pl.ds.websight.packagemanager.rest;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import pl.ds.websight.request.parameters.support.annotations.RequestParameter;

import javax.validation.constraints.NotBlank;

import static pl.ds.websight.packagemanager.rest.Messages.PACKAGE_PATH_VALIDATION_ERROR_BLANK_PATH;
import static pl.ds.websight.packagemanager.rest.requestparameters.CommonParameterConstants.PACKAGE_PATH_PARAM_NAME;

@Model(adaptables = SlingHttpServletRequest.class)
public class EditPackageRestModel extends PackageRestModel {

    @RequestParameter(name = PACKAGE_PATH_PARAM_NAME)
    @NotBlank(message = PACKAGE_PATH_VALIDATION_ERROR_BLANK_PATH)
    private String editedPackagePath;

    @RequestParameter
    @Default(booleanValues = false)
    private Boolean deleteThumbnail;

    public String getEditedPackagePath() {
        return editedPackagePath;
    }

    public boolean getDeleteThumbnail() {
        return Boolean.TRUE.equals(deleteThumbnail);
    }
}
