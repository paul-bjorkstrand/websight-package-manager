package pl.ds.websight.packagemanager.dto;

import java.util.Collections;
import java.util.List;

public class PackageListDto {

    public static final PackageListDto EMPTY = new PackageListDto(0, false, 0, 0, 0, null, Collections.emptyList());

    private final boolean limitExceeded;
    private final long numberOfPages;
    private final long pageNumber;
    private final long numberOfFoundPackages;
    private final long packagesLimit;
    private final String group;
    private final List<PackageDto> packages;

    public PackageListDto(long numberOfFoundPackages, boolean limitExceeded, long numberOfPages, long pageNumber, long packagesLimit, String group,
                          List<PackageDto> packages) {
        this.limitExceeded = limitExceeded;
        this.numberOfPages = numberOfPages;
        this.numberOfFoundPackages = numberOfFoundPackages;
        this.pageNumber = pageNumber;
        this.packagesLimit = packagesLimit;
        this.group = group;
        this.packages = packages;
    }

    public boolean isLimitExceeded() {
        return limitExceeded;
    }

    public long getNumberOfPages() {
        return numberOfPages;
    }

    public long getNumberOfFoundPackages() {
        return numberOfFoundPackages;
    }

    public long getPageNumber() {
        return pageNumber;
    }

    public long getPackagesLimit() {
        return packagesLimit;
    }

    public String getGroup() {
        return group;
    }

    public List<PackageDto> getPackages() {
        return packages;
    }
}
