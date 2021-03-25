import React from 'react';
import { Container, Format } from '@atlaskit/badge';
import { ContainerHeader, GroupHeading, HeaderSection, Item, MenuSection } from '@atlaskit/navigation-next';
import styled from 'styled-components';

import { AvatarIcon } from 'websight-admin/Icons';
import { colors } from 'websight-admin/theme';
import { LoadingWrapper } from 'websight-admin/Wrappers';
import StatefulFilterInput from 'websight-admin/components/StatefulFilterInput';

import { PACKAGE_MANAGER_ROOT_PATH } from '../utils/PackageManagerConstants.js';

const GroupFilterInputContainer = styled.div`
    margin-bottom: 6px;
`;

const GroupsNotFoundContainer = styled.div`
    text-align: center;
    margin-top: 20px;
`;

const createBadge = (limitExceeded, count, bold) => {
    const badgeContent = <Format max={limitExceeded ? count - 1 : null}>{count}</Format>;
    return (
        <Container
            backgroundColor={limitExceeded ? colors.red : colors.primaryBlue}
            textColor={colors.white}>
            {bold ? <b>{badgeContent}</b> : badgeContent}
        </Container>
    )
};

export default class ContainerNavigation extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            groupNameFilter: this.props.groupNameFilter
        };
    }

    render() {
        const groupNameFilter = this.state.groupNameFilter;
        const groupNameFilterNotEmpty = groupNameFilter && groupNameFilter.length > 0;
        const groupsToDisplay = (this.props.groups || [])
            .filter(group => groupNameFilterNotEmpty ?
                group.name.toLowerCase().includes(groupNameFilter.toLocaleLowerCase()) : !group.name.includes('/'));
        return (
            <>
                <HeaderSection>
                    {({ css }) => (
                        <div style={{ ...css, paddingBottom: 20 }}>
                            <ContainerHeader
                                before={() => (
                                    <AvatarIcon className='material-icons'>
                                        dvr
                                    </AvatarIcon>
                                )}
                                href={PACKAGE_MANAGER_ROOT_PATH}
                                text='Package Manager'
                            />
                        </div>
                    )}
                </HeaderSection>

                <MenuSection>
                    {({ className }) => (
                        <div className={className}>
                            <GroupHeading>Groups</GroupHeading>
                            <GroupFilterInputContainer>
                                <StatefulFilterInput
                                    value={groupNameFilter}
                                    onChange={(event) => {
                                        const value = event.target.value;
                                        this.props.onGroupNameFilterChange(value);
                                        this.setState({ groupNameFilter: value });
                                    }}
                                    onClear={() => {
                                        this.props.onGroupNameFilterChange('');
                                        this.setState({ groupNameFilter: '' });
                                    }}
                                />
                            </GroupFilterInputContainer>
                            <LoadingWrapper
                                isLoading={this.props.isLoadingGroups}
                                spinnerStyle={{ top: '30vh', bottom: 'auto' }}
                                spinnerSize='large'>
                                {((!groupNameFilter || groupNameFilter.length === 0) &&
                                    <Item
                                        text={<b>All packages</b>}
                                        isSelected={!this.props.selected}
                                        after={() => createBadge(this.props.limitExceeded, this.props.allPackagesCount, true)}
                                        onClick={() => this.props.findPackages({ group: null, pageNumber: 1 })}
                                    />
                                )}
                                {groupsToDisplay && groupsToDisplay.length > 0 ?
                                    groupsToDisplay
                                        .map(group => (
                                            <Item
                                                key={group.name}
                                                text={group.name === ':no_group' ? <i>No group</i> : group.name}
                                                isSelected={group.name === this.props.selected}
                                                after={() => createBadge(group.limitExceeded, group.count, false)}
                                                onClick={() => this.props.findPackages({ group: group.name, pageNumber: 1 })}
                                            />
                                        ))
                                    :
                                    <GroupsNotFoundContainer>
                                        No groups found
                                    </GroupsNotFoundContainer>
                                }
                            </LoadingWrapper>
                        </div>
                    )}
                </MenuSection>
            </>
        )
    }
}
