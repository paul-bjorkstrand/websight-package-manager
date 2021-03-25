package pl.ds.websight.packagemanager.packageaction;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.fs.api.ProgressTrackerListener;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ds.websight.packagemanager.util.PackageLogUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.apache.commons.lang3.time.DateUtils.MILLIS_PER_SECOND;
import static pl.ds.websight.packagemanager.util.PackageLogUtil.MAX_ENTRY_CAPACITY;
import static pl.ds.websight.packagemanager.util.PackageLogUtil.PN_LOG_AUTHOR_SIGN;
import static pl.ds.websight.packagemanager.util.PackageLogUtil.increaseRange;

public class PackageActionLogProgressListener implements ProgressTrackerListener {

    private static final Logger LOG = LoggerFactory.getLogger(PackageActionLogProgressListener.class);

    private final ModifiableValueMap logValueMap;
    private final ResourceResolver resolver;
    private final List<String> entries = new ArrayList<>();
    private final String jobId;

    private String[] searchPathPrefixes;
    private String[] overlayablePathPrefixes;
    private long lastCommitTimeInMillis = 0L;
    private String range = "0-" + MAX_ENTRY_CAPACITY;
    private boolean signedByJob;

    public PackageActionLogProgressListener(ModifiableValueMap logValueMap, ResourceResolver resolver, List<String> resolverSearchPaths,
            String jobId) {
        this.logValueMap = logValueMap;
        this.resolver = resolver;
        this.jobId = jobId;
        if (isOverlayPossible(resolverSearchPaths)) {
            this.searchPathPrefixes = resolverSearchPaths.stream()
                    .map(path -> StringUtils.appendIfMissing(path, "/"))
                    .distinct()
                    .toArray(String[]::new);
            ArrayUtils.reverse(searchPathPrefixes);
            this.overlayablePathPrefixes = ArrayUtils.remove(searchPathPrefixes, searchPathPrefixes.length - 1);// because the last one
            // could not be overlaid by any path
        }
    }

    private static boolean isOverlayPossible(List<String> resolverSearchPaths) {
        return resolverSearchPaths.size() > 1;
    }

    @Override
    public void onMessage(Mode mode, String action, String path) {
        processLog(getInfoLogMessages(action, path));
    }

    private List<String> getInfoLogMessages(String action, String path) {
        if (StringUtils.isBlank(path)) {
            return Collections.singletonList(action);
        }
        List<String> logMessages = new ArrayList<>();
        logMessages.add(String.format("%s %s", action, path));
        if (searchPathPrefixes != null && overlayablePathPrefixes.length > 0) {
            validateOverlay(path, logMessages);
        }
        return logMessages;
    }

    private void validateOverlay(String path, List<String> logMessages) {
        Arrays.stream(overlayablePathPrefixes)
                .filter(path::startsWith)
                .map(overlayablePathCandidate -> detectOverlay(overlayablePathCandidate, path, searchPathPrefixes, resolver))
                .filter(Objects::nonNull)
                .findFirst()
                .ifPresent(overlayPath ->
                        logMessages.add("Potential ResourceResolver overlay found: " + overlayPath + " for processed path: " + path));
    }

    private static String detectOverlay(String overlayPathPrefix, String sourcePath, String[] searchPathPrefixes,
            ResourceResolver resolver) {
        String[] possibleOverlays = ArrayUtils.subarray(searchPathPrefixes, ArrayUtils.indexOf(searchPathPrefixes, overlayPathPrefix) + 1,
                searchPathPrefixes.length);
        for (String possibleOverlay : possibleOverlays) {
            String possibleOverlayPath = possibleOverlay + sourcePath.substring(overlayPathPrefix.length());
            if (resolver.getResource(possibleOverlayPath) != null) {
                return possibleOverlayPath;
            }
        }
        return null;
    }

    @Override
    public void onError(Mode mode, String path, Exception e) {
        processLog(Collections.singletonList(String.format("E %s (%s)", path, e)));
    }

    private void processLog(List<String> logMessages) {
        entries.addAll(logMessages);
        long lastChangeTimeInMillis = System.currentTimeMillis();
        if (Math.abs(lastChangeTimeInMillis - lastCommitTimeInMillis) > MILLIS_PER_SECOND) {
            lastCommitTimeInMillis = lastChangeTimeInMillis;
            commitLog();
        }
    }

    public void flushUnsavedData() {
        if (!entries.isEmpty()) {
            commitLog();
        }
    }

    private void commitLog() {
        signLogs();
        while (entries.size() >= MAX_ENTRY_CAPACITY) {
            List<String> fullEntry = entries.subList(0, MAX_ENTRY_CAPACITY);
            PackageLogUtil.putLog(logValueMap, range, fullEntry);
            range = increaseRange(range);
            entries.removeAll(fullEntry);
        }
        PackageLogUtil.putLog(logValueMap, range, entries);
        try {
            resolver.commit();
        } catch (PersistenceException e) {
            LOG.warn("Could not update logs during package action", e);
        }
    }

    private void signLogs() {
        if (!signedByJob) {
            logValueMap.put(PN_LOG_AUTHOR_SIGN, jobId);
            signedByJob = true;
        }
    }
}
