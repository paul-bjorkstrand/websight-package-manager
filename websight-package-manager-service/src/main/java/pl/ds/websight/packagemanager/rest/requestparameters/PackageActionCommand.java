package pl.ds.websight.packagemanager.rest.requestparameters;

import org.apache.jackrabbit.vault.fs.api.ProgressTrackerListener;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.fs.io.ImportOptions;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageException;
import pl.ds.websight.packagemanager.packageaction.PackageActionJobConsumer;
import pl.ds.websight.packagemanager.util.JcrPackageUtil;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.Arrays;

import static org.apache.jackrabbit.vault.packaging.DependencyHandling.REQUIRED;

public enum PackageActionCommand {

    INSTALL(
            "Installation",
            PackageActionJobConsumer.INSTALL_TOPIC,
            "Install Package:",
            "Installing content",
            "Package installed",
            (jcrPackage, listener, classLoader, dryRunVal) ->
                    jcrPackage.install(createImportOptions(listener, classLoader, dryRunVal, jcrPackage))),

    UNINSTALL(
            "Uninstallation",
            PackageActionJobConsumer.UNINSTALL_TOPIC,
            "Uninstall Package:",
            "Uninstalling content",
            "Package uninstalled",
            (jcrPackage, listener, classLoader, dryRunVal) ->
                    jcrPackage.uninstall(createImportOptions(listener, classLoader, dryRunVal, jcrPackage))),

    BUILD(
            "Build",
            PackageActionJobConsumer.BUILD_TOPIC,
            "Build Package:",
            "Building package",
            "Package built",
            (jcrPackage, listener, manager) -> manager.assemble(jcrPackage, listener)),

    COVERAGE(
            "Coverage",
            PackageActionJobConsumer.COVERAGE_TOPIC,
            "Package Coverage Preview:",
            "Dump package coverage",
            "Coverage dumped",
            (jcrPackage, listener, classLoader, dryRunVal) -> {
                JcrPackageDefinition definition = jcrPackage.getDefinition();
                if (definition != null) {
                    definition.dumpCoverage(listener);
                }
            });

    private static final int NODES_MODIFIED_CONCURRENTLY = 1024;

    private final String actionTitle;
    private final String jobTopic;
    private final String logPrefix;
    private final String description;
    private final String logSuffix;
    private CmdPackageExecutor packageExecutor;
    private CmdManagementExecutor managementExecutor;

    PackageActionCommand(String actionTitle, String jobTopic, String logPrefix, String description, String logSuffix,
            CmdPackageExecutor packageExecutor) {
        this.actionTitle = actionTitle;
        this.jobTopic = jobTopic;
        this.logPrefix = logPrefix;
        this.description = description;
        this.logSuffix = logSuffix;
        this.packageExecutor = packageExecutor;
    }

    PackageActionCommand(String actionTitle, String jobTopic, String logPrefix, String description, String logSuffix,
            CmdManagementExecutor managementExecutor) {
        this.actionTitle = actionTitle;
        this.jobTopic = jobTopic;
        this.logPrefix = logPrefix;
        this.description = description;
        this.logSuffix = logSuffix;
        this.managementExecutor = managementExecutor;
    }

    public String getJobTopic() {
        return jobTopic;
    }

    public String getLogPrefix() {
        return logPrefix;
    }

    public String getLogSuffix() {
        return logSuffix;
    }

    public String getDescription() {
        return description;
    }

    public void executeCommand(JcrPackage jcrPackage, ProgressTrackerListener listener, ClassLoader classLoader, JcrPackageManager manager,
            boolean dryRun) throws RepositoryException, PackageException, IOException {
        if (this.managementExecutor == null) {
            this.packageExecutor.execute(jcrPackage, listener, classLoader, dryRun);
        } else {
            this.managementExecutor.execute(jcrPackage, listener, manager);
        }
    }

    public String getActionTitle() {
        return actionTitle;
    }

    @Override
    public String toString() {
        return this.name().toLowerCase(JcrPackageUtil.DEFAULT_LOCALE);
    }

    public static PackageActionCommand fetchByJobTopic(String jobTopic) {
        return Arrays.stream(PackageActionCommand.values())
                .filter(command -> command.getJobTopic().equals(jobTopic))
                .findFirst()
                .orElse(null);
    }

    private static ImportOptions createImportOptions(ProgressTrackerListener listener, ClassLoader classLoader, boolean dryRun,
            JcrPackage jcrPackage) {
        ImportOptions options = new ImportOptions();
        AccessControlHandling acHandling = JcrPackageUtil.fetchDefinition(jcrPackage)
                .map(JcrPackageDefinition::getAccessControlHandling)
                .orElse(null);
        options.setAccessControlHandling(acHandling);
        options.setDependencyHandling(REQUIRED);
        options.setAutoSaveThreshold(NODES_MODIFIED_CONCURRENTLY);
        options.setDryRun(dryRun);
        options.setListener(listener);
        options.setHookClassLoader(classLoader);
        return options;
    }

    private interface CmdPackageExecutor {
        void execute(JcrPackage jcrPackage, ProgressTrackerListener listener, ClassLoader classLoader, boolean dryRun)
                throws RepositoryException, IOException, PackageException;
    }

    private interface CmdManagementExecutor {
        void execute(JcrPackage jcrPackage, ProgressTrackerListener listener, JcrPackageManager manager)
                throws RepositoryException, IOException, PackageException;
    }
}
