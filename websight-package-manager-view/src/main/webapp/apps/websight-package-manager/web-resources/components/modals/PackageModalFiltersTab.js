import React from 'react';
import { DragDropContext, Droppable } from 'react-beautiful-dnd';
import styled from 'styled-components';

import { reorderCollection } from '../../utils/CommonUtils.js';
import { AddButton } from '../AddButton.js';
import PackageFilterRow from './PackageFilterRow.js';

const FiltersTabContainer = styled.div`
    margin: 10px 0 20px;
    width: 100%;
`;

const emptyFilter = {
    root: '/',
    mode: 'replace',
    rules: []
};

export default class PackageModalFiltersTab {

    constructor(props) {
        this.props = props;
        this.addFilter = this.addFilter.bind(this);
        this.updateFilter = this.updateFilter.bind(this);
        this.removeFilter = this.removeFilter.bind(this);
        this.onDragEnd = this.onDragEnd.bind(this);
    }

    addFilter() {
        const updatedFilters = [...this.props.filters, emptyFilter];
        this.props.updateFilters(updatedFilters)
    }

    updateFilter(indexToUpdate, filterData) {
        const updatedFilters = this.props.filters
            .map((filter, index) => index === indexToUpdate ? filterData : filter);
        this.props.updateFilters(updatedFilters)
    }

    removeFilter(indexToRemove) {
        const updatedFilters = this.props.filters
            .filter((filter, index) => index !== indexToRemove);

        this.props.updateFilters(updatedFilters)
    }

    onDragEnd(result) {
        if (!result.destination) {
            return;
        }

        const sourceIndex = result.source.index;
        const destIndex = result.destination.index;
        if (result.type === 'filter') {
            this.props.updateFilters(reorderCollection(this.props.filters, sourceIndex, destIndex));
        } else if (result.type === 'rule') {
            const sourceParentId = parseInt(result.source.droppableId);
            const destParentId = parseInt(result.destination.droppableId);

            let newFilters = [...this.props.filters];
            const sourceSubItems = this.props.filters[sourceParentId].rules;

            if (sourceParentId === destParentId) {
                const reorderedRules = reorderCollection(
                    sourceSubItems,
                    sourceIndex,
                    destIndex
                );
                newFilters = newFilters.map((filter, filterIndex) => {
                    if (filterIndex === sourceParentId) {
                        filter.rules = reorderedRules;
                    }
                    return filter;
                });
                this.props.updateFilters(newFilters);
            }
        }
    }

    render() {
        return (
            <FiltersTabContainer>
                <DragDropContext onDragEnd={this.onDragEnd}>
                    <Droppable droppableId='package-droppable-filters' type='filter'>
                        {(provided) => (
                            <div
                                ref={provided.innerRef}
                                {...provided.droppableProps}
                            >
                                {this.props.filters.map((filter, filterIndex) => (
                                    <PackageFilterRow
                                        key={filterIndex}
                                        filterIndex={filterIndex}
                                        filter={filter}
                                        onChange={(filterData) => this.updateFilter(filterIndex, filterData)}
                                        onRemove={() => this.removeFilter(filterIndex)}
                                    />
                                ))}
                                {provided.placeholder}
                            </div>
                        )}
                    </Droppable>
                </DragDropContext>
                <AddButton
                    onClick={this.addFilter}
                    style={{ margin: '10px 0 0', padding: '5px 10px' }}
                >
                    Add Filter
                </AddButton>
            </FiltersTabContainer>
        )
    }
}