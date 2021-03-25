package pl.ds.websight.packagemanager.rest;

import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import pl.ds.websight.packagemanager.dto.PackageDto;
import pl.ds.websight.rest.framework.RestAction;
import pl.ds.websight.rest.framework.RestActionResult;
import pl.ds.websight.rest.framework.annotations.SlingAction;

import javax.jcr.RepositoryException;
import java.io.IOException;

@Component(service = RestAction.class)
@SlingAction
public class EditPackageRestAction extends AbstractEditableRestAction {

    @Override
    protected RestActionResult<PackageDto> performAction(EditPackageRestModel model) throws IOException, RepositoryException {
        return performEdit(model);
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
