import React from 'react';
import styled from 'styled-components';

import { colors } from 'websight-admin/theme';
import { DateTimeOffset } from 'websight-admin/DateUtils';
import InfoIconPopup from './InfoIconPopup.js';

const DependenciesList = styled.ul`
    padding-left: 20px;
    margin-top: 6px;
`;

const Dependency = styled.li`
    font-size: 12px;
`;

const UnresolvedDependency = styled.li`
    color: ${colors.red};
    font-size: 12px;
`;

const Description = styled.span`
    font-size: 12px;
    word-wrap: break-word;
`;

const PackageDefinitionInfo = (props) => {
    const { packageData, onClick }  = props;
    let dependencies;
    let description;
    let hasUnresolvedDependencies = false;
    if (packageData.dependencies && packageData.dependencies.length > 0) {
        dependencies = (
            <div>
                <span>Dependencies</span>
                <DependenciesList>
                    {packageData.dependencies.map((dependency, index) => (
                        (dependency.resolved ?
                            <Dependency key={index}>{dependency.dependency}</Dependency>
                            :
                            <UnresolvedDependency key={index}>{dependency.dependency} (unresolved)</UnresolvedDependency>)
                    ))}
                </DependenciesList>
            </div>
        )
        hasUnresolvedDependencies = packageData.dependencies.find(dependency => !dependency.resolved);
    }
    if (packageData.description) {
        description = (
            <div>
                {dependencies && (
                    <>
                        <br />
                        <span>Description</span><br />
                    </>
                )}
                <Description>{packageData.description}</Description>
            </div>
        );
    }
    if (dependencies || description) {
        return (
            <InfoIconPopup
                content={<>{dependencies}{description}</>}
                icon='info_outline'
                iconStyle={{ color: hasUnresolvedDependencies ? colors.red : undefined }}
                onClick={onClick}
            />
        );
    } else {
        return null;
    }
}

const PackageScheduleInfo = (props) => {
    const { packageData, onClick }  = props;
    if (packageData.nextScheduledAction) {
        const nextScheduledAction = packageData.nextScheduledAction;
        const actionName = nextScheduledAction.action && nextScheduledAction.action.toLowerCase();
        const executionDateTimeOffset = new DateTimeOffset(nextScheduledAction.nextExecution);
        const executionTime = executionDateTimeOffset.getEnGbDateTimeOffsetString();
        return (
            <InfoIconPopup
                content={<Description>Next {actionName} scheduled at {executionTime} by {nextScheduledAction.applicantId}</Description>}
                icon='schedule'
                onClick={onClick}
            />
        );
    }
    return null;
}

const PackageInfo = (props) => {
    return (
        <>
            <PackageDefinitionInfo {...props} />
            <PackageScheduleInfo {...props} />
        </>
    );
}

export default PackageInfo;
