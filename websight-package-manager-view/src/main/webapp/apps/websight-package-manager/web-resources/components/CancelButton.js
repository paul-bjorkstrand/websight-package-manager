import React from 'react';
import Tooltip from '@atlaskit/tooltip';
import styled from 'styled-components';

import { colors } from 'websight-admin/theme';

const Container = styled.button`
    border: none;
    display: flex;
    background-color: inherit;
    cursor: pointer;
    align-items: center;
    text-align: center;
    padding: 0px;
    border-radius: 50%;
    outline: none;
`;

const Icon = styled.i`
    color: ${colors.grey}; 
    font-size: 16px;
    padding: 0 4px;

    &:hover {
        color: ${colors.black};
    }
`;

export class CancelButton extends React.Component {
    render() {
        return (
            <Container onClick={this.props.onClick} >
                <Tooltip content={'Cancel'} delay={0}>
                    <Icon className='material-icons'>cancel</Icon>
                </Tooltip>
            </Container >
        );
    }
}