package pl.ds.websight.packagemanager.dto;

import org.apache.sling.event.jobs.Job;

import java.util.Arrays;
import java.util.function.Predicate;

public enum PackageActionStateDto {

    QUEUED(Job.JobState.QUEUED::equals),
    RUNNING(Job.JobState.ACTIVE::equals),
    FINISHED(jobState -> !Job.JobState.QUEUED.equals(jobState) && !Job.JobState.ACTIVE.equals(jobState)),

    /**
     * Handles cases when package doesn't exist or logs could not be found, so state of possible actions is unknown.
     */
    UNKNOWN(jobState -> false);

    private final Predicate<Job.JobState> valueByJobStatePredicate;

    PackageActionStateDto(Predicate<Job.JobState> valueByJobStatePredicate) {
        this.valueByJobStatePredicate = valueByJobStatePredicate;
    }

    public static PackageActionStateDto getState(Job.JobState jobState) {
        return getState(actionStateDto -> actionStateDto.valueByJobStatePredicate.test(jobState));
    }

    public static PackageActionStateDto getState(String name) {
        return getState(actionStateDto -> actionStateDto.toString().equalsIgnoreCase(name));
    }

    private static PackageActionStateDto getState(Predicate<PackageActionStateDto> matchPredicate) {
        return Arrays.stream(PackageActionStateDto.values())
                .filter(matchPredicate)
                .findFirst()
                .orElse(UNKNOWN);
    }
}
