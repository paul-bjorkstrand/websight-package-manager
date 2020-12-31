package pl.ds.websight.packagemanager.rest.requestparameters;

import pl.ds.websight.packagemanager.rest.PackageDefinition;

import java.util.Comparator;

public enum SortBy {

    LAST_USED_DESC("lastUsed", Comparator.comparing(PackageDefinition::getLatestActionDate,
            Comparator.nullsLast(Comparator.reverseOrder()))),

    LAST_MODIFIED_DESC("lastModified", Comparator.comparing(PackageDefinition::getLastModified,
            Comparator.nullsLast(Comparator.reverseOrder()))),

    NAME_ASC("name", Comparator.comparing(PackageDefinition::getName, Comparator.nullsLast(Comparator.naturalOrder()))),

    INSTALLATION_DATE_DESC("installationDate", Comparator.comparing(PackageDefinition::getLastUnpacked,
            Comparator.nullsLast(Comparator.reverseOrder()))),

    RECENTLY_ADDED_DESC("recentlyAdded", Comparator.comparing(PackageDefinition::getLastUnwrapped,
            Comparator.nullsLast(Comparator.reverseOrder())));

    private final String paramName;

    private final Comparator<PackageDefinition> comparator;

    SortBy(String paramName, Comparator<PackageDefinition> comparator) {
        this.paramName = paramName;
        this.comparator = comparator;
    }

    public Comparator<PackageDefinition> getComparator() {
        return comparator;
    }

    @Override
    public String toString() {
        return paramName;
    }
}
