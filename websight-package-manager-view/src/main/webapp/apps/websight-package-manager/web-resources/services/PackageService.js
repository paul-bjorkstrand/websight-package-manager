import RestClient from 'websight-rest-atlaskit-client/RestClient';

const packageRequestData = (packageData) => ({
    path: packageData.path,
    name: packageData.name,
    version: packageData.version,
    description: packageData.description,
    group: packageData.group ? packageData.group.value : '',
    filters: packageData.filters,
    thumbnail: packageData.thumbnail,
    deleteThumbnail: packageData.deleteThumbnail,
    acHandling: packageData.acHandling ? packageData.acHandling.value : '',
    requiresRestart: packageData.requiresRestart,
    dependencies: packageData.dependencies
});

const extractPackages = (packages) => {
    return packages.map((entry) => ({
        path: entry.path,
        group: entry.packageInfo ? entry.packageInfo.group : null,
        name: entry.packageInfo ? entry.packageInfo.name : null,
        downloadName: entry.packageInfo ? entry.packageInfo.downloadName : null,
        version: entry.packageInfo ? entry.packageInfo.versionString : null,
        description: entry.description,
        size: (entry.size ? formatFileSize(entry.size, true) : 0),
        timestamp: entry.timestamp,
        thumbnail: entry.thumbnail,
        buildCount: entry.buildCount,
        status: entry.status,
        filters: entry.filters || [],
        lastAction: entry.lastAction,
        acHandling: entry.acHandling,
        requiresRestart: entry.requiresRestart || false,
        dependencies: entry.dependencies || [],
        nextScheduledAction: entry.nextScheduledAction,
        logs: [],
        isConsoleExpanded: false,
        wasConsoleScrolledByUser: false,
        modification: entry.modification,
        packageInfo: entry.packageInfo
    }));
}

const formatFileSize = (bytes, isDecimal) => {
    const threshold = isDecimal ? 1000 : 1024;
    if (Math.abs(bytes) < threshold) {
        return bytes + ' B';
    }
    const units = isDecimal
        ? ['KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB']
        : ['KiB', 'MiB', 'GiB', 'TiB', 'PiB', 'EiB', 'ZiB', 'YiB'];
    let chosenUnitIndex = -1;
    do {
        bytes /= threshold;
        ++chosenUnitIndex;
    } while (Math.abs(bytes) >= threshold && chosenUnitIndex < units.length - 1);
    return bytes.toFixed(1) + ' ' + units[chosenUnitIndex];
}

class PackageService {

    constructor() {
        this.client = new RestClient('websight-package-manager-service');
    }

    findPackages(params, onSuccess, onComplete) {
        this.client.get({
            action: 'find-packages',
            parameters: params,
            onSuccess: (data) => {
                onSuccess({
                    packages: extractPackages(data.entity.packages || []),
                    numberOfPages: data.entity.numberOfPages || 0,
                    numberOfFoundPackages: data.entity.numberOfFoundPackages || 0,
                    pageNumber: data.entity.pageNumber || 0,
                    limitExceeded: data.entity.limitExceeded,
                    packagesLimit: data.entity.packagesLimit,
                    group: data.entity.group || null
                });
                onComplete();
            },
            onValidationFailure: onComplete,
            onFailure: onComplete,
            onError: onComplete,
            onNonFrameworkError: onComplete
        });
    }

    getGroups(onSuccess) {
        this.client.get({
            action: 'get-groups',
            onSuccess: (data) => {
                onSuccess(data.entity);
            }
        });
    }

    createPackage(packageData, onSuccess, onValidationFailure, onFailure, onComplete) {
        this.client.post({
            action: 'create-package',
            data: packageRequestData(packageData),
            onSuccess: onSuccess,
            onValidationFailure: onValidationFailure,
            onFailure: onFailure,
            onError: onComplete,
            onNonFrameworkError: onComplete
        })
    }

    createAndBuildPackage(packageData, onSuccess, onValidationFailure, onFailure, onComplete) {
        this.client.post({
            action: 'create-and-build-package',
            data: packageRequestData(packageData),
            onSuccess: onSuccess,
            onValidationFailure: onValidationFailure,
            onFailure: onFailure,
            onError: onComplete,
            onNonFrameworkError: onComplete
        })
    }

    updatePackage(packageData, onSuccess, onValidationFailure, onComplete) {
        this.client.post({
            action: 'edit-package',
            data: packageRequestData(packageData),
            onSuccess: onSuccess,
            onValidationFailure: onValidationFailure,
            onFailure: onComplete,
            onError: onComplete,
            onNonFrameworkError: onComplete
        })
    }

    uploadPackage(data, onSuccess, onValidationFailure, onComplete) {
        this.client.post({
            action: 'upload-package',
            data: data,
            onSuccess: onSuccess,
            onValidationFailure: onValidationFailure,
            onFailure: onComplete,
            onError: onComplete,
            onNonFrameworkError: onComplete
        });
    }

    uploadAndInstallPackage(data, onSuccess, onValidationFailure, onFailure, onComplete) {
        this.client.post({
            action: 'upload-and-install-package',
            data: data,
            onSuccess: onSuccess,
            onValidationFailure: onValidationFailure,
            onFailure: onFailure,
            onError: onComplete,
            onNonFrameworkError: onComplete
        });
    }

    buildPackage(path, onSuccess, onComplete) {
        this.client.post({
            action: 'build-package',
            data: { path: path },
            onSuccess: onSuccess,
            onValidationFailure: onComplete,
            onFailure: onComplete,
            onError: onComplete,
            onNonFrameworkError: onComplete
        });
    }

    installPackage(path, acHandling, extractSubpackages, onSuccess, onComplete, dryRun) {
        this.client.post({
            action: 'install-package',
            data: { path: path, acHandling: acHandling, extractSubpackages: extractSubpackages, dryRun: dryRun },
            onSuccess: onSuccess,
            onValidationFailure: onComplete,
            onFailure: onComplete,
            onError: onComplete,
            onNonFrameworkError: onComplete
        });
    }

    uninstallPackage(path, onSuccess, onComplete) {
        this.client.post({
            action: 'uninstall-package',
            data: { path: path },
            onSuccess: onSuccess,
            onValidationFailure: onComplete,
            onFailure: onComplete,
            onError: onComplete,
            onNonFrameworkError: onComplete
        });
    }

    cancelQueuedPackageAction(path, onSuccess, onComplete) {
        this.client.post({
            action: 'cancel-package-action',
            data: { path: path },
            onSuccess: onSuccess,
            onValidationFailure: onComplete,
            onFailure: onComplete,
            onError: onComplete,
            onNonFrameworkError: onComplete
        });
    }

    dumpCoverage(path, onSuccess, onComplete) {
        this.client.post({
            action: 'coverage-package',
            data: { path: path },
            onSuccess: onSuccess,
            onValidationFailure: onComplete,
            onFailure: onComplete,
            onError: onComplete,
            onNonFrameworkError: onComplete
        });
    }

    deletePackage(path, onSuccess, onComplete) {
        this.client.post({
            action: 'delete-package',
            data: { path: path },
            onSuccess: onSuccess,
            onValidationFailure: onComplete,
            onFailure: onComplete,
            onError: onComplete,
            onNonFrameworkError: onComplete
        });
    }

    getPackagesActionsStates(paths, onSuccess, onComplete) {
        this.client.get({
            action: 'get-packages-actions-states',
            parameters: { path: paths },
            onSuccess: onSuccess,
            onValidationFailure: onComplete,
            onFailure: onComplete,
            onError: onComplete,
            onNonFrameworkError: onComplete
        });
    }

    getPackageActionReport(path, onSuccess, onFailure) {
        this.client.get({
            action: 'get-package-action-report',
            parameters: { path: path },
            onSuccess: onSuccess,
            onFailure: onFailure
        });
    }

    schedulePackageActions(requestData, onSuccess, onValidationFailure, onComplete) {
        this.client.post({
            action: 'schedule-package-actions',
            data: requestData,
            onSuccess: onSuccess,
            onValidationFailure: onValidationFailure,
            onFailure: onComplete,
            onError: onComplete,
            onNonFrameworkError: onComplete
        });
    }

    getScheduledPackageActions(path, onSuccess) {
        this.client.get({
            action: 'get-scheduled-package-actions',
            parameters: { path: path },
            onSuccess: onSuccess
        })
    }
}

export default new PackageService();
