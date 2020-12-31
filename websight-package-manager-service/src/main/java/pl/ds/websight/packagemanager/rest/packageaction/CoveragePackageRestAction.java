package pl.ds.websight.packagemanager.rest.packageaction;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import pl.ds.websight.packagemanager.dto.PackageActionStateDto;
import pl.ds.websight.packagemanager.rest.AbstractRestAction;
import pl.ds.websight.packagemanager.rest.Messages;
import pl.ds.websight.packagemanager.rest.PackagePrerequisiteValidator;
import pl.ds.websight.packagemanager.rest.requestparameters.PackageActionCommand;
import pl.ds.websight.packagemanager.util.JcrPackageUtil;
import pl.ds.websight.rest.framework.RestAction;
import pl.ds.websight.rest.framework.RestActionResult;
import pl.ds.websight.rest.framework.annotations.SlingAction;

import javax.jcr.RepositoryException;

@Component
@SlingAction
public class CoveragePackageRestAction extends AbstractRestAction<PackageActionRestModel, PackageActionStateDto>
        implements RestAction<PackageActionRestModel, PackageActionStateDto> {

    private static final PackagePrerequisiteValidator ACTION_PRE_VALIDATOR = new PackagePrerequisiteValidator(
            JcrPackageUtil::hasValidWorkspaceFilter,
            "Package does not contain Workspace filter",
            packageToCoverPath -> "Package: " + packageToCoverPath + " does not contain Workspace filter");

    @Reference
    private PackageActionProcessor processor;

    @Override
    protected RestActionResult<PackageActionStateDto> performAction(PackageActionRestModel model) throws RepositoryException {
        return processor.process(model.getPath(), model.getSession(), PackageActionCommand.COVERAGE, false, ACTION_PRE_VALIDATOR);
    }

    @Override
    protected String getUnexpectedErrorMessage() {
        return Messages.COVERAGE_PACKAGE_ERROR;
    }

}
