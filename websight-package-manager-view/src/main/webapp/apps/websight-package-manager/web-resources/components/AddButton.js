import React from 'react';
import styled from 'styled-components';

import { colors } from 'websight-admin/theme';

const AddButtonTemplate = styled.button`
    background: ${colors.veryLightGrey};
    border: 1px solid ${colors.veryLightGrey};
    border-radius: 3px;
    color: ${colors.veryDarkGrey};
    cursor: pointer;
    font-size: 14px;
    margin: 0px 0 10px 30px;
    padding: 2px 8px;

    &:hover,
    &:active {
        background: ${colors.mediumLightGrey};
        border: 1px solid ${colors.mediumLightGrey};
    }

    &:focus {
        outline:0;
    }
`;

export const AddButton = (props) => {
    return (
        <AddButtonTemplate
            type='button'
            {...props}
        />
    )
}