import React from 'react';
import { colors } from '@atlaskit/theme';
import Tooltip from '@atlaskit/tooltip';

import { DateTimeOffset } from 'websight-admin/DateUtils';

import * as PackageUtil from '../utils/PackageUtil.js';

const statusStyle = {
    borderRadius: '3px',
    border: '1px solid',
    borderColor: colors.N40,
    display: 'inline-block',
    fontSize: '12px',
    margin: '0 5px 1px 0',
    lineHeight: '16px',
    padding: '0 4px'
};

const defaultStatusStyle = {
    ...statusStyle,
    color: colors.G500
};

const warningStatusStyle = {
    ...statusStyle,
    color: colors.DN30,
    backgroundColor: colors.Y100,
    border: 'none'
};

const NeverBuild = () => {
    return <div style={warningStatusStyle}>Never built</div>
}

const Installed = (props) => {
    return (
        <Tooltip content={props.description}>
            <div style={defaultStatusStyle}>Installed</div>
        </Tooltip>
    )
}

const NotInstalled = () => {
    return <div style={warningStatusStyle}>Not installed</div>
}

const Modified = (props) => {
    return (
        <Tooltip content={props.description}>
            <div style={warningStatusStyle}>Modified</div>
        </Tooltip>
    )
}

const statusDescription = (status) => {
    return new DateTimeOffset(status.date).getEnGbDateTimeOffsetString() + ' by ' + status.executedBy;
}

export default class PackageStatuses extends React.Component {

    render() {
        const { packageData } = this.props;
        const { status = {} } = packageData;

        let buildStatus;
        let installationStatus;
        let modificationStatus;

        if (!PackageUtil.isPackageBuilt(packageData)) {
            buildStatus = <NeverBuild/>
        } else {
            if (PackageUtil.isPackageInstalled(packageData)) {
                installationStatus = <Installed description={statusDescription(status.installation)}/>
            } else {
                installationStatus = <NotInstalled/>
            }
            if (PackageUtil.isPackageModified(packageData)) {
                modificationStatus = <Modified description={statusDescription(status.modification)}/>
            }
        }

        return (
            <>
                {buildStatus}
                {installationStatus}
                {modificationStatus}
            </>
        );
    }
}
