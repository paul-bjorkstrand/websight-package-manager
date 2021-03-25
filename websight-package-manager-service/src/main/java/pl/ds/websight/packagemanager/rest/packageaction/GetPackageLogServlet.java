package pl.ds.websight.packagemanager.rest.packageaction;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ds.websight.packagemanager.rest.Messages;
import pl.ds.websight.packagemanager.util.JcrPackageUtil;
import pl.ds.websight.packagemanager.util.PackageLogUtil;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_EXTENSIONS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static pl.ds.websight.packagemanager.rest.requestparameters.CommonParameterConstants.PACKAGE_PATH_PARAM_NAME;

@Component(
        service = Servlet.class,
        property = {
                SLING_SERVLET_METHODS + '=' + HttpConstants.METHOD_GET,
                SLING_SERVLET_EXTENSIONS + '=' + GetPackageLogServlet.ACTION_LOG_EXTENSION
        })
@SlingServletPaths(value = "/apps/websight-package-manager-service/bin/package")
public class GetPackageLogServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = -4296175383327717326L;
    private static final Logger LOG = LoggerFactory.getLogger(GetPackageLogServlet.class);

    static final String ACTION_LOG_EXTENSION = "log";

    @Override
    protected void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws IOException {
        response.setContentType("text/plain");
        response.setCharacterEncoding(StandardCharsets.UTF_8.toString());
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        String packagePath = getPackagePath(request);
        if (packagePath == null) {
            response.sendError(SC_BAD_REQUEST, Messages.PACKAGE_PATH_VALIDATION_ERROR_INVALID_PATH);
            return;
        }
        Session session = request.getResourceResolver().adaptTo(Session.class);
        if (session == null) {
            response.sendError(SC_INTERNAL_SERVER_ERROR, Messages.GET_PACKAGE_LOG_ERROR_NO_USER_SESSION);
            return;
        }
        String logPath = PackageLogUtil.getLogPath(packagePath);
        try {
            if (session.nodeExists(packagePath) && session.nodeExists(logPath)) {
                printLogs(response, logPath, session);
            } else {
                response.sendError(SC_NOT_FOUND, Messages.formatMessage(Messages.GET_PACKAGE_LOG_ERROR_NO_LOGS_DETAILS, packagePath));
            }
        } catch (Exception e) {
            LOG.warn("Could not list full logs for package: {}", packagePath, e);
            if (!response.isCommitted()) {
                response.sendError(SC_INTERNAL_SERVER_ERROR,
                        Messages.formatMessage(Messages.GET_PACKAGE_LOG_ERROR_NO_LOGS_DETAILS, packagePath));
            }
        }
    }

    private static String getPackagePath(SlingHttpServletRequest request) {
        RequestParameter pathParam = request.getRequestParameter(PACKAGE_PATH_PARAM_NAME);
        if (pathParam != null) {
            String path = pathParam.getString();
            if (path.startsWith(JcrPackageUtil.PACKAGES_ROOT_PATH)) {
                return path;
            }
        }
        return null;
    }

    private static void printLogs(SlingHttpServletResponse response, String logPath, Session session)
            throws RepositoryException, IOException {
        Node logNode = session.getNode(logPath);
        List<String> fullLog = PackageLogUtil.getLog(logNode, true);
        try (PrintWriter writer = response.getWriter()) {
            fullLog.forEach(writer::append);
        }
    }
}
