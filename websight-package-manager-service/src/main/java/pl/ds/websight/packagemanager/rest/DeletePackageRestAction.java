package pl.ds.websight.packagemanager.rest;

import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ds.websight.packagemanager.util.JcrPackageUtil;
import pl.ds.websight.packagemanager.util.OpenPackageException;
import pl.ds.websight.packagemanager.util.PackageLogUtil;
import pl.ds.websight.rest.framework.RestAction;
import pl.ds.websight.rest.framework.RestActionResult;
import pl.ds.websight.rest.framework.annotations.SlingAction;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

@Component
@SlingAction
public class DeletePackageRestAction extends AbstractRestAction<DeletePackageRestModel, Void>
        implements RestAction<DeletePackageRestModel, Void> {

    private static final Logger LOG = LoggerFactory.getLogger(DeletePackageRestAction.class);

    @Reference
    private Packaging packaging;

    @Override
    protected RestActionResult<Void> performAction(DeletePackageRestModel model) throws RepositoryException {
        Session session = model.getSession();
        String packagePath = model.getPath();
        try {
            processDelete(packagePath, session, packaging.getPackageManager(session));
            return RestActionResult.success(
                    Messages.DELETE_PACKAGE_SUCCESS,
                    Messages.formatMessage(Messages.DELETE_PACKAGE_SUCCESS_DETAILS, packagePath));
        } catch (OpenPackageException e) {
            LOG.warn("Could not open package: {}", packagePath, e);
            return RestActionResult.failure(e.getSimplifiedMessage(), e.getMessage());
        }
    }

    public static void processDelete(String packagePath, Session session, JcrPackageManager packageManager)
            throws RepositoryException, OpenPackageException {
        try (JcrPackage packageToDelete = JcrPackageUtil.open(packagePath, session, packageManager)) {
            packageManager.remove(packageToDelete);
            LOG.info("Successfully deleted package: {}", packagePath);
            deletePackageLogs(session, packagePath);
        }
    }

    private static void deletePackageLogs(Session session, String packagePath) throws RepositoryException {
        String logPath = PackageLogUtil.getLogPath(packagePath);
        if (!session.nodeExists(logPath)) {
            return;
        }
        session.removeItem(logPath);
        session.save();
    }

    @Override
    protected String getUnexpectedErrorMessage() {
        return Messages.DELETE_PACKAGE_ERROR;
    }
}
