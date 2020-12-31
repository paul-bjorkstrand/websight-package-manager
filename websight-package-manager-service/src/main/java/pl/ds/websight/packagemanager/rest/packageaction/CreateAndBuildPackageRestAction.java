package pl.ds.websight.packagemanager.rest.packageaction;

import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ds.websight.packagemanager.dto.PackageDto;
import pl.ds.websight.packagemanager.rest.AbstractCreatableRestAction;
import pl.ds.websight.packagemanager.rest.PackagePathSaveHelper;
import pl.ds.websight.packagemanager.rest.PackageRestModel;
import pl.ds.websight.packagemanager.rest.requestparameters.PackageActionCommand;
import pl.ds.websight.rest.framework.RestAction;
import pl.ds.websight.rest.framework.RestActionResult;
import pl.ds.websight.rest.framework.annotations.SlingAction;

import javax.jcr.RepositoryException;
import java.io.IOException;

@Component(service = RestAction.class)
@SlingAction
public class CreateAndBuildPackageRestAction extends AbstractCreatableRestAction {

    private static final Logger LOG = LoggerFactory.getLogger(CreateAndBuildPackageRestAction.class);

    @Reference
    private PackageActionProcessor processor;

    @Override
    protected RestActionResult performAction(PackageRestModel model) throws IOException, RepositoryException {
        PackagePathSaveHelper pathSaveHelper = new PackagePathSaveHelper(model.getRequest(), this.getClass());
        RestActionResult<PackageDto> creationResult = performCreation(model, pathSaveHelper);
        if (RestActionResult.Status.FAILURE == creationResult.getStatus()) {
            LOG.warn("Could not execute package build due to creation failure");
            return creationResult;
        }
        return processor.processAfterPreviousAction(pathSaveHelper.getPathRequestAttribute(), model.getSession(),
                PackageActionCommand.BUILD, "created");
    }

    @Override
    @Reference
    protected void setPackaging(Packaging packaging) {
        super.packaging = packaging;
    }

    @Override
    @Reference
    protected void setJobManager(JobManager jobManager) {
        super.jobManager = jobManager;
    }
}
