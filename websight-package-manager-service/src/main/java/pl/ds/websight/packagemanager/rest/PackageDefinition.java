package pl.ds.websight.packagemanager.rest;

import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.Calendar;
import java.util.Objects;
import java.util.stream.Stream;

public class PackageDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(PackageDefinition.class);

    private final Node packageNode;
    private Calendar created;
    private Calendar lastModified;
    private Calendar lastUnpacked;
    private Calendar lastUnwrapped;
    private Calendar lastWrapped;
    private String name;

    public PackageDefinition(Node packageNode) {
        this.packageNode = packageNode;
        try {
            name = packageNode.getName();
            if (packageNode.hasNode(JcrConstants.JCR_CONTENT)) {
                Node content = packageNode.getNode(JcrConstants.JCR_CONTENT);
                if (content.hasNode(JcrPackage.NN_VLT_DEFINITION)) {
                    Node definition = content.getNode(JcrPackage.NN_VLT_DEFINITION);
                    created = getCalendarProperty(definition, JcrConstants.JCR_CREATED);
                    lastModified = getCalendarProperty(definition, JcrConstants.JCR_LASTMODIFIED);
                    lastUnpacked = getCalendarProperty(definition, JcrPackageDefinition.PN_LAST_UNPACKED);
                    lastUnwrapped = getCalendarProperty(definition, JcrPackageDefinition.PN_LAST_UNWRAPPED);
                    lastWrapped = getCalendarProperty(definition, JcrPackageDefinition.PN_LAST_WRAPPED);
                }
            }
        } catch (RepositoryException e) {
            LOG.warn("Could not get property of package Node", e);
        }
    }

    private Calendar getCalendarProperty(Node node, String name) throws RepositoryException {
        if (node.hasProperty(name)) {
            return node.getProperty(name).getDate();
        }
        return null;
    }

    public Calendar getLastModified() {
        return lastModified;
    }

    public Calendar getLastUnpacked() {
        return lastUnpacked;
    }

    public Calendar getLastUnwrapped() {
        return lastUnwrapped;
    }

    public String getName() {
        return name;
    }

    public Node getPackageNode() {
        return packageNode;
    }

    public Calendar getLatestActionDate() {
        return Stream.of(created, lastModified, lastUnpacked, lastUnwrapped, lastWrapped)
                .filter(Objects::nonNull)
                .max(Calendar::compareTo)
                .orElse(null);
    }

}
