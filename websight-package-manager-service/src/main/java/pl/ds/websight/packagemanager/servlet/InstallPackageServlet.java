package pl.ds.websight.packagemanager.servlet;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;

import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.fs.io.ImportOptions;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;

@Component(
        service = Servlet.class,
        property = {
                SLING_SERVLET_METHODS + "=[\"GET\", \"POST\", \"PUT\"]"
       })
@SlingServletPaths(value = "/apps/websight-package-manager-service/bin/install")
public class InstallPackageServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(InstallPackageServlet.class);
    private static final String FILE_PARAMETER = "file";

    @Reference
    private transient Packaging packaging;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        response.getWriter().append("InstallPackageServlet is running.");
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        doInstall(request, response);
    }

    @Override
    protected void doPut(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        doInstall(request, response);
    }

    private void doInstall(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        RequestParameter file = request.getRequestParameter(FILE_PARAMETER);
        if (file == null) {
            throw new IllegalArgumentException("Parameter 'file' cannot be empty");
        }

        Session session = request.getResourceResolver().adaptTo(Session.class);
        if (session == null) {
            throw new IllegalArgumentException("Session is null");
        }
        LOG.info("Attempting to install package: {}", file.getFileName());
        int status = install(file, session);
        response.getWriter().append("<document><response><status><code>" + status + "</code></status></response></document>");
        response.setStatus(status);
    }

    private int install(@NotNull RequestParameter file, @NotNull Session session) {
        try (InputStream is = file.getInputStream()) {
            JcrPackageManager manager = packaging.getPackageManager(session);
            JcrPackage jcrPackage = manager.upload(is, true);
            ImportOptions options = new ImportOptions();
            options.setAccessControlHandling(AccessControlHandling.MERGE);
            jcrPackage.install(options);
            return HttpServletResponse.SC_OK;
        } catch (RepositoryException e) {
            LOG.error(e.getMessage(), e);
            return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        } catch (PackageException e) {
            LOG.error("Error during package installation", e);
            return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        } catch (IOException e) {
            LOG.error("Error while reading input file", e);
            return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }
    }
}