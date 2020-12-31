import React from 'react';
import Button from '@atlaskit/button';
import { Checkbox } from '@atlaskit/checkbox';
import styled from 'styled-components';
import { Draggable } from 'react-beautiful-dnd';

import InlineInput from 'websight-admin/components/InlineInput';
import { colors } from 'websight-admin/theme';

const CheckboxContainer = styled.div`
    flex: 0 0 100px;
    margin-top: 2px;
`;

const CheckboxIcon = styled.i`
    font-size: 18px;
    padding: 3px;
`;

const CloseButtonContainer = styled.div`
    padding-top: 2px;
`;

const FilterRuleRowContainer = styled.div`
    display: flex;
    margin: 0 0 5px 30px;
`;

const FilterInlineTextFieldContainer = styled.div`
    margin: -10px 20px 0;
    flex: 1 1 auto;
`;

const MoveRuleIcon = styled.i`
    padding-top: 2px;
    color: ${colors.grey};
    visibility: hidden;
`;

const RemoveIconContainer = styled.i`
    font-size: 20px;
    margin-top: 6px;
`;

const ruleRemoveButtonStyle = {
    backgroundColor: 'white',
    marginRight: '30px',
    padding: 0
};

const checkboxLabelCss = () => {
    return (currentStyles) => {
        return {
            ...currentStyles,
            cursor: 'pointer'
        }
    };
}

const ruleTypes = [
    { label: 'include', value: true },
    { label: 'exclude', value: false }
];

export default class PackageFilterRuleRow extends React.PureComponent {
    constructor(props) {
        super(props);

        this.state = {
            mouseOverRow: false
        }

        this.onMouseEnter = this.onMouseEnter.bind(this);
        this.onMouseLeave = this.onMouseLeave.bind(this);
    }

    shouldComponentUpdate(nextProps, nextState) {
        return !(this.state.mouseOverRow === nextState.mouseOverRow
            && this.props.rule.path === nextProps.rule.path
            && this.props.rule.include === nextProps.rule.include);
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

    getCheckboxTheme(tokens, rule) {
        const setColor = (colorsObject, color) => {
            Object.keys(colorsObject).forEach((key) => colorsObject[key] = color);
        }

        const setColors = (borderColor, boxColor, tickColor) => {
            setColor(tokens.icon.borderColor, borderColor);
            setColor(tokens.icon.boxColor, boxColor);
            setColor(tokens.icon.tickColor, tickColor);
        }

        if (rule.include) {
            setColors(colors.lightGreen, colors.lightGreen, colors.white);
        } else {
            setColors(colors.lightRed, colors.lightRed, colors.white);
        }
        return tokens;
    }

    render() {
        const { rule, ruleIndex, filterIndex } = this.props;

        return (
            <Draggable
                key={`filter-${filterIndex}-rule-${ruleIndex}`}
                draggableId={`filter-${filterIndex}-rule-${ruleIndex}`}
                index={ruleIndex}
            >
                {(provided, snapshot) => (
                    <div
                        ref={provided.innerRef}
                        {...provided.draggableProps}
                    >
                        <FilterRuleRowContainer
                            onMouseEnter={this.onMouseEnter}
                            onMouseLeave={this.onMouseLeave}
                        >
                            <MoveRuleIcon
                                {...provided.dragHandleProps}
                                className='material-icons'
                                style={snapshot.isDragging ? { visibility: 'visible' } :
                                    (this.state.mouseOverRow ? { visibility: 'visible' } : { visibility: 'hidden' })}
                            >
                                swap_vert
                            </MoveRuleIcon>
                            <CheckboxContainer>
                                <Checkbox
                                    isChecked={rule.include}
                                    label={ruleTypes.find(({ value }) => value === rule.include).label}
                                    isIndeterminate={!rule.include}
                                    onChange={event => this.props.onRuleChange({ include: event.currentTarget.checked })}
                                    defaultChecked={rule.include}
                                    overrides={{
                                        Icon: {
                                            component: () => (
                                                <CheckboxIcon className='material-icons'>
                                                    add_box
                                                </CheckboxIcon>
                                            )
                                        },
                                        Label: {
                                            cssFn: checkboxLabelCss()
                                        }
                                    }}
                                    theme={(current, props) => this.getCheckboxTheme(current(props), rule)}
                                />
                            </CheckboxContainer>
                            <FilterInlineTextFieldContainer>
                                <InlineInput
                                    value={rule.path}
                                    onValueChange={value => this.props.onRuleChange({ path: value })}
                                    placeholder='Filter rule pattern'
                                    maxWidth='350px'
                                />
                            </FilterInlineTextFieldContainer>
                            <CloseButtonContainer>
                                <Button
                                    spacing='compact'
                                    style={ruleRemoveButtonStyle}
                                    onClick={() => this.props.removeRule(ruleIndex)}
                                    title='remove'>
                                    <RemoveIconContainer className='material-icons'>close</RemoveIconContainer>
                                </Button>
                            </CloseButtonContainer>
                        </FilterRuleRowContainer>
                        {provided.placeholder}
                    </div>
                )}
            </Draggable>
        );
    }
}
