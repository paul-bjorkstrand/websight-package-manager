package pl.ds.websight.packagemanager.dto;

import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.util.JcrConstants;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ds.websight.packagemanager.util.DateUtil;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import java.util.Calendar;

public class PackageActionInfoDto {

    private static final Logger LOG = LoggerFactory.getLogger(PackageActionInfoDto.class);

    private final String date;
    private final String executedBy;

    private PackageActionInfoDto(String date, String executedBy) {
        this.date = date;
        this.executedBy = executedBy;
    }

    public String getDate() {
        return date;
    }

    public String getExecutedBy() {
        return executedBy;
    }

    static PackageActionInfoDto create(JcrPackageDefinition packageDefinition, String actionPropName) {
        if (packageDefinition != null) {
            Calendar actionDate = packageDefinition.getCalendar(actionPropName);
            if (actionDate != null) {
                String date = DateUtil.format(actionDate);
                String executedBy = packageDefinition.get(actionPropName + "By");
                return new PackageActionInfoDto(date, executedBy);
            }
        }
        return null;
    }

    static PackageActionInfoDto createUploadPackageInfo(Node packageNode) {
        if (packageNode == null) {
            return null;
        }
        try {
            LOG.info("Package {} built date does not exist, using upload date", packageNode.getPath());
            if (packageNode.hasProperty(JcrConstants.JCR_CREATED)) {
                Property uploadProp = packageNode.getProperty(JcrConstants.JCR_CREATED);
                String uploadDate = DateUtil.format(uploadProp.getDate());
                return new PackageActionInfoDto(uploadDate, getUploadedBy(packageNode));
            }
        } catch (RepositoryException e) {
            LOG.warn("Could not fetch upload date", e);
        }
        return null;
    }

    private static String getUploadedBy(Node packageNode) throws RepositoryException {
        if (packageNode.hasProperty(JcrConstants.JCR_CREATED_BY)) {
            Property uploadedByProp = packageNode.getProperty(JcrConstants.JCR_CREATED_BY);
            if (!uploadedByProp.isMultiple()) {
                return PropertiesUtil.toString(uploadedByProp.getValue(), null);
            }
        }
        return null;
    }
}
