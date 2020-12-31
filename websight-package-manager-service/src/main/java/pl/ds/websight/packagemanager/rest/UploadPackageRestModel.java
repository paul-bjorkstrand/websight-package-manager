package pl.ds.websight.packagemanager.rest;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import pl.ds.websight.request.parameters.support.annotations.RequestParameter;
import pl.ds.websight.rest.framework.Errors;
import pl.ds.websight.rest.framework.Validatable;

import javax.annotation.PostConstruct;
import javax.jcr.Session;

@Model(adaptables = SlingHttpServletRequest.class)
public class UploadPackageRestModel implements Validatable {

    private static final String FILE_PARAM_NAME = "file";

    @Self
    private SlingHttpServletRequest request;

    @RequestParameter
    @Default(booleanValues = false)
    private Boolean force;

    private org.apache.sling.api.request.RequestParameter file;

    @PostConstruct
    protected void init() {
        this.file = request.getRequestParameter(FILE_PARAM_NAME);
    }

    public Session getSession() {
        return request.getResourceResolver().adaptTo(Session.class);
    }

    public String getRequestContentType() {
        return request.getContentType();
    }

    public org.apache.sling.api.request.RequestParameter getFileParam() {
        return file;
    }

    public SlingHttpServletRequest getRequest() {
        return request;
    }

    public boolean isForce() {
        return Boolean.TRUE.equals(force);
    }

    @Override
    public Errors validate() {
        Errors errors = Errors.createErrors();
        return file == null ?
                errors.add(FILE_PARAM_NAME, null, "Request does not contain a file") :
                errors;
    }
}
