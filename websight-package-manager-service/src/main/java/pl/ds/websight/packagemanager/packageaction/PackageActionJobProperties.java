package pl.ds.websight.packagemanager.packageaction;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.event.jobs.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ds.websight.packagemanager.JobProperties;
import pl.ds.websight.packagemanager.packageoptions.PackageImportOptions;
import pl.ds.websight.packagemanager.util.PackageLogUtil;

import java.io.IOException;
import java.util.Map;

public class PackageActionJobProperties {

    private static final Logger LOG = LoggerFactory.getLogger(PackageActionJobProperties.class);
    private static final String LOG_PATH_PROPS_PARAM = "logPath";
    private static final String PACKAGE_IMPORT_OPTIONS_PROPS_PARAM = "packageImportOptions";
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true);

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
        PackageImportOptions packageImportOptions = null;
        try {
            packageImportOptions = MAPPER.readValue(
                    job.getProperty(PACKAGE_IMPORT_OPTIONS_PROPS_PARAM, String.class), PackageImportOptions.class);
        } catch (IOException e) {
            LOG.warn("Unable to read package import options value: " + job.getProperty(PACKAGE_IMPORT_OPTIONS_PROPS_PARAM), e);
        }
        return getValidJobProperties(jobProperties, logNodePath, packageImportOptions);
    }

    private static PackageActionJobProperties getValidJobProperties(JobProperties jobProperties, String logNodePath, PackageImportOptions packageImportOptions) {
        return StringUtils.isNotBlank(logNodePath) && jobProperties != null ?
                new PackageActionJobProperties(logNodePath, packageImportOptions, jobProperties) :
                null;
    }

    public static Map<String, Object> toMap(String packagePath, PackageImportOptions packageImportOptions, String userID) {
        Map<String, Object> jobProperties = JobProperties.toMap(packagePath, userID);
        jobProperties.put(LOG_PATH_PROPS_PARAM, PackageLogUtil.getLogPath(packagePath));
        try {
            jobProperties.put(PACKAGE_IMPORT_OPTIONS_PROPS_PARAM, MAPPER.writeValueAsString(packageImportOptions));
        } catch (JsonProcessingException e) {
            LOG.warn("Unable to write package import options value: " + packageImportOptions, e);
        }
        return jobProperties;
    }

    public static Map<String, Object> asQueryMap(String packagePath) {
        Map<String, Object> jobQueryProperties = JobProperties.asQueryMap(packagePath);
        jobQueryProperties.put(LOG_PATH_PROPS_PARAM, PackageLogUtil.getLogPath(packagePath));
        return jobQueryProperties;
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
}
