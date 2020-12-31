package pl.ds.websight.packagemanager.dto;

import org.apache.jackrabbit.vault.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ds.websight.packagemanager.util.JcrPackageUtil;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import java.util.Calendar;


public class PackageThumbnailDto {

    private static final Logger LOG = LoggerFactory.getLogger(PackageThumbnailDto.class);

    private final String path;

    private final long timestamp;

    private PackageThumbnailDto(String path, long timestamp) {
        this.path = path;
        this.timestamp = timestamp;
    }

    public static PackageThumbnailDto forPackagePath(String packagePath, Session session) {
        try {
            String packageThumbnailPath = packagePath + JcrPackageUtil.THUMBNAIL_REL_PATH;
            if (!session.nodeExists(packageThumbnailPath)) {
                return null;
            }
            Node packageThumbnailNode = session.getNode(packageThumbnailPath);
            if (!packageThumbnailNode.hasNode(JcrConstants.JCR_CONTENT)) {
                return null;
            }
            Node thumbnailContentNode = packageThumbnailNode.getNode(JcrConstants.JCR_CONTENT);
            if (!thumbnailContentNode.isNodeType(NodeType.NT_RESOURCE)) {
                return null;
            }
            Calendar lastModificationDate = thumbnailContentNode.hasProperty(JcrConstants.JCR_LASTMODIFIED) ?
                    thumbnailContentNode.getProperty(JcrConstants.JCR_LASTMODIFIED).getDate() : null;
            return lastModificationDate != null ?
                    new PackageThumbnailDto(packageThumbnailPath, lastModificationDate.getTimeInMillis()) :
                    null;
        } catch (RepositoryException e) {
            LOG.warn("Could not check package thumbnail", e);
            return null;
        }
    }

    public String getPath() {
        return path;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
