package pl.ds.websight.packagemanager.rest.schedule;

import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import pl.ds.websight.packagemanager.JobProperties;
import pl.ds.websight.packagemanager.dto.PackageScheduleActionInfoDto;
import pl.ds.websight.packagemanager.dto.ScheduledPackageActionsDto;
import pl.ds.websight.packagemanager.rest.AbstractRestAction;
import pl.ds.websight.packagemanager.rest.Messages;
import pl.ds.websight.packagemanager.rest.packageaction.PackageActionRestModel;
import pl.ds.websight.packagemanager.util.JobUtil;
import pl.ds.websight.rest.framework.RestAction;
import pl.ds.websight.rest.framework.RestActionResult;
import pl.ds.websight.rest.framework.annotations.SlingAction;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static pl.ds.websight.rest.framework.annotations.SlingAction.HttpMethod.GET;

@Component
@SlingAction(GET)
public class GetScheduledPackageActionsRestAction extends AbstractRestAction<PackageActionRestModel, ScheduledPackageActionsDto>
        implements RestAction<PackageActionRestModel, ScheduledPackageActionsDto> {

    @Reference
    private JobManager jobManager;

    @Override
    protected RestActionResult<ScheduledPackageActionsDto> performAction(PackageActionRestModel model) throws RepositoryException {
        Session session = model.getSession();
        String path = model.getPath();
        if (!session.nodeExists(path)) {
            return RestActionResult.failure(Messages.GET_PACKAGE_SCHEDULED_ACTIONS_ERROR,
                    Messages.formatMessage(Messages.GET_PACKAGE_SCHEDULED_ACTIONS_ERROR_NO_PACKAGE_NODE_DETAILS, path));
        }
        List<PackageScheduleActionInfoDto> scheduledActions =
                JobUtil.findAllScheduledJobs(jobManager, JobProperties.asQueryMap(path)).stream()
                        .map(PackageScheduleActionInfoDto::asFullInfo)
                        .collect(toList());
        return RestActionResult.success(new ScheduledPackageActionsDto(scheduledActions));
    }

    @Override
    protected String getUnexpectedErrorMessage() {
        return Messages.GET_PACKAGE_SCHEDULED_ACTIONS_ERROR;
    }
}
