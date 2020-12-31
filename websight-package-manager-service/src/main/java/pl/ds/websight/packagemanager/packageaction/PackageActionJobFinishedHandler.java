package pl.ds.websight.packagemanager.packageaction;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ds.websight.packagemanager.rest.requestparameters.PackageActionCommand;
import pl.ds.websight.packagemanager.util.JobUtil;
import pl.ds.websight.packagemanager.util.PackageLogUtil;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static pl.ds.websight.packagemanager.util.PackageLogUtil.MAX_ENTRY_CAPACITY;
import static pl.ds.websight.packagemanager.util.PackageLogUtil.PN_FIRST_LOG_ENTRY;
import static pl.ds.websight.packagemanager.util.PackageLogUtil.PN_LOG_AUTHOR_SIGN;
import static pl.ds.websight.packagemanager.util.PackageLogUtil.PN_LOG_ENTRY_PREFIX;

public final class PackageActionJobFinishedHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PackageActionJobFinishedHandler.class);

    private final ResourceResolverFactory resolverFactory;
    private final PackageActionJobProperties properties;
    private final String jobId;
    private final String actionType;

    public PackageActionJobFinishedHandler(ResourceResolverFactory resolverFactory, PackageActionJobProperties properties, String jobId,
            PackageActionCommand packageActionCommand) {
        this.resolverFactory = resolverFactory;
        this.properties = properties;
        this.jobId = jobId;
        this.actionType = packageActionCommand.toString();
    }

    public void handleFinish(String finishMessage) {
        handleFinish(finishMessage, null);
    }

    public void handleFinish(String finishMessage, String exceptionStacktrace) {
        String applicantId = properties.getApplicantId();
        try (ResourceResolver applicantResolver = JobUtil.getImpersonatedResolver(resolverFactory, applicantId)) {
            ModifiableValueMap logValueMap = Optional.ofNullable(applicantResolver)
                    .map(resolver -> PackageLogUtil.getOrCreatePackageLogMap(resolver, properties.getLogPath()))
                    .orElse(null);
            if (logValueMap == null) {
                LOG.warn("Could not access basic action log objects for user {} and path {}",
                        applicantId, properties.getPackageReference());
                return;
            }
            PackageLogUtil.updateActionInfo(actionType, applicantId, logValueMap);
            List<String> footerEntries = getFooterEntries(finishMessage, exceptionStacktrace);
            if (exceptionStacktrace != null && PackageLogUtil.isLogsAuthorDifferent(jobId, logValueMap)) {
                removeAllLogs(logValueMap);
                logValueMap.put(PN_LOG_AUTHOR_SIGN, jobId);
                saveCompletedLogs(logValueMap, PN_FIRST_LOG_ENTRY, footerEntries);
            } else {
                appendPackageLogFooter(logValueMap, footerEntries);
            }
            applicantResolver.commit();
        } catch (PersistenceException e) {
            LOG.warn("Could not insert finish info", e);
        }
    }

    private static List<String> getFooterEntries(String finishMessage, String stacktrace) {
        List<String> entries = new LinkedList<>();
        List<String> stacktraceList = PackageLogUtil.splitLogEntry(stacktrace);
        if (stacktraceList != null) {
            entries.addAll(stacktraceList);
        }
        entries.add(""); // add one empty line between package action logs and finish info
        entries.add(finishMessage);
        return entries;
    }

    private static void removeAllLogs(ValueMap valueMap) {
        valueMap.keySet().stream()
                .filter(PackageLogUtil::isLogEntry)
                .collect(toList())
                .forEach(valueMap::remove);
    }

    private static void appendPackageLogFooter(ModifiableValueMap logValueMap, List<String> footerEntries) {
        String lastLogEntryName = getLastLogEntryName(logValueMap);
        if (lastLogEntryName == null) {
            LOG.warn("No log has been specified, skipped finish info addition");
            return;
        }
        List<String> lastLogEntries = PackageLogUtil.getLogEntry(logValueMap, lastLogEntryName);
        if (lastLogEntries != null) {
            lastLogEntries.addAll(footerEntries);
            saveCompletedLogs(logValueMap, lastLogEntryName, lastLogEntries);
        }
    }

    private static String getLastLogEntryName(ValueMap valueMap) {
        return valueMap.keySet().stream()
                .filter(PackageLogUtil::isLogEntry)
                .max(Comparator.comparing(logProp -> {
                    String range = logProp.substring(PN_LOG_ENTRY_PREFIX.length());
                    return PackageLogUtil.getRangeLimits(range).getLeft();
                }))
                .orElse(null);
    }

    private static void saveCompletedLogs(ModifiableValueMap logValueMap, String lastLogEntryName, List<String> updatedLogs) {
        String range = PackageLogUtil.removeLogEntryNamePrefix(lastLogEntryName);
        while (updatedLogs.size() >= MAX_ENTRY_CAPACITY) {
            List<String> fullEntry = updatedLogs.subList(0, MAX_ENTRY_CAPACITY);
            PackageLogUtil.putLog(logValueMap, range, fullEntry);
            range = PackageLogUtil.increaseRange(range);
            updatedLogs.removeAll(fullEntry);
        }
        PackageLogUtil.putLog(logValueMap, range, updatedLogs);
        deleteLastCarriageReturn(logValueMap, range);
    }

    private static void deleteLastCarriageReturn(ModifiableValueMap logValueMap, String range) {
        String lastLineWithoutCarriageReturn = StringUtils.removeEnd(logValueMap.get(PN_LOG_ENTRY_PREFIX + range, String.class), "\n");
        logValueMap.put(PN_LOG_ENTRY_PREFIX + range, lastLineWithoutCarriageReturn);
    }
}
