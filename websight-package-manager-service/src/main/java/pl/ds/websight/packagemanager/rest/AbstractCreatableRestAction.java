package pl.ds.websight.packagemanager.rest;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.event.jobs.JobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ds.websight.packagemanager.JcrPackageEditFacade;
import pl.ds.websight.packagemanager.dto.PackageDto;
import pl.ds.websight.packagemanager.util.JcrPackageUtil;
import pl.ds.websight.rest.framework.RestAction;
import pl.ds.websight.rest.framework.RestActionResult;

import javax.jcr.ItemExistsException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.io.InputStream;

public abstract class AbstractCreatableRestAction extends AbstractRestAction<PackageRestModel, PackageDto>
        implements RestAction<PackageRestModel, PackageDto> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractCreatableRestAction.class);

    protected Packaging packaging;

    protected JobManager jobManager;

    protected abstract void setPackaging(Packaging packaging);

    protected abstract void setJobManager(JobManager jobManager);

    protected RestActionResult<PackageDto> performCreation(PackageRestModel model) throws IOException, RepositoryException {
        return performCreation(model, null);
    }

    protected RestActionResult<PackageDto> performCreation(PackageRestModel model, PackagePathSaveHelper pathSaveHelper)
            throws IOException, RepositoryException {
        Session session = model.getSession();
        JcrPackageManager packageManager = packaging.getPackageManager(session);
        String packageName = model.getName();
        String packageVersion = model.getVersion();
        try (JcrPackage createdPackage = packageManager.create(model.getGroup(), packageName, packageVersion)) {
            JcrPackageEditFacade editFacade = JcrPackageEditFacade.forPackage(createdPackage);
            if (editFacade == null) {
                LOG.warn("Could not access definition after creating a package: {}",
                        JcrPackageUtil.getSimplePackageName(packageName, packageVersion));
                return RestActionResult.failure(
                        Messages.CREATE_PACKAGE_ERROR_CREATED_PARTIALLY,
                        Messages.formatMessage(Messages.CREATE_PACKAGE_ERROR_CREATED_PARTIALLY_DETAILS, packageName),
                        PackageDto.wrap(createdPackage, session, jobManager));
            }
            editFacade.setFilters(model.getFilters());
            String description = model.getDescription();
            if (StringUtils.isNotEmpty(description)) {
                editFacade.setDescription(description);
            }
            editFacade.setAcHandling(model.getAcHandling());
            editFacade.setRequiresRestart(model.requiresRestart());
            editFacade.setDependencies(model.getDependencies());
            if (model.getThumbnail() != null) {
                try (InputStream thumbnailStream = model.getThumbnail().getInputStream()) {
                    editFacade.setThumbnail(thumbnailStream, model.getResourceResolver());
                }
            }
            session.save();
            if (pathSaveHelper != null) {
                pathSaveHelper.setPathRequestAttribute(createdPackage.getNode());
            }
            return RestActionResult.success(
                    Messages.CREATE_PACKAGE_SUCCESS,
                    Messages.formatMessage(Messages.CREATE_PACKAGE_SUCCESS_DETAILS, packageName),
                    PackageDto.wrap(createdPackage, session, jobManager));
        } catch (ItemExistsException e) {
            LOG.warn("Package already exists", e);
            return RestActionResult.failure(
                    Messages.CREATE_PACKAGE_ERROR_ALREADY_EXISTS,
                    Messages.formatMessage(Messages.CREATE_PACKAGE_ERROR_ALREADY_EXISTS_DETAILS, packageName));
        }
    }

    @Override
    protected String getUnexpectedErrorMessage() {
        return Messages.CREATE_PACKAGE_ERROR;
    }
}
