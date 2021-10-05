package pl.ds.websight.packagemanager.packageoptions;

import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;

import java.io.Serializable;

public class PackageImportOptions implements Serializable {

    public static final PackageImportOptions DEFAULT = new PackageImportOptions();
    private AccessControlHandling acHandling;
    private boolean extractSubpackages;
    private boolean dryRun;

    private PackageImportOptions() {
        this.extractSubpackages = true;
        this.dryRun = false;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public AccessControlHandling getAcHandling() {
        return acHandling;
    }

    public boolean isExtractSubpackages() {
        return extractSubpackages;
    }

    public void setAcHandling(AccessControlHandling acHandling) {
        this.acHandling = acHandling;
    }

    public void setExtractSubpackages(boolean extractSubpackages) {
        this.extractSubpackages = extractSubpackages;
    }

    @Override
    public String toString() {
        return "PackageImportOptions{" +
                "acHandling=" + acHandling +
                ", extractSubpackages=" + extractSubpackages +
                ", dryRun=" + dryRun +
                '}';
    }
}
