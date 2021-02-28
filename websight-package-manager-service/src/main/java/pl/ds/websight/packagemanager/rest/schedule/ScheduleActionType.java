package pl.ds.websight.packagemanager.rest.schedule;

import pl.ds.websight.packagemanager.DeletePackageScheduleJobConsumer;
import pl.ds.websight.packagemanager.JobProperties;
import pl.ds.websight.packagemanager.packageaction.PackageActionJobConsumer;
import pl.ds.websight.packagemanager.packageaction.PackageActionJobProperties;
import pl.ds.websight.packagemanager.packageoptions.PackageImportOptions;
import pl.ds.websight.packagemanager.rest.PackagePrerequisiteValidator;
import pl.ds.websight.packagemanager.rest.packageaction.InstallPackageRestAction;
import pl.ds.websight.packagemanager.rest.packageaction.UninstallPackageRestAction;

import java.util.Arrays;
import java.util.Map;

public enum ScheduleActionType {

    INSTALL(
            "Installation",
            PackageActionJobConsumer.INSTALL_TOPIC,
            (packagePath, userId) -> PackageActionJobProperties.toMap(packagePath, PackageImportOptions.DEFAULT, userId),
            InstallPackageRestAction.ACTION_PRE_VALIDATORS),

    BUILD(
            "Build",
            PackageActionJobConsumer.BUILD_TOPIC,
            (packagePath, userId) -> PackageActionJobProperties.toMap(packagePath,  PackageImportOptions.DEFAULT, userId)),

    UNINSTALL(
            "Uninstallation",
            PackageActionJobConsumer.UNINSTALL_TOPIC,
            (packagePath, userId) -> PackageActionJobProperties.toMap(packagePath,  PackageImportOptions.DEFAULT, userId),
            UninstallPackageRestAction.ACTION_PRE_VALIDATOR),

    DELETE(
            "Delete",
            DeletePackageScheduleJobConsumer.TOPIC,
            JobProperties::toMap);

    private final String fullName;
    private final String jobTopic;
    private final JobPropertiesCreator creator;
    private final PackagePrerequisiteValidator[] validators;

    ScheduleActionType(String fullName, String jobTopic, JobPropertiesCreator creator, PackagePrerequisiteValidator... validators) {
        this.fullName = fullName;
        this.jobTopic = jobTopic;
        this.creator = creator;
        this.validators = validators;
    }

    public String getFullName() {
        return fullName;
    }

    public String getJobTopic() {
        return jobTopic;
    }

    public Map<String, Object> getJobProperties(String packagePath, String userId) {
        return creator.create(packagePath, userId);
    }

    public PackagePrerequisiteValidator[] getValidators() {
        return validators;
    }

    public static ScheduleActionType from(String jobTopic) {
        return Arrays.stream(ScheduleActionType.values())
                .filter(actionType -> actionType.jobTopic.equals(jobTopic))
                .findFirst()
                .orElse(null);
    }

    public static String getFullName(String jobTopic) {
        return Arrays.stream(ScheduleActionType.values())
                .filter(actionType -> actionType.jobTopic.equals(jobTopic))
                .findFirst()
                .map(actionType -> actionType.fullName)
                .orElse("Unknown action");
    }

    private interface JobPropertiesCreator {
        Map<String, Object> create(String packagePath, String userId);
    }
}
