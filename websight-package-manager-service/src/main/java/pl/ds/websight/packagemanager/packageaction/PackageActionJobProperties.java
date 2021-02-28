package pl.ds.websight.packagemanager.packageaction;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.event.jobs.Job;
import pl.ds.websight.packagemanager.JobProperties;
import pl.ds.websight.packagemanager.packageoptions.PackageImportOptions;
import pl.ds.websight.packagemanager.util.PackageLogUtil;

import java.util.Map;

public class PackageActionJobProperties {

    private static final String LOG_PATH_PROPS_PARAM = "logPath";
    private static final String PACKAGE_IMPORT_OPTIONS_PROPS_PARAM = "packageImportOptions";

    private final String logPath;
    private final PackageImportOptions packageImportOptions;
    private final JobProperties jobProperties;

    private PackageActionJobProperties(String logPath, PackageImportOptions packageImportOptions, JobProperties jobProperties) {
        this.logPath = logPath;
        this.packageImportOptions = packageImportOptions;
        this.jobProperties = jobProperties;
    }

    public static PackageActionJobProperties fetch(Job job) {
        JobProperties jobProperties = JobProperties.fetch(job);
        String logNodePath = job.getProperty(LOG_PATH_PROPS_PARAM, String.class);
        PackageImportOptions packageImportOptions = job.getProperty(PACKAGE_IMPORT_OPTIONS_PROPS_PARAM, PackageImportOptions.class);
        return getValidJobProperties(jobProperties, logNodePath, packageImportOptions);
    }

    private static PackageActionJobProperties getValidJobProperties(JobProperties jobProperties, String logNodePath, PackageImportOptions packageImportOptions) {
        return StringUtils.isNotBlank(logNodePath) && jobProperties != null ?
                new PackageActionJobProperties(logNodePath, packageImportOptions, jobProperties) :
                null;
    }

    public String getLogPath() {
        return logPath;
    }

    public PackageImportOptions getPackageImportOptions() {
        return packageImportOptions;
    }

    public String getApplicantId() {
        return jobProperties.getApplicantId();
    }

    public String getPackageReference() {
        return jobProperties.getPackageReference();
    }

    public static Map<String, Object> toMap(String packagePath, PackageImportOptions packageImportOptions, String userID) {
        Map<String, Object> jobProperties = JobProperties.toMap(packagePath, userID);
        jobProperties.put(LOG_PATH_PROPS_PARAM, PackageLogUtil.getLogPath(packagePath));
        jobProperties.put(PACKAGE_IMPORT_OPTIONS_PROPS_PARAM, packageImportOptions);
        return jobProperties;
    }

    public static Map<String, Object> asQueryMap(String packagePath) {
        Map<String, Object> jobQueryProperties = JobProperties.asQueryMap(packagePath);
        jobQueryProperties.put(LOG_PATH_PROPS_PARAM, PackageLogUtil.getLogPath(packagePath));
        return jobQueryProperties;
    }
}
