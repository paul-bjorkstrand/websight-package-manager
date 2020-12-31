package pl.ds.websight.packagemanager.dto;

import pl.ds.websight.packagemanager.util.DateUtil;

import java.util.Calendar;
import java.util.List;

public class ScheduledPackageActionsDto {

    private final String currentSystemDateTime;
    private final List<PackageScheduleActionInfoDto> scheduledPackageActions;

    public ScheduledPackageActionsDto(List<PackageScheduleActionInfoDto> scheduledPackageActions) {
        this.scheduledPackageActions = scheduledPackageActions;
        currentSystemDateTime = DateUtil.format(Calendar.getInstance());
    }

    public String getCurrentSystemDateTime() {
        return currentSystemDateTime;
    }

    public List<PackageScheduleActionInfoDto> getScheduledPackageActions() {
        return scheduledPackageActions;
    }
}
