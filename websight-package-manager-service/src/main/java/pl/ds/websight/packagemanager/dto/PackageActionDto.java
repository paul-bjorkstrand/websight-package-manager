package pl.ds.websight.packagemanager.dto;

import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ds.websight.packagemanager.JobProperties;
import pl.ds.websight.packagemanager.rest.requestparameters.PackageActionCommand;
import pl.ds.websight.packagemanager.util.JobUtil;
import pl.ds.websight.packagemanager.util.PackageLogUtil;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

public class PackageActionDto {

    private static final Logger LOG = LoggerFactory.getLogger(PackageActionDto.class);

    public static final PackageActionDto UNKNOWN = new PackageActionDto(PackageActionStateDto.UNKNOWN, null, null);

    private final PackageActionStateDto state;
    private final PackageActionCommand type;

    private final String applicantId;

    private PackageActionDto(PackageActionStateDto state, PackageActionCommand type, String applicantId) {
        this.state = state;
        this.type = type;
        this.applicantId = applicantId;
    }

    public PackageActionStateDto getState() {
        return state;
    }

    public PackageActionCommand getType() {
        return type;
    }

    public String getApplicantId() {
        return applicantId;
    }

    public static PackageActionDto forPackagePath(JobManager jobManager, Session session, String packagePath) {
        return forPackagePaths(jobManager, session, packagePath).getOrDefault(packagePath, UNKNOWN);
    }

    public static Map<String, PackageActionDto> forPackagePaths(JobManager jobManager, Session session, String... packagePaths) {
        String[] existingPackagesPaths = Stream.of(packagePaths)
                .filter(path -> nodeExists(session, path))
                .toArray(String[]::new);
        Map<String, Job> jobs = JobUtil.findRunningAndQueuedJobs(jobManager, existingPackagesPaths).stream()
                .collect(toMap(JobProperties::getPackagePath, job -> job, (job1, job2) -> job1));
        Map<String, PackageActionDto> result = new HashMap<>(existingPackagesPaths.length);
        for (String packagePath : existingPackagesPaths) {
            result.put(packagePath, toFinishOrRunningPackageActionDto(jobs.get(packagePath), packagePath, session));
        }
        return result;
    }

    private static boolean nodeExists(Session session, String path) {
        try {
            return session.nodeExists(path);
        } catch (RepositoryException e) {
            LOG.debug("Could not check if node exists", e);
            return false;
        }
    }

    private static PackageActionDto toFinishOrRunningPackageActionDto(Job job, String packagePath, Session session) {
        return job == null ?
                processFinishedAction(packagePath, session) :
                Optional.of(job)
                        .map(Job::getTopic)
                        .map(PackageActionCommand::fetchByJobTopic)
                        .map(actionType -> new PackageActionDto(PackageActionStateDto.getState(job.getJobState()), actionType,
                                JobProperties.getApplicantId(job)))
                        .orElse(UNKNOWN);
    }

    private static PackageActionDto processFinishedAction(String packagePath, Session session) {
        try {
            if (!session.nodeExists(packagePath)) {
                return UNKNOWN;
            }
            String logPath = PackageLogUtil.getLogPath(packagePath);
            if (!session.nodeExists(logPath)) {
                return UNKNOWN;
            }
            Node logNode = session.getNode(logPath);
            if (!logNode.hasProperty(PackageLogUtil.PN_LOG_PACKAGE_ACTION_TYPE)) {
                return UNKNOWN;
            }
            Property actionTypeProp = logNode.getProperty(PackageLogUtil.PN_LOG_PACKAGE_ACTION_TYPE);
            if (actionTypeProp.isMultiple() || !logNode.hasProperty(PackageLogUtil.PN_APPLICANT_ID)) {
                return UNKNOWN;
            }
            PackageActionCommand actionType = getActionCommandIgnoreCase(PropertiesUtil.toString(actionTypeProp.getValue(), null));
            return actionType != null ?
                    new PackageActionDto(PackageActionStateDto.FINISHED, actionType,
                            logNode.getProperty(PackageLogUtil.PN_APPLICANT_ID).getString()) :
                    UNKNOWN;
        } catch (RepositoryException e) {
            LOG.debug("Could not get package action", e);
            return UNKNOWN;
        }
    }

    private static PackageActionCommand getActionCommandIgnoreCase(String enumName) {
        return enumName != null ?
                Arrays.stream(PackageActionCommand.values())
                        .filter(command -> command.name().equalsIgnoreCase(enumName))
                        .findFirst()
                        .orElse(null) :
                null;
    }
}
