package pl.ds.websight.packagemanager.rest;

import pl.ds.websight.packagemanager.util.JcrPackageUtil;
import pl.ds.websight.rest.framework.Errors;
import pl.ds.websight.rest.framework.Validatable;

import static pl.ds.websight.packagemanager.rest.requestparameters.CommonParameterConstants.PACKAGE_PATH_PARAM_NAME;

public abstract class PackagePathValidatable implements Validatable {

    protected abstract String getPath();

    @Override
    public Errors validate() {
        Errors errors = Errors.createErrors();
        String packagePath = getPath();
        return !packagePath.startsWith(JcrPackageUtil.PACKAGES_ROOT_PATH) ?
                errors.add(PACKAGE_PATH_PARAM_NAME, packagePath, Messages.PACKAGE_PATH_VALIDATION_ERROR_INVALID_PATH) :
                errors;
    }
}
