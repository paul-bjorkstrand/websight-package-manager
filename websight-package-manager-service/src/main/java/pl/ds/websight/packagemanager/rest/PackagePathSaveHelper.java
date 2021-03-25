package pl.ds.websight.packagemanager.rest;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

public final class PackagePathSaveHelper {

    private static final Logger LOG = LoggerFactory.getLogger(PackagePathSaveHelper.class);

    private static final String PATH_ATTR_NAME_SUFFIX = ".savedPackagePath";

    private final SlingHttpServletRequest request;
    private final String pathAttrName;

    public PackagePathSaveHelper(SlingHttpServletRequest request, Class<?> applicantClass) {
        this.request = request;
        this.pathAttrName = applicantClass.getName() + PATH_ATTR_NAME_SUFFIX;
    }

    public void setPathRequestAttribute(Node packageNode) {
        try {
            String packagePath = packageNode != null ? packageNode.getPath() : null;
            if (StringUtils.isNotBlank(packagePath)) {
                RequestUtil.setRequestAttribute(request, pathAttrName, packagePath);
            }
        } catch (RepositoryException e) {
            LOG.warn("Could not save path in request attribute", e);
        }
    }

    public String getPathRequestAttribute() {
        Object pathAttr = request.getAttribute(pathAttrName);
        return pathAttr != null ? pathAttr.toString() : null;
    }
}
