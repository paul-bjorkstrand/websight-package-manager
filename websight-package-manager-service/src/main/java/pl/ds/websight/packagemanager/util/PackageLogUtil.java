package pl.ds.websight.packagemanager.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static org.apache.jackrabbit.vault.util.JcrConstants.JCR_PRIMARYTYPE;
import static org.apache.jackrabbit.vault.util.JcrConstants.NT_UNSTRUCTURED;
import static org.apache.sling.jcr.resource.api.JcrResourceConstants.NT_SLING_FOLDER;
import static pl.ds.websight.packagemanager.util.JcrPackageUtil.PACKAGES_ROOT_PATH;

public final class PackageLogUtil {

    private static final Logger LOG = LoggerFactory.getLogger(PackageLogUtil.class);

    public static final String PN_LOG_ENTRY_PREFIX = "entries";
    public static final int MAX_ENTRY_CAPACITY = 1000;
    public static final String PN_FIRST_LOG_ENTRY = PN_LOG_ENTRY_PREFIX + "0-" + MAX_ENTRY_CAPACITY;
    public static final String PN_APPLICANT_ID = "applicantId";
    public static final String PN_LOG_AUTHOR_SIGN = "logAuthorId";
    public static final String PN_LOG_PACKAGE_ACTION_TYPE = "logPackageActionType";
    public static final String RT_LOG_INTERMEDIATE_NODES = NT_SLING_FOLDER;
    public static final String LOG_PATH_PREFIX = "/var/websight/websight-package-manager-service/logs/"; //NOSONAR

    private PackageLogUtil() {
        // no instance
    }

    public static String increaseRange(String range) {
        ImmutablePair<Long, Long> rangeLimits = getRangeLimits(range);
        long lowerLimit = rangeLimits.getLeft();
        long upperLimit = rangeLimits.getRight();
        lowerLimit += lowerLimit == 0 ? MAX_ENTRY_CAPACITY + 1 : MAX_ENTRY_CAPACITY;
        upperLimit += MAX_ENTRY_CAPACITY;
        return String.format("%d-%d", lowerLimit, upperLimit);
    }

    public static ImmutablePair<Long, Long> getRangeLimits(String range) {
        String[] limits = StringUtils.split(range, "-", 2);
        if (limits.length != 2) {
            return getPreIncrementedLimits();
        }
        try {
            return ImmutablePair.of(NumberUtils.createLong(limits[0]), NumberUtils.createLong(limits[1]));
        } catch (NumberFormatException e) {
            LOG.debug("Could not get limits of log range", e);
            return getPreIncrementedLimits();
        }
    }

    private static ImmutablePair<Long, Long> getPreIncrementedLimits() {
        return ImmutablePair.of(Math.negateExact(MAX_ENTRY_CAPACITY + 1L), 0L);
    }

    public static String removeLogEntryNamePrefix(String propertyName) {
        return propertyName.substring(PN_LOG_ENTRY_PREFIX.length());
    }

    public static void putLog(ModifiableValueMap logValueMap, String range, List<String> entry) {
        if (entry.isEmpty()) {
            return;
        }
        String mergedLogEntry = entry.stream()
                .map(logLine -> StringUtils.appendIfMissing(logLine, "\n"))
                .reduce("", String::concat);
        logValueMap.put(PN_LOG_ENTRY_PREFIX + range, mergedLogEntry);
    }

    public static boolean isLogsAuthorDifferent(String newAuthorJobId, ValueMap valueMap) {
        return !newAuthorJobId.equals(valueMap.get(PN_LOG_AUTHOR_SIGN, String.class));
    }

    public static List<String> getLogEntry(ValueMap logValueMap, String logEntryName) {
        return splitLogEntry(logValueMap.get(logEntryName, String.class));
    }

    public static List<String> getLog(Node logNode, boolean fullLog) throws RepositoryException {
        if (!logNode.hasProperty(PN_FIRST_LOG_ENTRY)) {
            return Collections.emptyList();
        }
        PropertyIterator properties = logNode.getProperties();
        return fullLog ?
                getFullLog(properties) :
                getLogTail(properties, logNode.getProperty(PN_FIRST_LOG_ENTRY));
    }

    private static List<String> getFullLog(PropertyIterator properties) throws RepositoryException {
        Map<Long, List<String>> logEntriesMap = new TreeMap<>();
        while (properties.hasNext()) {
            Property property = properties.nextProperty();
            if (isLogEntry(property.getName())) {
                List<String> logEntry = getLogEntry(property);
                if (logEntry != null) {
                    logEntriesMap.put(getLowerLimit(property), logEntry);
                }
            }
        }
        return logEntriesMap.values().stream()
                .flatMap(Collection::stream)
                .collect(toList());
    }

    private static List<String> getLogTail(PropertyIterator properties, Property firstLogNode) throws RepositoryException {
        Property logTailPredecessor = null;
        Property logTailProp = firstLogNode;
        while (properties.hasNext()) {
            Property property = properties.nextProperty();
            if (isLogEntry(property.getName()) && getLowerLimit(property) > getLowerLimit(logTailProp)) {
                logTailPredecessor = logTailProp;
                logTailProp = property;
            }
        }
        return getMaxFilledLogTail(logTailPredecessor, logTailProp);
    }

    public static boolean isLogEntry(String propName) {
        return propName.startsWith(PN_LOG_ENTRY_PREFIX);
    }

    private static long getLowerLimit(Property logProperty) throws RepositoryException {
        return Optional.of(logProperty.getName())
                .map(PackageLogUtil::removeLogEntryNamePrefix)
                .map(range -> range.split("-"))
                .filter(limits -> limits.length == 2)
                .map(limits -> limits[0])
                .map(lowerLimit -> {
                    try {
                        return Long.parseUnsignedLong(lowerLimit);
                    } catch (NumberFormatException e) {
                        LOG.warn("Could not get lower limit of one of log entries", e);
                        return 0L;
                    }
                })
                .orElse(0L);
    }

    private static List<String> getMaxFilledLogTail(Property logTailPredecessor, Property logTailProp) throws RepositoryException {
        List<String> logTail = getLogEntry(logTailProp);
        if (logTailPredecessor != null && logTail != null && logTail.size() < MAX_ENTRY_CAPACITY) {
            return Optional.ofNullable(getLogEntry(logTailPredecessor))
                    .filter(predecessorEntry -> predecessorEntry.size() == MAX_ENTRY_CAPACITY)
                    .map(predecessorEntry -> predecessorEntry.subList(logTail.size(), MAX_ENTRY_CAPACITY))
                    .map(Collection::stream)
                    .map(extendedLogsStream -> Stream.concat(extendedLogsStream, logTail.stream()))
                    .orElseGet(logTail::stream)
                    .collect(toCollection(ArrayList::new));
        }
        return logTail;
    }

    private static List<String> getLogEntry(Property logProp) throws RepositoryException {
        return splitLogEntry(PropertiesUtil.toString(logProp.getValue(), null));
    }

    public static List<String> splitLogEntry(String mergedLogEntry) {
        return mergedLogEntry != null ? new ArrayList<>(Arrays.asList(mergedLogEntry.split("(?<=\n)"))) : null;
    }

    public static ModifiableValueMap getOrCreatePackageLogMap(ResourceResolver resolver, String destinationPath) {
        try {
            Resource actionLog = ResourceUtil.getOrCreateResource(resolver, destinationPath,
                    Collections.singletonMap(JCR_PRIMARYTYPE, NT_UNSTRUCTURED), RT_LOG_INTERMEDIATE_NODES, false);
            return actionLog.adaptTo(ModifiableValueMap.class);
        } catch (PersistenceException e) {
            LOG.warn("Could not access action log resource in: {}", destinationPath, e);
            return null;
        }
    }

    public static void updateActionInfo(String logActionType, String applicantId, ModifiableValueMap logValueMap) {
        logValueMap.put(PN_LOG_PACKAGE_ACTION_TYPE, logActionType);
        logValueMap.put(PN_APPLICANT_ID, applicantId);
    }

    public static String getLogPath(String packagePath) {
        return LOG_PATH_PREFIX + StringUtils.removeStart(packagePath, PACKAGES_ROOT_PATH);
    }
}
