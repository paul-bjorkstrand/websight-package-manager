package pl.ds.websight.packagemanager.rest.packageaction;

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import pl.ds.websight.packagemanager.dto.PackageActionDto;
import pl.ds.websight.packagemanager.dto.PackageActionReportDto;
import pl.ds.websight.packagemanager.dto.PackageActionStateDto;
import pl.ds.websight.packagemanager.rest.AbstractRestAction;
import pl.ds.websight.packagemanager.rest.Messages;
import pl.ds.websight.packagemanager.util.PackageLogUtil;
import pl.ds.websight.rest.framework.RestAction;
import pl.ds.websight.rest.framework.RestActionResult;
import pl.ds.websight.rest.framework.annotations.SlingAction;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static pl.ds.websight.rest.framework.annotations.SlingAction.HttpMethod.GET;

@Component
@SlingAction(GET)
public class GetPackageActionReportRestAction extends AbstractRestAction<GetPackageActionReportRestModel, PackageActionReportDto>
        implements RestAction<GetPackageActionReportRestModel, PackageActionReportDto> {

    @Reference
    private JobManager jobManager;

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Override
    protected RestActionResult<PackageActionReportDto> performAction(GetPackageActionReportRestModel model) throws RepositoryException {
        String packagePath = model.getPath();
        Session session = model.getSession();
        String logPath = PackageLogUtil.getLogPath(packagePath);
        PackageActionDto action = PackageActionDto.forPackagePath(jobManager, session, packagePath);
        PackageActionStateDto actionState = action.getState();
        if (PackageActionStateDto.UNKNOWN.equals(actionState)) {
            return (!session.nodeExists(packagePath) || !session.nodeExists(logPath)) ?
                    RestActionResult.failure(Messages.GET_PACKAGE_ACTION_REPORT_ERROR,
                            Messages.GET_PACKAGE_ACTION_REPORT_ERROR_DETAILS) :
                    RestActionResult.success(new PackageActionReportDto(action));
        }
        Node logNode = session.getNode(logPath);
        return RestActionResult.success(new PackageActionReportDto(action, PackageLogUtil.getLog(logNode, false)));
    }

    @Override
    protected String getUnexpectedErrorMessage() {
        return Messages.GET_PACKAGE_ACTION_REPORT_ERROR;
    }
}
