import React from 'react';
import Tooltip from '@atlaskit/tooltip';
import styled from 'styled-components';

import { colors } from 'websight-admin/theme';
import { DateTimeOffset, isSameDay } from 'websight-admin/DateUtils';

const BuildDetails = styled.div`
    font-size: 12px;
    color: ${colors.darkGrey};
`;

const buildPackageBuildDetails = (entry) => {
    let details = '';
    const version = entry.packageInfo ? entry.packageInfo.versionString : null;
    if (version) {
        details += ('Version: ' + version + ' | ');
    }
    const buildsCount = entry.buildCount;
    if (buildsCount) {
        details += ('Build #' + buildsCount + ' | ');
    }
    return details;
}

const getActionInfo = (entry) => {
    if (entry.status && entry.status.installation) {
        return <ActionInfo
            name='Installed'
            dateString={entry.status.installation.date}
            executedBy={entry.status.installation.executedBy}
        />;
    } else if (entry.status && entry.status.build) {
        return <ActionInfo
            name='Built'
            dateString={entry.status.build.date}
            executedBy={entry.status.build.executedBy}
        />;
    } else if (entry.modification) {
        return <ActionInfo
            name='Modified'
            dateString={entry.modification.date}
            executedBy={entry.modification.executedBy}
        />;
    }
}

const ActionInfo = ({ name: actionName, dateString, executedBy }) => {
    const actionDateTimeOffset = new DateTimeOffset(dateString);
    return <>
        <Tooltip tag='span' content={`Server time: ${actionDateTimeOffset.getEnGbDateTimeOffsetString()}`}>
            <span>{`${actionName} ${formatActionDate(actionDateTimeOffset)}`}</span>
        </Tooltip>
        {' | ' + executedBy}
    </>;
}

const formatActionDate = (actionDateTimeOffset) => {
    const now = new Date();
    const actionDate = new Date(actionDateTimeOffset.iso8601DateTimeOffsetString)
    if (isSameDay(actionDate, now)) {
        return `${actionDateTimeOffset.hours}:${actionDateTimeOffset.minutes}`;
    }
    if (actionDate.getFullYear() === now.getFullYear()) {
        return `${actionDateTimeOffset.shortMonth} ${actionDateTimeOffset.day}`;
    }
    return actionDateTimeOffset.getEnGbDateString();
}

export default class PackageBuildDetails extends React.Component {
    render() {
        const { packageData } = this.props;
        const packageBuildDetails = buildPackageBuildDetails(packageData);
        const buildInfo = getActionInfo(packageData);
        return (
            <BuildDetails>
                {packageBuildDetails}
                {buildInfo}
            </BuildDetails>
        );
    }
}
