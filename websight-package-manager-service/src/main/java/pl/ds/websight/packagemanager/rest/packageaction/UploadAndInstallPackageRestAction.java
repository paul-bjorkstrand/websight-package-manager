package pl.ds.websight.packagemanager.rest.packageaction;

import org.apache.jackrabbit.vault.packaging.Packaging;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ds.websight.packagemanager.dto.PackageUploadDto;
import pl.ds.websight.packagemanager.rest.AbstractUploadableRestAction;
import pl.ds.websight.packagemanager.rest.Messages;
import pl.ds.websight.packagemanager.rest.PackagePathSaveHelper;
import pl.ds.websight.packagemanager.rest.UploadPackageRestModel;
import pl.ds.websight.packagemanager.rest.requestparameters.PackageActionCommand;
import pl.ds.websight.rest.framework.RestAction;
import pl.ds.websight.rest.framework.RestActionResult;
import pl.ds.websight.rest.framework.annotations.SlingAction;

import javax.jcr.RepositoryException;
import java.io.IOException;

@Component(service = RestAction.class)
@SlingAction
public class UploadAndInstallPackageRestAction extends AbstractUploadableRestAction {

    private static final Logger LOG = LoggerFactory.getLogger(UploadAndInstallPackageRestAction.class);

    @Reference
    private PackageActionProcessor processor;

    @Override
    protected RestActionResult performAction(UploadPackageRestModel model) throws IOException, RepositoryException {
        PackagePathSaveHelper pathSaveHelper = new PackagePathSaveHelper(model.getRequest(), this.getClass());
        RestActionResult<PackageUploadDto> uploadResult = performUpload(model, pathSaveHelper);
        if (RestActionResult.Status.FAILURE == uploadResult.getStatus()) {
            LOG.warn("Could not execute package installation due to uploading failure");
            return uploadResult;
        }
        return processor.processAfterPreviousAction(pathSaveHelper.getPathRequestAttribute(), model.getSession(),
                PackageActionCommand.INSTALL, "uploaded", InstallPackageRestAction.ACTION_PRE_VALIDATORS);
    }

    @Override
    @Reference
    protected void setPackaging(Packaging packaging) {
        super.packaging = packaging;
    }

    @Override
    protected String getUnexpectedErrorMessage() {
        return Messages.UPLOAD_PACKAGE_ERROR;
    }

}
