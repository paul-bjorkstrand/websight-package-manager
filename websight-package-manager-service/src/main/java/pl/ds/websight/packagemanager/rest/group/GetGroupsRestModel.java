package pl.ds.websight.packagemanager.rest.group;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import javax.jcr.Session;

@Model(adaptables = SlingHttpServletRequest.class)
public class GetGroupsRestModel {

    @SlingObject
    private ResourceResolver resourceResolver;

    public Session getSession() {
        return resourceResolver.adaptTo(Session.class);
    }
}
