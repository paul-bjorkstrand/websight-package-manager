import React from 'react';
import { Checkbox } from '@atlaskit/checkbox';
import { Fieldset } from '@atlaskit/form';
import Select from '@atlaskit/select';
import { DragDropContext, Droppable } from 'react-beautiful-dnd';
import styled from 'styled-components';
import { acHandlingSelectOptions } from '../../utils/PackageManagerConstants.js'

import { getOptionByValue, reorderCollection } from '../../utils/CommonUtils.js';
import { AddButton } from '../AddButton.js';
import PackageDependencyRow from './PackageDependencyRow.js';

const AdvancedTabContainer = styled.div`
    margin: 10px 0 20px;
    width: 100%;
`;

export default class PackageModalAdvancedTab {

    constructor(props) {
        this.props = props;
        this.addDependency = this.addDependency.bind(this);
        this.removeDependency = this.removeDependency.bind(this);
        this.updateDependency = this.updateDependency.bind(this);
        this.onDependencyDragEnd = this.onDependencyDragEnd.bind(this);
    }

    addDependency() {
        const updatedDependencies = [...this.props.dependencies, ''];
        this.props.updateDependencies(updatedDependencies);
    }

    removeDependency(indexToRemove) {
        const updatedDependencies = this.props.dependencies
            .filter((dependency, index) => index !== indexToRemove);
        this.props.updateDependencies(updatedDependencies)
    }

    updateDependency(indexToUpdate, newValue) {
        const updatedDependencies = this.props.dependencies
            .map((dependency, index) => index === indexToUpdate ? newValue : dependency);
        this.props.updateDependencies(updatedDependencies)
    }

    onDependencyDragEnd(result) {
        if (!result.destination) {
            return;
        }
        const sourceIndex = result.source.index;
        const destIndex = result.destination.index;
        this.props.updateDependencies(reorderCollection(this.props.dependencies, sourceIndex, destIndex));
    }

    render() {
        const { packageData } = this.props;

        return (
            <AdvancedTabContainer>
                <Checkbox
                    hideLabel={true}
                    label='Requires restart'
                    name='requiresRestart'
                    defaultValue={packageData.requiresRestart}
                    defaultChecked={packageData.requiresRestart}
                    value='true'
                />
                <Select
                    label='AC Handling'
                    placeholder=''
                    name='acHandling'
                    options={acHandlingSelectOptions}
                    defaultValue={getOptionByValue(packageData.acHandling, acHandlingSelectOptions)}
                    menuPortalTarget={document.body}
                    styles={{
                        menuPortal: base => ({
                            ...base,
                            zIndex: 9999
                        })
                    }}
                />
                <Fieldset legend='Dependencies'>
                    <DragDropContext onDragEnd={this.onDependencyDragEnd}>
                        <Droppable droppableId='package-droppable-dependencies'>
                            {(provided) => (
                                <div
                                    ref={provided.innerRef}
                                    {...provided.droppableProps}
                                >
                                    {this.props.dependencies.map((dependency, dependencyIndex) => (
                                        <PackageDependencyRow
                                            key={dependencyIndex}
                                            dependencyIndex={dependencyIndex}
                                            dependency={dependency}
                                            onChange={(data) => this.updateDependency(dependencyIndex, data)}
                                            onRemove={() => this.removeDependency(dependencyIndex)}
                                        />
                                    ))}
                                    {provided.placeholder}
                                </div>
                            )}
                        </Droppable>
                    </DragDropContext>
                    <AddButton
                        onClick={this.addDependency}
                        style={{ margin: '10px 0 0', padding: '5px 10px' }}
                    >
                        Add Dependency
                    </AddButton>
                </Fieldset>
            </AdvancedTabContainer>
        )
    }
}