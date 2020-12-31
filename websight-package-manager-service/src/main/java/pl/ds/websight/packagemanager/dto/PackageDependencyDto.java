package pl.ds.websight.packagemanager.dto;

public class PackageDependencyDto {

    private final String dependency;

    private final boolean resolved;

    public PackageDependencyDto(String dependency, boolean resolved) {
        this.dependency = dependency;
        this.resolved = resolved;
    }

    public String getDependency() {
        return dependency;
    }

    public boolean isResolved() {
        return resolved;
    }
}
