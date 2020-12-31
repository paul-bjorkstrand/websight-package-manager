package pl.ds.websight.packagemanager.rest;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ds.websight.packagemanager.dto.PackageUploadDto;
import pl.ds.websight.packagemanager.util.JcrPackageUtil;
import pl.ds.websight.rest.framework.RestAction;
import pl.ds.websight.rest.framework.RestActionResult;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public abstract class AbstractUploadableRestAction extends AbstractRestAction<UploadPackageRestModel, PackageUploadDto>
        implements RestAction<UploadPackageRestModel, PackageUploadDto> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractUploadableRestAction.class);

    private static final String VALID_CONTENT_TYPE_PREFIX = "multipart";

    protected Packaging packaging;

    protected abstract void setPackaging(Packaging packaging);

    protected RestActionResult<PackageUploadDto> performUpload(UploadPackageRestModel model) throws IOException, RepositoryException {
        return performUpload(model, null);
    }

    protected RestActionResult<PackageUploadDto> performUpload(UploadPackageRestModel model, PackagePathSaveHelper pathSaveHelper)
            throws IOException, RepositoryException {
        if (!StringUtils.startsWithIgnoreCase(model.getRequestContentType(), VALID_CONTENT_TYPE_PREFIX + '/')) {
            return RestActionResult.failure(
                    Messages.UPLOAD_PACKAGE_ERROR,
                    Messages.formatMessage(Messages.UPLOAD_PACKAGE_ERROR_CONTENT_TYPE_DETAILS, VALID_CONTENT_TYPE_PREFIX));
        }
        if (model.getFileParam().isFormField()) {
            return RestActionResult.failure(
                    Messages.UPLOAD_PACKAGE_ERROR,
                    Messages.UPLOAD_PACKAGE_ERROR_FILE_IS_FORM_FIELD_DETAILS);
        }

        try (InputStream packageStream = model.getFileParam().getInputStream();
             JcrPackage uploadedPackage = uploadPackage(packageStream, model.getSession(), model.isForce())) {
            LOG.debug("Successfully uploaded a package");
            if (pathSaveHelper != null) {
                pathSaveHelper.setPathRequestAttribute(uploadedPackage.getNode());
            }
            return RestActionResult.success(
                    Messages.UPLOAD_PACKAGE_SUCCESS,
                    Messages.formatMessage(Messages.UPLOAD_PACKAGE_SUCCESS_DETAILS, JcrPackageUtil.getSimplePackageName(uploadedPackage)),
                    new PackageUploadDto(getPath(uploadedPackage)));
        } catch (ItemExistsException e) {
            return RestActionResult.failure(
                    Messages.UPLOAD_PACKAGE_ERROR,
                    Messages.UPLOAD_PACKAGE_ERROR_ALREADY_EXISTS_DETAILS);
        }
    }

    private String getPath(JcrPackage uploadedPackage) throws RepositoryException {
        Node packageNode = Optional.ofNullable(uploadedPackage).map(JcrPackage::getNode).orElse(null);
        if (packageNode != null) {
            return packageNode.getPath();
        }
        return "";
    }

    private JcrPackage uploadPackage(InputStream packageStream, Session session, boolean force)
            throws IOException, RepositoryException {
        LOG.debug("Starting package upload");
        JcrPackageManager manager = packaging.getPackageManager(session);
        return manager.upload(packageStream, force);
    }

    @Override
    protected String getUnexpectedErrorMessage() {
        return Messages.UPLOAD_PACKAGE_ERROR;
    }

}
