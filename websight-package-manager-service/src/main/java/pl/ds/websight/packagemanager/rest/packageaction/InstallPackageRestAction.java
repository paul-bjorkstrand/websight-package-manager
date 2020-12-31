package pl.ds.websight.packagemanager.rest.packageaction;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import pl.ds.websight.packagemanager.dto.PackageActionStateDto;
import pl.ds.websight.packagemanager.rest.AbstractRestAction;
import pl.ds.websight.packagemanager.rest.Messages;
import pl.ds.websight.packagemanager.rest.PackagePrerequisiteValidator;
import pl.ds.websight.packagemanager.rest.requestparameters.PackageActionCommand;
import pl.ds.websight.packagemanager.util.JcrPackageStatusUtil;
import pl.ds.websight.rest.framework.RestAction;
import pl.ds.websight.rest.framework.RestActionResult;
import pl.ds.websight.rest.framework.annotations.SlingAction;

import javax.jcr.RepositoryException;

@Component
@SlingAction
public class InstallPackageRestAction extends AbstractRestAction<PackageActionRestModel, PackageActionStateDto>
        implements RestAction<PackageActionRestModel, PackageActionStateDto> {

    public static final PackagePrerequisiteValidator[] ACTION_PRE_VALIDATORS = new PackagePrerequisiteValidator[] {
            new PackagePrerequisiteValidator(
                    JcrPackageStatusUtil::isBuilt,
                    "Package was never built",
                    packageToInstallPath -> "Package: " + packageToInstallPath + " was never built"),
            new PackagePrerequisiteValidator(
                    JcrPackageStatusUtil::hasUnresolvedDependencies,
                    "Package has unresolved dependencies",
                    packageToInstallPath -> "Package: " + packageToInstallPath + " has unresolved dependencies")
    };

    @Reference
    private PackageActionProcessor processor;

    @Override
    protected RestActionResult<PackageActionStateDto> performAction(PackageActionRestModel model) throws RepositoryException {
        return processor.process(model.getPath(), model.getSession(), PackageActionCommand.INSTALL, model.isDryRun(),
                ACTION_PRE_VALIDATORS);
    }

    @Override
    protected String getUnexpectedErrorMessage() {
        return Messages.INSTALL_PACKAGE_ERROR;
    }

}
