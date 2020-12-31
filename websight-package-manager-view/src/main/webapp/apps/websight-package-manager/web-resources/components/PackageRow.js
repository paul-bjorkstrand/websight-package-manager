import React from 'react';
import Badge from '@atlaskit/badge';
import Spinner from '@atlaskit/spinner';
import { Cell, Row } from '@atlaskit/table-tree';
import Tooltip from '@atlaskit/tooltip';
import styled from 'styled-components';

import { ConsoleContainer } from 'websight-admin/Containers';
import { colors } from 'websight-admin/theme';
import { ResizableWrapper } from 'websight-admin/Wrappers';

import PackageService from '../services/PackageService.js';
import * as PackageUtil from '../utils/PackageUtil.js';
import { CancelButton } from './CancelButton.js';
import PackageActions from './PackageActions.js';
import PackageStatuses from './PackageStatuses.js';
import PackageThumbnail from './PackageThumbnail.js';
import PackageInfo from './PackageInfo.js';
import PackageBuildDetails from './PackageBuildDetails.js';

const NameContainer = styled.span`
    display: flex;
    align-items: center;
    white-space: normal;
`;

const ActivityLogContainer = styled.div`
    margin: 10px 30px;
`;

const ResizableConsoleContent = styled.div`
    width: 100%;
    height: 100%;
    overflow: hidden;
`;

const ActivityLogConsole = styled(ConsoleContainer)`
    height: 100%;
    width: auto;
`;

const PackageActionStateDetails = styled.div`
    font-size: 12px;
    color: ${colors.darkGrey};
`;

const QueuedStateContainer = styled.div`
    display: flex;
`;

const getSelectedStylesOrEmpty = (packageData) => {
    return packageData.isSelectedPackage
        ? { backgroundColor: colors.mediumLightGrey }
        : {}
}

const PackageName = (props) => {
    const styles = {
        marginRight: '5px'
    }
    const { packageData } = props;
    if (PackageUtil.isPackageBuilt(packageData)) {
        return (
            <Tooltip content='Download' delay={0} tag='span'>
                <a href={packageData.path + '?' + packageData.timestamp} style={styles}>{packageData.downloadName}</a>
            </Tooltip>
        );
    } else {
        return <span style={styles}>{packageData.downloadName}</span>
    }
}

const PackageActionState = (props) => {
    const { state, type, cancelQueuedPackage, applicantId } = props;
    if (state === 'CHECKING') {
        return <CheckingState />
    } else if (state === 'QUEUED') {
        return <QueuedState action={type} cancelQueuedPackage={cancelQueuedPackage} applicantId={applicantId || ''} />
    } else if (state === 'RUNNING') {
        return <RunningState action={type} />
    }
}

const CheckingState = () => {
    return <PackageActionStateDetails><Spinner size='small' /></PackageActionStateDetails>
}

const QueuedState = (props) => {
    const { cancelQueuedPackage, applicantId } = props;
    const labels = {
        'BUILD': 'Build',
        'COVERAGE': 'Coverage',
        'INSTALL': 'Installation',
        'UNINSTALL': 'Uninstallation'
    }
    return <PackageActionStateDetails>
        <QueuedStateContainer>
            <Tooltip content={'by ' + applicantId} delay={0}>
                <div>{labels[props.action]} Queued</div>
            </Tooltip>
            <CancelButton onClick={cancelQueuedPackage} />
        </QueuedStateContainer>
    </PackageActionStateDetails>
}

const RunningState = (props) => {
    const labels = {
        'BUILD': 'Building...',
        'COVERAGE': 'Dumping coverage...',
        'INSTALL': 'Installing...',
        'UNINSTALL': 'Uninstalling...'
    }
    return (
        <PackageActionStateDetails>
            <span style={{ marginRight: '5px' }}>{labels[props.action]}</span>
            <Spinner size='small' />
        </PackageActionStateDetails>
    )
}

export default class PackageRow {

    constructor(props) {
        this.props = props;
    }

    onConsoleScroll(event, path) {
        const element = event.target;
        const isOnBottom = element.scrollHeight - element.scrollTop === element.clientHeight;
        if (!isOnBottom) {
            this.props.onConsoleScroll(path, isOnBottom);
        }
    }

    cancelQueuedPackage(path) {
        PackageService.cancelQueuedPackageAction(
            path,
            () => {
                this.props.updateActionState(path, { state: 'CHECKING', type: 'CANCEL' }, false);
            }
        );
    }

    render() {
        const row = this.props.data;

        const packageRow = (packageData) => {
            const selectedStyles = getSelectedStylesOrEmpty(packageData);
            const clickableStyles = { cursor: 'pointer' };
            return (
                [
                    <Cell
                        key='package'
                        className='name-cell'
                        style={{ ...selectedStyles, ...clickableStyles }}
                        onClick={() => {
                            if (!this.preventSelectingPackage) {
                                this.props.selectPackage(packageData.path);
                            }
                            this.preventSelectingPackage = false;
                        }}
                    >
                        <NameContainer style={{ cursor: 'pointer' }}>
                            <PackageThumbnail thumbnailData={packageData.thumbnail} />
                            <div>
                                <div>
                                    <PackageName packageData={packageData} />
                                    <Badge>{packageData.size ? packageData.size : 'new'}</Badge>
                                    <PackageInfo packageData={packageData} onClick={() => this.preventSelectingPackage = true} />
                                </div>
                                <PackageBuildDetails packageData={packageData} />
                            </div>
                        </NameContainer>
                    </Cell>,
                    <Cell
                        key='status'
                        singleLine
                        style={{ ...selectedStyles, ...clickableStyles }}
                        onClick={() => this.props.selectPackage(packageData.path)}
                    >
                        <PackageStatuses packageData={packageData} />
                    </Cell>,
                    <Cell
                        key='group'
                        singleLine
                        style={{ ...selectedStyles, ...clickableStyles }}
                        onClick={() => this.props.selectPackage(packageData.path)}
                    >
                        {packageData.group}
                    </Cell>,
                    <Cell
                        key='actions'
                        singleLine
                        style={{ ...selectedStyles, paddingRight: '0px' }}
                    >
                        {actions(packageData)}
                    </Cell>
                ]
            )
        }

        const consoleRow = (item) => (
            <div style={{ width: '100%' }}>
                <ActivityLogContainer>
                    <ResizableWrapper size={'270px'}>
                        <ResizableConsoleContent>
                            <ActivityLogConsole
                                id={'activity-log-console|' + item.path}
                                className='activity-log-console'
                                onScroll={(event) => this.onConsoleScroll(event, item.path)}
                            >
                                {item.logs.map((log, index) => <div key={index}>{log}</div>)}
                            </ActivityLogConsole>
                        </ResizableConsoleContent>
                    </ResizableWrapper>
                </ActivityLogContainer>
            </div>
        )

        const actionButtons = (packageData) => {
            return (
                <PackageActions
                    packageData={packageData}
                    groups={this.props.groups}
                    openConsoleForNewPackage={this.props.openConsoleForNewPackage}
                    refreshPage={this.props.refreshPage}
                    toggleConsole={this.props.toggleConsole}
                    updateActionState={this.props.updateActionState}
                />
            );
        }

        const actions = (packageData) => {
            const lastAction = packageData.lastAction || {};
            if (['CHECKING', 'QUEUED', 'RUNNING'].includes(lastAction.state)) {
                const actionState = (
                    <PackageActionState
                        {...lastAction}
                        cancelQueuedPackage={() => this.cancelQueuedPackage(packageData.path)}
                    />
                );
                return lastAction.state === 'QUEUED' ? actionState :
                    <Tooltip content={'by ' + lastAction.applicantId} delay={0}>
                        <div>{actionState}</div>
                    </Tooltip>;
            } else {
                return actionButtons(packageData);
            }
        }

        const content = !row.children && row.logs ? consoleRow(row) : packageRow(row);
        const hasPackageLogs = (row.children && row.children.length > 0)
            || (['QUEUED', 'RUNNING', 'FINISHED'].includes((row.lastAction || {}).state));

        return (
            <Row
                hasChildren={hasPackageLogs}
                items={row.children}
                isExpanded={row.isConsoleExpanded}
                onExpand={() => {
                    this.preventSelectingPackage = true;
                    this.props.toggleConsole(row.path, true);
                }}
                onCollapse={() => {
                    this.preventSelectingPackage = true;
                    this.props.toggleConsole(row.path, false);
                }}
                key={row.path}
            >
                {content}
            </Row>
        )
    }
}
