import React from 'react';
import Button, { ButtonGroup } from '@atlaskit/button';
import { LayoutManager, NavigationProvider } from '@atlaskit/navigation-next';
import PageHeader from '@atlaskit/page-header';
import Pagination from '@atlaskit/pagination';
import Spinner from '@atlaskit/spinner';

import Breadcrumbs from 'websight-admin/Breadcrumbs';
import { PageContentContainer, PaginationContainer } from 'websight-admin/Containers';
import { LoadingWrapper } from 'websight-admin/Wrappers';
import GlobalNavigation from 'websight-admin/GlobalNavigation';
import { getUrlHashValue, getUrlParamValue, setUrlHashValue, setUrlParamValues } from 'websight-admin/services/SearchParamsService';
import Footer from 'websight-admin/Footer';
import { AUTH_CONTEXT_UPDATED } from 'websight-rest-atlaskit-client/RestClient';

import { PackageLogs, Separator } from './components/ActivityLog.js';
import ContainerNavigation from './components/ContainerNavigation.js';
import HeaderBottomBar from './components/HeaderBottomBar.js';
import CreatePackageModal from './components/modals/CreatePackageModal.js';
import PackagesCountInfo from './components/PackagesCountInfo.js';
import PackageTable from './components/PackageTable.js';
import UploadPackageModal from './components/modals/UploadPackageModal.js';
import PackageService from './services/PackageService.js';
import * as PackageMangerUtils from './utils/PackageManagerUtils.js';
import { PACKAGE_MANAGER_ROOT_PATH } from './utils/PackageManagerConstants.js'

const CHECK_PACKAGE_ACTION_STATE_TIMEOUT = 1000;
const CHECK_PACKAGE_ACTION_STATE_QUICK_TIMEOUT = 10;
const LOAD_LOGS_FOR_RUNNING_PACKAGE_TIMEOUT = 1000;
const OPEN_CONSOLE_WHEN_PROCESSED_TIMEOUT = 300;

export default class PackageManager extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            packages: [],
            numberOfPages: 0,
            isLoadingPackages: false,
            shouldReload: false,
            shouldForceReload: false,
            groups: [],
            allPackagesCount: null,
            numberOfFoundPackages: null,
            packagesLimit: 0,
            limitExceededPackages: false,
            limitExceededGroups: false,
            isLoadingGroups: false,
            params: {
                pageNumber: 1,
                group: '',
                filterOptions: []
            },
            loadedParams: null,
            selectedPackage: '',
            triggeredPackages: {},
            isPackageStateCheckScheduled: false,
            isLoadingLogsScheduled: false
        }
        // we can't store it in state, because updating it remounts ContainerNavigation and we loose focus on filter input:
        this.groupNameFilter = '';
        this.onHashChange = this.onHashChange.bind(this);
        this.onPageChange = this.onPageChange.bind(this);
        this.findPackages = this.findPackages.bind(this);
        this.refreshPage = this.refreshPage.bind(this);
        this.selectPackage = this.selectPackage.bind(this);
        this.setLogs = this.setLogs.bind(this);
        this.toggleConsole = this.toggleConsole.bind(this);
        this.onConsoleScroll = this.onConsoleScroll.bind(this);
        this.openConsoleForNewPackage = this.openConsoleForNewPackage.bind(this);
        this.updateActionState = this.updateActionState.bind(this);
        this.checkPackagesStates = this.checkPackagesStates.bind(this);
        this.loadLogsForRunningPackage = this.loadLogsForRunningPackage.bind(this);
    }

    componentDidMount() {
        window.addEventListener('hashchange', this.onHashChange);
        this.loadDataFromUrl();
        window.addEventListener(AUTH_CONTEXT_UPDATED, () => {
            this.loadDataFromUrl();
        });
    }

    componentWillUnmount() {
        window.removeEventListener('hashchange', this.onHashChange);
    }

    onHashChange() {
        this.loadDataFromUrl();
    }

    loadDataFromUrl() {
        const path = getUrlParamValue('path');
        let params;
        if (path) {
            params = {
                path: path,
                group: null,
                filter: null,
                filterOptions: [],
                sortBy: null,
                pageNumber: null
            }
        } else {
            params = {
                group: getUrlHashValue(),
                ...PackageMangerUtils.getFilterValuesFromUrl()
            }
        }
        this.findPackages(params);
        this.getGroups();
    }

    componentDidUpdate(prevProps, prevState) {
        this.setUrlParams(prevState);
        this.scrollActivityLogConsoles();
    }

    setUrlParams(prevState) {
        const { params, selectedPackage } = this.state;
        if (JSON.stringify(params) !== JSON.stringify(prevState.params)) {
            let pageNumber;
            let path;

            if (params.path || selectedPackage) {
                path = params.path || selectedPackage;
            } else {
                pageNumber = params.pageNumber > 1 ? params.pageNumber : undefined;
            }
            const filterParams = { ...this.state.params, pageNumber: pageNumber, group: undefined, path: path }
            setUrlParamValues(filterParams);
        }
        if (params.group !== prevState.group) {
            setUrlHashValue(params.group);
        }
        if (selectedPackage !== prevState.selectedPackage) {
            let filterParams = PackageMangerUtils.getFilterValuesFromUrl();
            if (selectedPackage) {
                filterParams = { ...filterParams, pageNumber: undefined };
            } else if (params.pageNumber > 1) {
                filterParams = { ...filterParams, pageNumber: params.pageNumber };
            }
            filterParams = { ...filterParams, path: selectedPackage };
            setUrlParamValues(filterParams);
        }
    }

    scrollActivityLogConsoles() {
        const consoles = document.getElementsByClassName('activity-log-console');
        if (consoles) {
            Array.from(consoles).forEach(consoleElement => {
                const packagePath = consoleElement.id.substring('activity-log-console|'.length);
                const packageData = this.getPackageByPath(packagePath);
                if (!packageData.wasConsoleScrolledByUser) {
                    consoleElement.scrollTop = consoleElement.scrollHeight
                }
            });
        }
    }

    getPackageByPath(path) {
        return this.state.packages.find(packageData => packageData.path === path);
    }

    findPackages(newParams, forceReload, callback) {
        if (!('path' in newParams)) {
            newParams.path = null;
        }

        if (this.state.isLoadingPackages) {
            this.setState(prevState => ({
                params: { ...prevState.params, ...newParams },
                shouldReload: true,
                shouldForceReload: forceReload
            }));
            return;
        }

        const params = PackageMangerUtils.buildGetPackagesParams({ ...this.state.params, ...newParams });
        if (forceReload || PackageMangerUtils.areGetPackagesParamsDifferent(params, this.state.loadedParams)) {
            this.setState(
                prevState => ({
                    params: { ...prevState.params, ...newParams },
                    isLoadingPackages: true,
                    shouldReload: false,
                    shouldForceReload: false
                }),
                () => this.loadPackages(params, forceReload, callback));
        } else {
            this.setState(prevState => ({
                params: { ...prevState.params, ...newParams },
                shouldReload: false,
                shouldForceReload: false
            }));
        }
    }

    loadPackages(params, shouldPreservePackageData, callback) {
        const onSuccess = (data) => {
            this.setState((prevState) => {
                const prevParams = prevState.params;
                const selectedPackage = data.packages.find(pack => pack.path === (params.path || prevState.selectedPackage));

                const wasPageNumberChangedAfterRequest = params.pageNumber !== prevParams.pageNumber && prevParams.pageNumber;
                const newPage = wasPageNumberChangedAfterRequest ? prevParams.pageNumber : data.pageNumber;
                const wasGroupChangedAfterRequest = params.group !== prevParams.group && prevParams.group;
                const newGroup = wasGroupChangedAfterRequest ? prevParams.group : data.group;
                const wasSortByChangedAfterRequest = params.sortBy !== prevParams.sortBy && prevParams.sortBy;
                const newSortBy = wasSortByChangedAfterRequest
                    ? prevState.params.sortBy
                    : data.limitExceeded ? null : params.sortBy;

                return {
                    packages: shouldPreservePackageData
                        ? PackageMangerUtils.preservePackageData(data.packages, prevState.packages)
                        : data.packages,
                    numberOfPages: data.numberOfPages,
                    pageNumber: newPage,
                    numberOfFoundPackages: data.numberOfFoundPackages,
                    limitExceededPackages: data.limitExceeded,
                    packagesLimit: data.packagesLimit,
                    isLoadingPackages: false,
                    params: { ...prevState.params, pageNumber: newPage, group: newGroup, sortBy: newSortBy },
                    loadedParams: { ...params, pageNumber: data.pageNumber, group: data.group, sortBy: newSortBy },
                    selectedPackage: selectedPackage ? selectedPackage.path : null
                }
            }, callback);
        }

        const onComplete = () => {
            if (this.state.shouldReload) {
                this.findPackages(PackageMangerUtils.buildGetPackagesParams(this.state.params), this.state.shouldForceReload);
            } else {
                if (this.state.isLoadingPackages) {
                    // if onSuccess wasn't called, restore last loaded params
                    this.setState((prevState) => ({
                        isLoadingPackages: false,
                        params: { ...prevState.loadedParams }
                    }));
                }
                this.schedulePackagesStatesCheck();
            }
        }

        PackageService.findPackages(params, onSuccess, onComplete);
    }

    getGroups() {
        this.setState({ isLoadingGroups: true })
        const onSuccess = (data) => {
            this.setState({
                allPackagesCount: data.allPackagesCount,
                limitExceededGroups: data.limitExceeded,
                groups: data.groups,
                isLoadingGroups: false
            })
        }

        PackageService.getGroups(onSuccess);
    }

    onPageChange(event, newPage) {
        this.findPackages({ pageNumber: newPage });
    }

    refreshPage(callback) {
        this.findPackages({}, true, callback);
        this.getGroups();
    }

    selectPackage(path) {
        this.setState((prevState) => ({
            selectedPackage: prevState.selectedPackage === path ? undefined : path
        }));
    }

    setLogs(path, activityLogs) {
        this.updatePackageState(path, previousStatePackage => {
            const newLogs = [];
            newLogs.push(activityLogs);
            return {
                ...previousStatePackage,
                logs: newLogs
            }
        });
    }

    toggleConsole(path, expand, logsToAppend) {
        if (expand) {
            this.openConsole(path, logsToAppend);
        } else {
            this.closeConsole(path);
        }
    }

    closeConsole(path) {
        const packageData = this.getPackageByPath(path) || {};
        if (packageData.isConsoleExpanded) {
            this.updatePackageState(path, previousStatePackage => {
                return {
                    ...previousStatePackage,
                    isConsoleExpanded: false,
                    logs: []
                }
            });
        }
    }

    openConsole(path, logsToAppend, shouldReload) {
        const packageData = this.getPackageByPath(path) || {};
        const hasLogs = (['CHECKING', 'QUEUED', 'RUNNING', 'FINISHED'].includes((packageData.lastAction || {}).state));
        if (hasLogs && (!packageData.isConsoleExpanded || shouldReload)) {
            this.updatePackageState(path, previousStatePackage => {
                return {
                    ...previousStatePackage,
                    isConsoleExpanded: true,
                    logs: [<Spinner size='small' key={path} />],
                    wasConsoleScrolledByUser: false
                }
            });
            const onSuccess = ({ entity }) => {
                const loadedLogsAreNotEmpty = entity.logs && entity.logs.length > 0;
                let logs = [];
                if (loadedLogsAreNotEmpty) {
                    logs = [<PackageLogs logs={entity.logs} path={packageData.path} key={packageData.path} />];
                }
                if (logsToAppend) {
                    if (loadedLogsAreNotEmpty) {
                        logs.push(<Separator />);
                    }
                    logs.push(logsToAppend);
                }
                this.updatePackageState(path, previousStatePackage => {
                    return {
                        ...previousStatePackage,
                        isConsoleExpanded: true,
                        logs: logs,
                        wasConsoleScrolledByUser: false
                    }
                });
            }
            const onFailure = () => {
                this.updatePackageState(path, previousStatePackage => {
                    return {
                        ...previousStatePackage,
                        isConsoleExpanded: false,
                        logs: [],
                        wasConsoleScrolledByUser: false
                    }
                });
            }
            PackageService.getPackageActionReport(path, onSuccess, onFailure);
        } else if (logsToAppend) {
            this.updatePackageState(path, previousStatePackage => {
                const logs = previousStatePackage.logs;
                if (logs.length > 0) {
                    logs.push(<Separator />);
                }
                logs.push(logsToAppend);
                return {
                    ...previousStatePackage,
                    isConsoleExpanded: true,
                    logs: logs,
                    wasConsoleScrolledByUser: false
                }
            });
        }
    }

    onConsoleScroll(path, isOnBottom) {
        const packageData = this.getPackageByPath(path);
        const alreadyScrolledByUserBefore = packageData.wasConsoleScrolledByUser;
        const newConsoleScrolled = !isOnBottom;
        if (newConsoleScrolled && alreadyScrolledByUserBefore) {
            return;
        }
        this.updatePackageState(path, previousStatePackage => {
            return {
                ...previousStatePackage,
                wasConsoleScrolledByUser: newConsoleScrolled
            }
        });
    }

    updatePackageState(path, modifier, afterSetStateCallback) {
        this.setState(prevState => {
            const packages = prevState.packages.map(pkg => pkg.path === path ? modifier(pkg) : pkg);
            return { packages: packages };
        }, afterSetStateCallback);
    }

    addToTriggered(path) {
        this.setState(prevState => {
            const previousTriggeredPackages = prevState.triggeredPackages;
            return {
                triggeredPackages: {
                    ...previousTriggeredPackages,
                    [path]: true
                }
            }
        })
    }

    updateActionState(path, state, addToTriggered) {
        this.updatePackageState(path, previousStatePackage => ({
            ...previousStatePackage,
            lastAction: state
        }), () => this.schedulePackagesStatesCheck(true));
        if (addToTriggered) {
            this.addToTriggered(path);
        }
        if (state.type === 'CANCEL') {
            this.setState(prevState => {
                const previousTriggeredPackages = prevState.triggeredPackages;
                delete previousTriggeredPackages[path];
                return previousTriggeredPackages;
            })
        }
    }

    schedulePackagesStatesCheck(quickCheck = false) {
        if (this.state.isPackageStateCheckScheduled || this.state.isLoadingPackages) {
            return;
        }
        const pendingPackagesPaths = PackageMangerUtils.getPendingPackagesPaths(this.state.packages);
        if (pendingPackagesPaths.length > 0) {
            this.setState({
                isPackageStateCheckScheduled: true
            }, () => setTimeout(
                this.checkPackagesStates,
                quickCheck ? CHECK_PACKAGE_ACTION_STATE_QUICK_TIMEOUT : CHECK_PACKAGE_ACTION_STATE_TIMEOUT
            ));
            this.scheduleLoadingLogsForRunningPackage();
        }
    }

    checkPackagesStates() {
        if (this.state.isLoadingPackages) {
            this.setState({
                isPackageStateCheckScheduled: false
            });
            return;
        }
        const pendingPackagesPaths = PackageMangerUtils.getPendingPackagesPaths(this.state.packages);
        if (pendingPackagesPaths.length > 0) {
            const onSuccess = ({ entity }) => {
                this.openConsoleForProcessedPackages(pendingPackagesPaths, entity);
                const packageThatFinished = pendingPackagesPaths.find(packagePath => {
                    return entity[packagePath].state === 'FINISHED';
                })
                if (packageThatFinished) {
                    // There is at least one package that requires update of its building status
                    this.setState({
                        isPackageStateCheckScheduled: false
                    }, this.refreshPage);
                } else {
                    pendingPackagesPaths.forEach(packagePath => {
                        this.updateActionState(packagePath, entity[packagePath]);
                    });
                    this.setState({
                        isPackageStateCheckScheduled: false
                    }, this.schedulePackagesStatesCheck);
                }
            }
            PackageService.getPackagesActionsStates(pendingPackagesPaths, onSuccess);
        } else {
            this.setState({
                isPackageStateCheckScheduled: false
            });
        }
    }

    openConsoleWhenProcessed(path) {
        this.setState(prevState => {
            const previousTriggeredPackages = prevState.triggeredPackages;
            delete previousTriggeredPackages[path];
            return previousTriggeredPackages;
        })
        setTimeout(() => this.openConsole(path, null, true), OPEN_CONSOLE_WHEN_PROCESSED_TIMEOUT);
    }

    openConsoleForProcessedPackages(pendingPackagesPaths, packagesStatuses) {
        (pendingPackagesPaths || []).forEach(pendingPackagePath => {
            if (!this.state.triggeredPackages[pendingPackagePath]) {
                return;
            }
            const previousPackageState = (this.getPackageByPath(pendingPackagePath) || {}).lastAction;
            const processedStates = ['RUNNING', 'FINISHED'];
            const wasNotProcessedBefore = !processedStates.includes(previousPackageState.state);
            const wasProcessedNow = processedStates.includes(packagesStatuses[pendingPackagePath].state);
            if (wasNotProcessedBefore && wasProcessedNow) {
                this.openConsoleWhenProcessed(pendingPackagePath);
            }
        })
    }

    openConsoleForNewPackage(path) {
        const packageData = this.getPackageByPath(path);
        if (packageData) {
            if (['RUNNING', 'FINISHED'].includes(packageData.lastAction.state)) {
                this.openConsoleWhenProcessed(path);
            } else {
                this.addToTriggered(path);
            }
        }
    }

    scheduleLoadingLogsForRunningPackage() {
        if (this.state.isLoadingLogsScheduled) {
            return;
        }
        const runningPackage = PackageMangerUtils.getRunningPackage(this.state.packages);
        if (!runningPackage) {
            return;
        }
        this.setState({
            isLoadingLogsScheduled: true
        }, () => setTimeout(() => this.loadLogsForRunningPackage(runningPackage.path), LOAD_LOGS_FOR_RUNNING_PACKAGE_TIMEOUT));
    }

    loadLogsForRunningPackage(path) {
        const runningPackage = this.getPackageByPath(path);
        if (!runningPackage) {
            this.setState({
                isLoadingLogsScheduled: false
            });
            return;
        }
        if (runningPackage.isConsoleExpanded) {
            const onSuccess = ({ entity }) => {
                const packageToUpdateLogs = this.getPackageByPath(runningPackage.path) || {};
                if (packageToUpdateLogs.isConsoleExpanded) {
                    this.setLogs(
                        runningPackage.path,
                        <PackageLogs logs={entity.logs} path={packageToUpdateLogs.path} />
                    );
                }
                this.setState({
                    isLoadingLogsScheduled: false
                }, () => this.scheduleLoadingLogsForRunningPackage());
            }
            PackageService.getPackageActionReport(runningPackage.path, onSuccess);
        } else {
            this.setState({
                isLoadingLogsScheduled: false
            }, () => this.scheduleLoadingLogsForRunningPackage());
        }
    }

    extractGroup(path) {
        let group = path.substring(0, path.lastIndexOf('/'));
        group = group.replace('/etc/packages', '');
        if (group.charAt(0) === '/') {
            group = group.substring(1, group.length);
        }
        const groups = group.split('/');
        return groups[0] || ':no_group';
    }

    clearFiltersAndSelectNewPackage(path) {
        const group = path ? this.extractGroup(path) : '';
        this.setState((prevState) => ({
            selectedPackage: prevState.selectedPackage === path ? undefined : path,
            params: {
                pageNumber: 1,
                group: group,
                filterOptions: [],
                filter: null,
                sortBy: null
            }
        }));
    }

    render() {
        const containerNavigation = (
            <ContainerNavigation
                groups={this.state.groups}
                selected={this.state.params.group}
                allPackagesCount={this.state.allPackagesCount}
                limitExceeded={this.state.limitExceededGroups}
                isLoadingGroups={this.state.isLoadingGroups}
                findPackages={this.findPackages}
                groupNameFilter={this.groupNameFilter}
                onGroupNameFilterChange={(value) => this.groupNameFilter = value}
            />
        )

        const actions = (
            <ButtonGroup>
                <Button onClick={() => this.uploadPackageModal.open()}>Upload</Button>
                <Button onClick={() => this.createPackageModal.open()}>Create</Button>
            </ButtonGroup>
        )

        const bottomBar = (
            <HeaderBottomBar
                findPackages={this.findPackages}
                params={this.state.params}
                limitExceeded={this.state.limitExceededPackages}
            />
        )

        const showPagination = this.state.numberOfPages > 1
            || (this.state.numberOfPages === 1 && this.state.params.pageNumber > 1);

        return (
            <NavigationProvider>
                <LayoutManager
                    globalNavigation={GlobalNavigation}
                    productNavigation={() => null}
                    containerNavigation={() => containerNavigation}
                    experimental_horizontalGlobalNav
                >
                    <PageContentContainer>
                        <PageHeader
                            actions={actions}
                            bottomBar={bottomBar}
                            breadcrumbs={
                                <Breadcrumbs breadcrumbs={[
                                    { text: 'Package Manager', path: PACKAGE_MANAGER_ROOT_PATH, reactPath: '' }
                                ]} />
                            }
                        >
                            Packages
                        </PageHeader>
                        <PackagesCountInfo
                            numberOfFoundPackages={this.state.numberOfFoundPackages}
                            packagesLimit={this.state.packagesLimit}
                            packagesLimitExceeded={this.state.limitExceededPackages}
                        />
                        <PackageTable
                            packages={this.state.packages}
                            groups={this.state.groups}
                            toggleConsole={this.toggleConsole}
                            onConsoleScroll={this.onConsoleScroll}
                            openConsoleForNewPackage={this.openConsoleForNewPackage}
                            refreshPage={this.refreshPage}
                            selectPackage={this.selectPackage}
                            selectedPackagePath={this.state.selectedPackage}
                            updateActionState={this.updateActionState}
                            isLoading={this.state.isLoadingPackages}
                            isInitialized={this.state.loadedParams !== null}
                        />
                        {showPagination && (
                            <PaginationContainer>
                                <LoadingWrapper isLoading={this.state.isLoadingPackages}>
                                    <Pagination
                                        pages={PackageMangerUtils.calculatePages(this.state.numberOfPages)}
                                        selectedIndex={this.state.params.pageNumber - 1}
                                        onChange={this.onPageChange}
                                    />
                                </LoadingWrapper>
                            </PaginationContainer>
                        )}
                        <UploadPackageModal
                            onUploadSuccess={(path) => {
                                this.clearFiltersAndSelectNewPackage(path);
                                this.refreshPage();
                            }}
                            onUploadAndInstallSuccess={(path) => {
                                this.clearFiltersAndSelectNewPackage(path);
                                this.refreshPage(() => this.openConsoleForNewPackage(path));
                            }}
                            ref={element => this.uploadPackageModal = element}
                        />
                        <CreatePackageModal
                            groups={this.state.groups}
                            defaultGroup={this.state.params.group}
                            onCreateSuccess={(path) => {
                                this.clearFiltersAndSelectNewPackage(path);
                                this.refreshPage();
                            }}
                            onCreateAndBuildSuccess={(path) => {
                                this.clearFiltersAndSelectNewPackage(path);
                                this.refreshPage(() => this.openConsoleForNewPackage(path));
                            }}
                            ref={(element) => this.createPackageModal = element}
                        />
                    </PageContentContainer>
                    <Footer />
                </LayoutManager>
            </NavigationProvider>
        );
    }
}
