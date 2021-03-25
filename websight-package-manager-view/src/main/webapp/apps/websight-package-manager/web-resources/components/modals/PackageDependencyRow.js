import React from 'react';
import Button from '@atlaskit/button';
import styled from 'styled-components';
import { Draggable } from 'react-beautiful-dnd';

import InlineInput from 'websight-admin/components/InlineInput';
import { colors } from 'websight-admin/theme';

const RemoveDependencyButtonContainer = styled.div`
    padding-top: 12px;
`;

const RemoveDependencyIconContainer = styled.i`
    font-size: 20px;
    margin-top: 6px;
`;

const DependencyRowContainer = styled.div`
    display: flex;
`;

const DependencyInlineTextFieldContainer = styled.div`
    margin-right: 20px;
    flex: 1 1 auto;
`;

const MoveDependencyIcon = styled.i`
    margin-top: 8px;
    padding: 4px 0;
    color: ${colors.grey};
    visibility: hidden;
`;

const removeDependencyButtonStyle = {
    backgroundColor: 'white',
    padding: 0
};

export default class PackageDependencyRow extends React.Component {

    constructor(props) {
        super(props);

        this.state = {
            mouseOverRow: false
        }

        this.onMouseEnter = this.onMouseEnter.bind(this);
        this.onMouseLeave = this.onMouseLeave.bind(this);
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
        const { dependency, dependencyIndex } = this.props;

        return (
            <Draggable
                key={`dependency-${dependencyIndex}`}
                draggableId={`dependency-${dependencyIndex}`}
                index={dependencyIndex}
            >
                {(provided, snapshot) => (
                    <div
                        ref={provided.innerRef}
                        {...provided.draggableProps}
                    >
                        <DependencyRowContainer
                            onMouseEnter={this.onMouseEnter}
                            onMouseLeave={this.onMouseLeave}
                        >
                            <MoveDependencyIcon
                                {...provided.dragHandleProps}
                                className='material-icons'
                                style={snapshot.isDragging ? { visibility: 'visible' } :
                                    (this.state.mouseOverRow ? { visibility: 'visible' } : { visibility: 'hidden' })}
                            >
                                swap_vert
                            </MoveDependencyIcon>
                            <DependencyInlineTextFieldContainer>
                                <InlineInput
                                    value={dependency}
                                    onValueChange={this.props.onChange}
                                    placeholder='Pattern: group:name:version'
                                    maxWidth='540px'
                                />
                            </DependencyInlineTextFieldContainer>
                            <RemoveDependencyButtonContainer>
                                <Button
                                    style={removeDependencyButtonStyle}
                                    appearance='subtle'
                                    spacing='compact'
                                    onClick={this.props.onRemove}
                                    title='remove'>
                                    <RemoveDependencyIconContainer
                                        className='material-icons'
                                    >
                                        close
                                    </RemoveDependencyIconContainer>
                                </Button>
                            </RemoveDependencyButtonContainer>
                        </DependencyRowContainer>
                        {provided.placeholder}
                    </div>
                )}
            </Draggable>
        )
    }
}