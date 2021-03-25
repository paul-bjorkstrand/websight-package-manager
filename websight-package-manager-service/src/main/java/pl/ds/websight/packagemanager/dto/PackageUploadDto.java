package pl.ds.websight.packagemanager.dto;

public class PackageUploadDto {

    private final String path;

    public PackageUploadDto(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
