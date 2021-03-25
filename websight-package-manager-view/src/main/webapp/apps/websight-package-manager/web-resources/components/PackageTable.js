import React from 'react';
import styled from 'styled-components';
import TableTree, { Header, Headers, Rows } from '@atlaskit/table-tree';

import { NoTableDataContainer } from 'websight-admin/Containers';
import { colors } from 'websight-admin/theme';
import { LoadingWrapper } from 'websight-admin/Wrappers';

import PackageRow from './PackageRow.js'

const TableRowsContainer = styled.div`
    .name-cell span[class^="styled__ChevronContainer"] {
        height: 100%;
        top: 0;
    }
    
    div[class^="styled__TreeRowContainer"][aria-level="1"] {
        &:hover {
            background-color: ${colors.veryLightGrey};
        } 
    }
`;

const preparePackages = (packages, selectedPackagePath) => {
    return (packages || []).map(packageData => {
        const logs = packageData.logs || [];
        const children = logs.length > 0 ? [{ logs: logs, path: packageData.path }] : [];
        return {
            ...packageData,
            id: packageData.path,   // 'id' will be used as a 'key' in actual row in a table
            children: children,
            isSelectedPackage: packageData.path === selectedPackagePath
        };
    });
}

const loadingWrapperSpinnerStyle = {
    top: '30vh',
    bottom: 'auto'
}

export default class PackageTable extends React.Component {

    render() {
        return (
            <TableTree>
                <Headers>
                    <Header width={'calc(80% - 335px)'}>Package</Header>
                    <Header width={'160px'}>Status</Header>
                    <Header width={'20%'}>Group</Header>
                    <Header width={'175px'}>Actions</Header>
                </Headers>
                <LoadingWrapper isLoading={this.props.isLoading} spinnerStyle={loadingWrapperSpinnerStyle} spinnerSize='large'>
                    <TableRowsContainer>
                        <Rows
                            items={preparePackages(this.props.packages, this.props.selectedPackagePath)}
                            render={(entry) => {
                                return (new PackageRow({
                                    data: entry,
                                    groups: this.props.groups,
                                    toggleConsole: this.props.toggleConsole,
                                    onConsoleScroll: this.props.onConsoleScroll,
                                    openConsoleForNewPackage: this.props.openConsoleForNewPackage,
                                    refreshPage: this.props.refreshPage,
                                    selectPackage: this.props.selectPackage,
                                    updateActionState: this.props.updateActionState,
                                    extraActions: this.props.extraActions
                                }).render());
                            }}
                        />
                    </TableRowsContainer>
                    {this.props.packages.length === 0 && this.props.isInitialized &&
                    <NoTableDataContainer>
                        <h3>No packages found.</h3>
                    </NoTableDataContainer>
                    }
                </LoadingWrapper>
            </TableTree>
        )
    }
}
