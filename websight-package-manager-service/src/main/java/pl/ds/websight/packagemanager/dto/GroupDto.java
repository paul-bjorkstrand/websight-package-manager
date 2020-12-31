package pl.ds.websight.packagemanager.dto;

public class GroupDto {

    private final String name;
    private final long count;
    private final boolean limitExceeded;

    public GroupDto(String name, long count, long limit) {
        this.name = name;
        if (count > limit) {
            this.count = limit;
            this.limitExceeded = true;
        } else {
            this.count = count;
            this.limitExceeded = false;
        }
    }

    public String getName() {
        return name;
    }

    public long getCount() {
        return count;
    }

    public boolean isLimitExceeded() {
        return limitExceeded;
    }

}