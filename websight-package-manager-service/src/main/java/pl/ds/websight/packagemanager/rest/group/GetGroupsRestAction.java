package pl.ds.websight.packagemanager.rest.group;

import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import pl.ds.websight.packagemanager.dto.GroupDto;
import pl.ds.websight.packagemanager.dto.GroupListDto;
import pl.ds.websight.packagemanager.rest.AbstractRestAction;
import pl.ds.websight.packagemanager.rest.Messages;
import pl.ds.websight.packagemanager.util.JcrPackageUtil;
import pl.ds.websight.rest.framework.RestAction;
import pl.ds.websight.rest.framework.RestActionResult;
import pl.ds.websight.rest.framework.annotations.SlingAction;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static pl.ds.websight.rest.framework.annotations.SlingAction.HttpMethod.GET;

@Component
@Designate(ocd = GetGroupsRestAction.Config.class)
@SlingAction(GET)
public class GetGroupsRestAction extends AbstractRestAction<GetGroupsRestModel, GroupListDto>
        implements RestAction<GetGroupsRestModel, GroupListDto> {

    private Config config;

    //This implementation doesn't handle case when package is provided by some PackageRegistry
    @Override
    protected RestActionResult<GroupListDto> performAction(GetGroupsRestModel model) throws RepositoryException {
        return RestActionResult.success(listGroups(model.getSession(), config.count_limit()));
    }

    private static GroupListDto listGroups(Session session, int limit) throws RepositoryException {
        long allPackagesCount = 0;
        List<GroupDto> result = new ArrayList<>();
        if (session.nodeExists(JcrPackageUtil.PACKAGES_ROOT_PATH)) {
            // Packages without group assigned
            Node packageRoot = session.getNode(JcrPackageUtil.PACKAGES_ROOT_PATH);
            long noGroupPackagesCount = JcrPackageUtil.countPackages(packageRoot, limit, false);
            addGroup(result, JcrPackageUtil.NO_GROUP, noGroupPackagesCount, limit);
            allPackagesCount += noGroupPackagesCount;

            // Packages with group assigned
            final boolean countPackagesRecursively = true;
            for (Node group : getChildGroups(packageRoot)) {
                String groupName = JcrPackageUtil.getGroupIdFromNode(packageRoot, group);
                long packagesCount = JcrPackageUtil.countPackages(group, limit, countPackagesRecursively);
                addGroup(result, groupName, packagesCount, limit);
                if (!groupName.contains("/")) {
                    // Add only top level groups counts when done recursively
                    allPackagesCount += packagesCount;
                }
            }
            result.sort(Comparator.comparing(GroupDto::getName, String::compareToIgnoreCase));
        }
        return new GroupListDto(allPackagesCount, limit, result);
    }

    @NotNull
    private static List<Node> getChildGroups(Node root) throws RepositoryException {
        List<Node> result = new ArrayList<>();
        NodeIterator nodeIterator = root.getNodes();
        while (nodeIterator.hasNext()) {
            Node child = nodeIterator.nextNode();
            String name = child.getName();
            if (!".snapshot".equals(name) && !JcrPackageUtil.isValidPackageNode(child)) {
                result.add(child);
                result.addAll(getChildGroups(child));
            }
        }
        return result;
    }

    private static void addGroup(List<GroupDto> groups, String groupName, long packagesCount, long limit) {
        if (packagesCount > 0) {
            groups.add(new GroupDto(groupName, packagesCount, limit));
        }
    }

    @Override
    protected String getUnexpectedErrorMessage() {
        return Messages.GET_GROUPS_ERROR;
    }

    @Activate
    private void activate(Config config) {
        this.config = config;
    }

    @ObjectClassDefinition(name = "WebSight Package Manager: Get Groups Rest Action Configuration")
    public @interface Config {

        @AttributeDefinition(
                name = "Package count limit",
                description = "Maximum number of 'count' property returned by 'get-groups' action.",
                type = AttributeType.INTEGER
        )
        int count_limit() default 10000; // NOSONAR

    }

}
