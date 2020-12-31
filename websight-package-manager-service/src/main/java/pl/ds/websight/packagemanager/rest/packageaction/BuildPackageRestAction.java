package pl.ds.websight.packagemanager.rest.packageaction;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import pl.ds.websight.packagemanager.dto.PackageActionStateDto;
import pl.ds.websight.packagemanager.rest.AbstractRestAction;
import pl.ds.websight.packagemanager.rest.Messages;
import pl.ds.websight.packagemanager.rest.requestparameters.PackageActionCommand;
import pl.ds.websight.rest.framework.RestAction;
import pl.ds.websight.rest.framework.RestActionResult;
import pl.ds.websight.rest.framework.annotations.SlingAction;

import javax.jcr.RepositoryException;

@Component
@SlingAction
public class BuildPackageRestAction extends AbstractRestAction<PackageActionRestModel, PackageActionStateDto>
        implements RestAction<PackageActionRestModel, PackageActionStateDto> {

    @Reference
    private PackageActionProcessor processor;

    @Override
    protected RestActionResult<PackageActionStateDto> performAction(PackageActionRestModel model) throws RepositoryException {
        return processor.process(model.getPath(), model.getSession(), PackageActionCommand.BUILD, false);
    }

    @Override
    protected String getUnexpectedErrorMessage() {
        return Messages.BUILD_PACKAGE_ERROR;
    }
}
