package pl.ds.websight.packagemanager.packageaction;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.event.jobs.Job;
import pl.ds.websight.packagemanager.JobProperties;
import pl.ds.websight.packagemanager.util.PackageLogUtil;

import java.util.Map;

public class PackageActionJobProperties {

    private static final String LOG_PATH_PROPS_PARAM = "logPath";
    private static final String DRY_RUN_PROPS_PARAM = "dryRun";

    private final String logPath;
    private final boolean dryRun;
    private final JobProperties jobProperties;

    private PackageActionJobProperties(String logPath, boolean dryRun, JobProperties jobProperties) {
        this.logPath = logPath;
        this.dryRun = dryRun;
        this.jobProperties = jobProperties;
    }

    public static PackageActionJobProperties fetch(Job job) {
        JobProperties jobProperties = JobProperties.fetch(job);
        String logNodePath = job.getProperty(LOG_PATH_PROPS_PARAM, String.class);
        boolean dryRun = Boolean.TRUE.equals(job.getProperty(DRY_RUN_PROPS_PARAM, Boolean.class));
        return getValidJobProperties(jobProperties, logNodePath, dryRun);
    }

    private static PackageActionJobProperties getValidJobProperties(JobProperties jobProperties, String logNodePath, boolean dryRun) {
        return StringUtils.isNotBlank(logNodePath) && jobProperties != null ?
                new PackageActionJobProperties(logNodePath, dryRun, jobProperties) :
                null;
    }

    public String getLogPath() {
        return logPath;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public String getApplicantId() {
        return jobProperties.getApplicantId();
    }

    public String getPackageReference() {
        return jobProperties.getPackageReference();
    }

    public static Map<String, Object> toMap(String packagePath, String userID, boolean dryRun) {
        Map<String, Object> jobProperties = JobProperties.toMap(packagePath, userID);
        jobProperties.put(LOG_PATH_PROPS_PARAM, PackageLogUtil.getLogPath(packagePath));
        jobProperties.put(DRY_RUN_PROPS_PARAM, dryRun);
        return jobProperties;
    }

    public static Map<String, Object> asQueryMap(String packagePath) {
        Map<String, Object> jobQueryProperties = JobProperties.asQueryMap(packagePath);
        jobQueryProperties.put(LOG_PATH_PROPS_PARAM, PackageLogUtil.getLogPath(packagePath));
        return jobQueryProperties;
    }
}
