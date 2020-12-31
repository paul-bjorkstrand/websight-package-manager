import React from 'react';
import DropdownMenu, { DropdownItem } from '@atlaskit/dropdown-menu';

import { TableRowActionButton } from 'websight-admin/Buttons';
import ConfirmationModal from 'websight-admin/ConfirmationModal';
import { TableRowActionButtonsContainer } from 'websight-admin/Containers';

import PackageService from '../services/PackageService.js';
import * as PackageUtil from '../utils/PackageUtil.js';
import { Filters } from './ActivityLog.js';
import EditPackageModal from './modals/EditPackageModal.js';
import SchedulePackageActionsModal from './modals/SchedulePackageActionsModal.js';

const getSuggestedAction = (packageData) => {
    const isBuilt = PackageUtil.isPackageBuilt(packageData);
    const isNotInstalled = !PackageUtil.isPackageInstalled(packageData);
    const isModified = PackageUtil.isPackageModified(packageData);
    if (isBuilt && isNotInstalled && !isModified) {
        return 'install';
    } else if (isModified || !isBuilt) {
        return 'build';
    }
}

export default class PackageActions extends React.Component {

    constructor(props) {
        super(props);
        this.buildPackage = this.buildPackage.bind(this);
        this.deletePackage = this.deletePackage.bind(this);
        this.dumpCoverage = this.dumpCoverage.bind(this);
        this.installPackage = this.installPackage.bind(this);
        this.showFilters = this.showFilters.bind(this);
        this.uninstallPackage = this.uninstallPackage.bind(this);
    }

    buildPackage(path) {
        PackageService.buildPackage(
            path,
            () => {
                this.buildConfirmationModal.close();
                this.props.updateActionState(path, { state: 'CHECKING', type: 'BUILD' }, true);
            },
            () => this.buildConfirmationModal.close()
        );
    }

    installPackage(path, dryRun = false) {
        PackageService.installPackage(
            path,
            () => {
                this.installConfirmationModal.close();
                this.props.updateActionState(path, { state: 'CHECKING', type: 'INSTALL' }, true);
            },
            () => this.installConfirmationModal.close(),
            dryRun
        );
    }

    uninstallPackage(path) {
        PackageService.uninstallPackage(
            path,
            () => {
                this.uninstallConfirmationModal.close();
                this.props.updateActionState(path, { state: 'CHECKING', type: 'UNINSTALL' }, true);
            },
            () => this.uninstallConfirmationModal.close()
        );
    }

    dumpCoverage(path) {
        PackageService.dumpCoverage(
            path,
            () => {
                this.props.updateActionState(path, { state: 'CHECKING', type: 'COVERAGE' }, true);
            }
        );
    }

    showFilters() {
        const { packageData } = this.props;
        const showFiltersLogs = (
            <Filters name={packageData.name} path={packageData.path} filters={packageData.filters} date={new Date()} />
        );
        this.props.toggleConsole(packageData.path, true, showFiltersLogs);
    }

    deletePackage() {
        PackageService.deletePackage(
            this.props.packageData.path,
            () => {
                this.deleteConfirmationModal.close();
                this.props.refreshPage();
            },
            () => this.deleteConfirmationModal.close()
        );
    }

    render() {
        const { packageData } = this.props;
        const suggestedAction = getSuggestedAction(packageData);
        const isPackageInstalled = PackageUtil.isPackageInstalled(packageData);
        const isPackageBuilt = PackageUtil.isPackageBuilt(packageData);
        return (
            <TableRowActionButtonsContainer>
                <TableRowActionButton
                    tooltipContent='Build'
                    iconName='gavel'
                    appearance={suggestedAction === 'build' ? 'primary' : 'default'}
                    onClick={() => this.buildConfirmationModal.open()}
                />
                <TableRowActionButton
                    tooltipContent={isPackageInstalled ? 'Reinstall' : 'Install'}
                    iconName='play_arrow'
                    isDisabled={!isPackageBuilt}
                    appearance={suggestedAction === 'install' ? 'primary' : 'default'}
                    onClick={() => this.installConfirmationModal.open()}
                />
                <DropdownMenu
                    trigger=''
                    triggerType='button'
                >
                    <DropdownItem
                        href={packageData.path + '?' + packageData.timestamp}
                        isDisabled={!isPackageBuilt}
                    >
                        Download
                    </DropdownItem>
                    <DropdownItem
                        onClick={() => this.editPackageModal.open()}
                    >
                        Edit
                    </DropdownItem>
                    <DropdownItem
                        onClick={this.showFilters}
                    >
                        Show Filters
                    </DropdownItem>
                    <DropdownItem
                        onClick={() => this.dumpCoverage(packageData.path)}
                    >
                        Coverage
                    </DropdownItem>
                    <DropdownItem
                        isDisabled={!isPackageBuilt}
                        onClick={() => this.installPackage(packageData.path, true)}
                    >
                        Test Install
                    </DropdownItem>
                    <DropdownItem
                        onClick={() => this.uninstallConfirmationModal.open()}
                        isDisabled={!isPackageInstalled}
                    >
                        Uninstall
                    </DropdownItem>
                    <DropdownItem
                        onClick={() => this.deleteConfirmationModal.open()}
                    >
                        Delete
                    </DropdownItem>
                    <DropdownItem
                        onClick={() => this.schedulePackageActionsModal.open()}
                    >
                        Schedule
                    </DropdownItem>
                </DropdownMenu>
                <ConfirmationModal
                    buttonText={'Build'}
                    heading={'Build package'}
                    message={(
                        <>
                            Do you really want to build this package?<br /><br />
                            <b>{packageData.downloadName}</b>
                        </>
                    )}
                    onConfirm={() => this.buildPackage(packageData.path)}
                    ref={(element) => this.buildConfirmationModal = element}
                />
                <ConfirmationModal
                    buttonText={isPackageInstalled ? 'Reinstall' : 'Install'}
                    heading={isPackageInstalled ? 'Reinstall' : 'Install' + ' package'}
                    message={(
                        <>
                            Do you really want to {isPackageInstalled ? 'reinstall' : 'install'} this
                            package?<br /><br />
                            <b>{packageData.downloadName}</b>
                        </>
                    )}
                    onConfirm={() => this.installPackage(packageData.path)}
                    ref={(element) => this.installConfirmationModal = element}
                />
                <EditPackageModal
                    packageToEdit={packageData}
                    groups={this.props.groups}
                    onEditSuccess={() => this.props.refreshPage()}
                    ref={(element) => this.editPackageModal = element}
                />
                <ConfirmationModal
                    buttonText={'Delete'}
                    heading={'Delete package'}
                    appearance='danger'
                    message={(
                        <>
                            Are you sure you want to permanently delete package?<br /><br />
                            <b>{packageData.downloadName}</b>
                        </>
                    )}
                    onConfirm={this.deletePackage}
                    ref={(element) => this.deleteConfirmationModal = element}
                />
                <ConfirmationModal
                    buttonText={'Uninstall'}
                    heading={'Uninstall package'}
                    message={(
                        <>
                            Do you really want to uninstall this package?<br /><br />
                            <b>{packageData.downloadName}</b>
                        </>
                    )}
                    onConfirm={() => this.uninstallPackage(packageData.path)}
                    ref={(element) => this.uninstallConfirmationModal = element}
                />
                <SchedulePackageActionsModal
                    packagePath={packageData.path}
                    packageName={packageData.downloadName}
                    onScheduleSuccess={() => this.props.refreshPage()}
                    ref={(element) => this.schedulePackageActionsModal = element}
                />
            </TableRowActionButtonsContainer>
        );
    }
}
