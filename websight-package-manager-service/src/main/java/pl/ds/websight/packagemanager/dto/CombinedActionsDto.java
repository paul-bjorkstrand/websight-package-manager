package pl.ds.websight.packagemanager.dto;

public class CombinedActionsDto {

    private final String path;
    private final PackageActionStateDto state;

    public CombinedActionsDto(String path, PackageActionStateDto state) {
        this.path = path;
        this.state = state;
    }

    public String getPath() {
        return path;
    }

    public PackageActionStateDto getState() {
        return state;
    }
}
