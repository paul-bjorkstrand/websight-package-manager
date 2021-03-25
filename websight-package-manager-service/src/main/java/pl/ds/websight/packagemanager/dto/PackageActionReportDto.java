package pl.ds.websight.packagemanager.dto;

import java.util.List;

public class PackageActionReportDto {

    private final PackageActionDto action;
    private final List<String> logs;

    public PackageActionReportDto(PackageActionDto action) {
        this(action, null);
    }

    public PackageActionReportDto(PackageActionDto action, List<String> logs) {
        this.action = action;
        this.logs = logs;
    }

    public PackageActionDto getAction() {
        return action;
    }

    public List<String> getLogs() {
        return logs;
    }
}
