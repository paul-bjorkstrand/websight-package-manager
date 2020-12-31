package pl.ds.websight.packagemanager.rest.packageaction;

import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import pl.ds.websight.packagemanager.dto.PackageActionDto;
import pl.ds.websight.packagemanager.rest.AbstractRestAction;
import pl.ds.websight.packagemanager.rest.Messages;
import pl.ds.websight.rest.framework.RestAction;
import pl.ds.websight.rest.framework.RestActionResult;
import pl.ds.websight.rest.framework.annotations.SlingAction;

import javax.jcr.RepositoryException;
import java.util.Map;

import static pl.ds.websight.rest.framework.annotations.SlingAction.HttpMethod.GET;

@Component
@SlingAction(GET)
public class GetPackagesActionsStatesRestAction extends AbstractRestAction<GetPackagesActionsStatesRestModel, Map<String, PackageActionDto>>
        implements RestAction<GetPackagesActionsStatesRestModel, Map<String, PackageActionDto>> {

    @Reference
    private JobManager jobManager;

    @Override
    protected RestActionResult<Map<String, PackageActionDto>> performAction(GetPackagesActionsStatesRestModel model)
            throws RepositoryException {
        String[] paths = model.getPaths().toArray(new String[0]);
        return RestActionResult.success(PackageActionDto.forPackagePaths(jobManager, model.getSession(), paths));
    }

    @Override
    protected String getUnexpectedErrorMessage() {
        return Messages.GET_PACKAGE_ACTION_ERROR;
    }

}
