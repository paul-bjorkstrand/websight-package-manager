package pl.ds.websight.packagemanager.dto;

import java.util.List;

public class GroupListDto {

    private final boolean limitExceeded;
    private final long allPackagesCount;
    private final List<GroupDto> groups;

    public GroupListDto(long allPackagesCount, long limit, List<GroupDto> groups) {
        this.groups = groups;
        if (allPackagesCount > limit) {
            this.allPackagesCount = limit;
            this.limitExceeded = true;
        } else {
            this.allPackagesCount = allPackagesCount;
            this.limitExceeded = false;
        }
    }

    public boolean isLimitExceeded() {
        return limitExceeded;
    }

    public long getAllPackagesCount() {
        return allPackagesCount;
    }

    public List<GroupDto> getGroups() {
        return groups;
    }
}
