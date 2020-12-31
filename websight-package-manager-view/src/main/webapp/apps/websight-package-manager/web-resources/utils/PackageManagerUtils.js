import { getUrlParamValues } from 'websight-admin/services/SearchParamsService';

export const calculatePages = (pagesCount) => {
    return pagesCount ? [...Array(pagesCount).keys()].map(x => x + 1) : [];
}

export const buildGetPackagesParams = (source) => {
    const params = {};
    if (source.filter) {
        params.filter = source.filter;
    }
    if (source.filterOptions && source.filterOptions.length > 0) {
        params.filterOptions = source.filterOptions;
    }
    if (source.sortBy) {
        params.sortBy = source.sortBy;
    }
    if (source.pageNumber) {
        params.pageNumber = source.pageNumber;
    }
    if (source.group) {
        params.group = source.group;
    }
    if (source.path) {
        params.path = source.path;
    }
    return params;
}

export const areGetPackagesParamsDifferent = (params1, params2) => {
    return (!params1 || !params2)
        || (params1.filter !== params2.filter)
        || areFilterOptionsDifferent(params1.filterOptions, params2.filterOptions)
        || (params1.sortBy !== params2.sortBy)
        || (params1.pageNumber !== params2.pageNumber)
        || (params1.group !== params2.group);
}

export const areFilterOptionsDifferent = (options1, options2) => {
    options1 = options1 || [];
    options2 = options2 || [];
    return options1.sort().join(',') !== options2.sort().join(',');
}

export const getPendingPackagesPaths = (packages) => {
    const pendingPackagesPaths = [];
    (packages || []).forEach(packageData => {
        if (['CHECKING', 'QUEUED', 'RUNNING'].includes(packageData.lastAction.state)) {
            pendingPackagesPaths.push(packageData.path);
        }
    });
    return pendingPackagesPaths;
}

export const getRunningPackage = (packages) => {
    return (packages || []).find(packageData => 'RUNNING' === packageData.lastAction.state);
}

export const preservePackageData = (newPackages, previousPackages) => {
    return (newPackages || []).map(newPackage => {
        const previousPackage = (previousPackages || []).find(previous => previous.path === newPackage.path);
        if (previousPackage) {
            return {
                ...newPackage,
                logs: previousPackage.logs,
                isConsoleExpanded: previousPackage.isConsoleExpanded,
                wasConsoleScrolledByUser: previousPackage.wasConsoleScrolledByUser
            }
        } else {
            return newPackage;
        }
    });
}

export const getFilterValuesFromUrl = () => {
    const paramsConfig = {
        pageNumber: { type: 'number' },
        filterOptions: { isArray: true }
    }
    return {
        ...getUrlParamValues(['filter', 'filterOptions', 'pageNumber', 'sortBy'], paramsConfig)
    }
}
