package pl.ds.websight.packagemanager.dto;

import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.util.JcrConstants;
import pl.ds.websight.packagemanager.util.JcrPackageStatusUtil;
import pl.ds.websight.packagemanager.util.JcrPackageUtil;

public final class PackageStatusDto {

    private final PackageActionInfoDto build;
    private final PackageActionInfoDto installation;
    private final PackageActionInfoDto modification;

    private PackageStatusDto(JcrPackage jcrPackage) {
        boolean hasValidBuildDate = JcrPackageUtil.fetchDefinition(jcrPackage)
                .map(packageDefinition -> packageDefinition.getCreated() != null)
                .orElse(false);
        this.build = hasValidBuildDate ?
                getActionStatus(jcrPackage, JcrConstants.JCR_CREATED) :
                PackageActionInfoDto.createUploadPackageInfo(jcrPackage.getNode());
        this.installation = JcrPackageStatusUtil.isInstalled(jcrPackage) ?
                getActionStatus(jcrPackage, JcrPackageDefinition.PN_LAST_UNPACKED) :
                null;
        this.modification = JcrPackageStatusUtil.isModified(jcrPackage) ?
                getActionStatus(jcrPackage, JcrConstants.JCR_LASTMODIFIED) :
                null;
    }

    private static PackageActionInfoDto getActionStatus(JcrPackage jcrPackage, String actionPropName) {
        return JcrPackageUtil.fetchDefinition(jcrPackage)
                .map(jcrPackageDefinition -> PackageActionInfoDto.create(jcrPackageDefinition, actionPropName))
                .orElse(null);
    }

    public PackageActionInfoDto getBuild() {
        return build;
    }

    public PackageActionInfoDto getInstallation() {
        return installation;
    }

    public PackageActionInfoDto getModification() {
        return modification;
    }

    public static PackageStatusDto fetchPackageStatus(JcrPackage jcrPackage) {
        return JcrPackageStatusUtil.isBuilt(jcrPackage) ?
                new PackageStatusDto(jcrPackage) :
                null;
    }
}