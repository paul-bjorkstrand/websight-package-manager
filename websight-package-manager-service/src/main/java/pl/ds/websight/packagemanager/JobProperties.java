package pl.ds.websight.packagemanager;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.ScheduledJobInfo;

import java.util.HashMap;
import java.util.Map;

public class JobProperties {

    private static final String APPLICANT_ID_PROPS_PARAM = "userId";
    private static final String PACKAGE_REF_PROPS_PARAM = "reference";

    private final String packageReference;
    private final String applicantId;

    private JobProperties(String packageReference, String applicantId) {
        this.packageReference = packageReference;
        this.applicantId = applicantId;
    }

    public String getPackageReference() {
        return packageReference;
    }

    public String getApplicantId() {
        return applicantId;
    }

    public static JobProperties fetch(Job job) {
        String packageReference = job.getProperty(PACKAGE_REF_PROPS_PARAM, String.class);
        String applicantId = job.getProperty(APPLICANT_ID_PROPS_PARAM, String.class);
        return StringUtils.isNotBlank(packageReference) && StringUtils.isNotBlank(applicantId) ? new JobProperties(packageReference,
                applicantId) : null;
    }

    public static JobProperties fetch(ScheduledJobInfo scheduledInfo) {
        Map<String, Object> jobProperties = scheduledInfo.getJobProperties();
        String packageReference = jobProperties.containsKey(JobProperties.PACKAGE_REF_PROPS_PARAM) ?
                jobProperties.get(JobProperties.PACKAGE_REF_PROPS_PARAM).toString() :
                null;
        String applicantId = jobProperties.containsKey(JobProperties.APPLICANT_ID_PROPS_PARAM) ?
                jobProperties.get(JobProperties.APPLICANT_ID_PROPS_PARAM).toString() :
                null;
        return StringUtils.isNotBlank(packageReference) && StringUtils.isNotBlank(applicantId) ? new JobProperties(packageReference,
                applicantId) : null;
    }

    public static Map<String, Object> toMap(String packagePath, String userID) {
        Map<String, Object> jobProperties = new HashMap<>();
        jobProperties.put(PACKAGE_REF_PROPS_PARAM, packagePath);
        jobProperties.put(APPLICANT_ID_PROPS_PARAM, userID);
        return jobProperties;
    }

    public static Map<String, Object> asQueryMap(String packagePath) {
        Map<String, Object> jobQueryProperties = new HashMap<>();
        jobQueryProperties.put(PACKAGE_REF_PROPS_PARAM, packagePath);
        return jobQueryProperties;
    }

    public static String getApplicantId(Job job) {
        return job.getProperty(JobProperties.APPLICANT_ID_PROPS_PARAM, String.class);
    }

    public static String getApplicantId(ScheduledJobInfo scheduledJobInfo) {
        return getStringProp(scheduledJobInfo, JobProperties.APPLICANT_ID_PROPS_PARAM);
    }

    public static String getPackagePath(Job job) {
        return job.getProperty(JobProperties.PACKAGE_REF_PROPS_PARAM, String.class);
    }

    public static String getPackagePath(ScheduledJobInfo scheduledJobInfo) {
        return getStringProp(scheduledJobInfo, JobProperties.PACKAGE_REF_PROPS_PARAM);
    }

    private static String getStringProp(ScheduledJobInfo scheduledJobInfo, String propName) {
        Map<String, Object> jobProperties = scheduledJobInfo.getJobProperties();
        return jobProperties.containsKey(propName) ? jobProperties.get(propName).toString() : null;
    }
}
