import React from 'react';
import Button from '@atlaskit/button';
import Select from '@atlaskit/select';
import styled from 'styled-components';
import { Draggable, Droppable } from 'react-beautiful-dnd';

import { colors } from 'websight-admin/theme'
import PathAutosuggestion from 'websight-autosuggestion-esm/PathAutosuggestion';

import { AddButton } from '../AddButton.js';
import PackageFilterRuleRow from './PackageFilterRuleRow.js';

const FilterContainer = styled.div`
    margin-bottom: 20px;
    border-bottom: 1px solid ${colors.veryLightGrey};
    width: 100%;
`;

const FilterRowContainer = styled.div`
    display: flex;
    margin: 0 0 10px;
`;

const FilterSelectContainer = styled.div`
    flex: 0 0 100px;
`;

const FilterPathAutosuggestionContainer = styled.div`
    background: ${colors.white};
    margin: 0 20px 0;
    flex: 1 1 auto;

    div > div > div:last-child > div > span {
        visibility: hidden
    }
`;

const RemoveIconContainer = styled.i`
    font-size: 20px;
    margin-top: 6px;
`;

const RemoveButtonContainer = styled.div`
    padding-top: 4px;
`;

const MoveFilterIcon = styled.i`
    padding: 4px 0;
    color: ${colors.grey};
    visibility: hidden;
`;

const filterAutosuggestionStyle = {
    backgroundColor: colors.white,
    borderColor: colors.white,
    color: colors.black
}

const filterRemoveButtonStyle = {
    backgroundColor: colors.white,
    marginRight: '30px',
    padding: 0
};

const filterStyle = (draggableStyle) => ({
    display: 'flex',
    ...draggableStyle
})

const emptyRule = {
    path: '',
    include: true
};

const filterModes = ['replace', 'merge', 'update']
    .map((entry) => ({ label: entry, value: entry }));

export default class PackageFilterRow extends React.PureComponent {
    constructor(props) {
        super(props);

        this.state = {
            mouseOverRow: false
        }

        this.addRule = this.addRule.bind(this);
        this.removeRule = this.removeRule.bind(this);
        this.onMouseEnter = this.onMouseEnter.bind(this);
        this.onMouseLeave = this.onMouseLeave.bind(this);
        this.onRuleChange = this.onRuleChange.bind(this);
    }

    shouldComponentUpdate(nextProps, nextState) {
        return !(JSON.stringify(this.state) === JSON.stringify(nextState)
            && JSON.stringify(this.props.filter) === JSON.stringify(nextProps.filter)
            && this.props.filterIndex === nextProps.filterIndex);
    }

    onChange(filterData) {
        this.props.onChange({ ...this.props.filter, ...filterData })
    }

    onRuleChange(indexToUpdate, newData) {
        const updatedRules = this.props.filter.rules
            .map((rule, index) => {
                if (index === indexToUpdate) {
                    return { ...rule, ...newData };
                }
                return rule;
            });

        this.props.onChange({ ...this.props.filter, rules: updatedRules });
    }

    addRule() {
        this.props.onChange({ ...this.props.filter, rules: [...this.props.filter.rules, emptyRule] });
    }

    removeRule(indexToRemove) {
        const updatedRules = this.props.filter.rules
            .filter((rule, index) => index !== indexToRemove);

        this.props.onChange({ ...this.props.filter, rules: updatedRules });
    }


    onMouseEnter() {
        if (!this.state.mouseOverRow) {
            this.setState({ mouseOverRow: true });
        }
    }

    onMouseLeave() {
        if (this.state.mouseOverRow) {
            this.setState({ mouseOverRow: false });
        }
    }

    render() {
        const { filter, filterIndex } = this.props;

        return (
            <Draggable key={`filter-${filterIndex}`} draggableId={`filter-${filterIndex}`} index={filterIndex}>
                {(provided, snapshot) => (
                    <div
                        ref={provided.innerRef}
                        {...provided.draggableProps}
                        style={filterStyle(provided.draggableProps.style)}
                    >
                        <FilterContainer>
                            <FilterRowContainer
                                onMouseEnter={this.onMouseEnter}
                                onMouseLeave={this.onMouseLeave}
                            >
                                <MoveFilterIcon
                                    {...provided.dragHandleProps}
                                    className='material-icons'
                                    style={snapshot.isDragging ? { visibility: 'visible' } :
                                        (this.state.mouseOverRow ? { visibility: 'visible' } : { visibility: 'hidden' })}
                                >
                                    swap_vert
                                </MoveFilterIcon>
                                <FilterSelectContainer>
                                    <Select
                                        spacing='compact'
                                        options={filterModes}
                                        value={filterModes.find(({ value }) => value === filter.mode)}
                                        onChange={(filterMode) => this.onChange({ mode: filterMode.value })}
                                        defaultValue={filterModes[0]}
                                    />
                                </FilterSelectContainer>
                                <FilterPathAutosuggestionContainer>
                                    <PathAutosuggestion
                                        noOptionsMessage={(inputValue) => `No resource found for "${inputValue}"`}
                                        onChange={(value) => this.onChange({ root: value })}
                                        parameters={{ autosuggestionType: 'jcr-path' }}
                                        placeholder='Choose a filter root path (eg: /home)'
                                        styles={filterAutosuggestionStyle}
                                        value={filter.root}
                                    />
                                </FilterPathAutosuggestionContainer>
                                <RemoveButtonContainer>
                                    <Button
                                        style={filterRemoveButtonStyle}
                                        appearance='subtle'
                                        spacing='compact'
                                        onClick={this.props.onRemove}
                                        title='remove'>
                                        <RemoveIconContainer className='material-icons'>close</RemoveIconContainer>
                                    </Button>
                                </RemoveButtonContainer>
                            </FilterRowContainer>
                            <Droppable droppableId={`${filterIndex}`} type='rule'>
                                {(providedDroppable) => (
                                    <div
                                        ref={providedDroppable.innerRef}
                                        {...providedDroppable.droppableProps}
                                    >
                                        {(filter.rules || []).map((rule, ruleIndex) => {
                                            return <PackageFilterRuleRow
                                                key={ruleIndex}
                                                rule={rule}
                                                ruleIndex={ruleIndex}
                                                filterIndex={filterIndex}
                                                onRuleChange={data => this.onRuleChange(ruleIndex, data)}
                                                removeRule={this.removeRule}
                                            />
                                        })}
                                        {providedDroppable.placeholder}
                                    </div>
                                )}
                            </Droppable>
                            <AddButton
                                onClick={this.addRule}
                            >
                                Add Rule
                            </AddButton>
                        </FilterContainer>
                        {provided.placeholder}
                    </div>
                )}
            </Draggable>
        );
    }
}