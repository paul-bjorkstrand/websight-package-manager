package pl.ds.websight.packagemanager.rest;

import org.apache.jackrabbit.vault.packaging.Packaging;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import pl.ds.websight.packagemanager.dto.PackageUploadDto;
import pl.ds.websight.rest.framework.RestAction;
import pl.ds.websight.rest.framework.RestActionResult;
import pl.ds.websight.rest.framework.annotations.SlingAction;

import javax.jcr.RepositoryException;
import java.io.IOException;

@Component(service = RestAction.class)
@SlingAction
public class UploadPackageRestAction extends AbstractUploadableRestAction {

    @Override
    protected RestActionResult<PackageUploadDto> performAction(UploadPackageRestModel model) throws IOException, RepositoryException {
        return performUpload(model);
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
