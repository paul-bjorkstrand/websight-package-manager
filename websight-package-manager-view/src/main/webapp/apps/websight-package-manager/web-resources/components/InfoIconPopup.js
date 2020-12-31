import React from 'react';
import Popup from '@atlaskit/popup';
import styled from 'styled-components';

import { colors } from 'websight-admin/theme';

const PopupContainer = styled.div`
    display: inline-block;

    & > div {
        display: inline-block;
    }
`;

const ContentContainer = styled.div`
    max-width: 500px;
    max-height: 300px;
    margin: 10px;
`;

const InfoIcon = styled.i`
    font-size: 21px;
    margin-left: 5px;
    color: ${colors.grey};
    cursor: pointer;
    vertical-align: bottom;
`;

export default class InfoIconPopup extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            isOpen: false
        }
    }

    render() {
        const { content, icon, iconStyle, onClick } = this.props;
        const { isOpen } = this.state;

        return (
            <PopupContainer>
                <Popup
                    isOpen={isOpen}
                    onClose={() => this.setState({ isOpen: false })}
                    placement='bottom-start'
                    content={() => <ContentContainer>{content}</ContentContainer>}
                    trigger={triggerProps => (
                        <div {...triggerProps} style={{ display: 'inline' }}>
                            <InfoIcon
                                className='material-icons'
                                style={iconStyle}
                                onClick={() => {
                                    onClick();
                                    this.setState((prevState) => ({ isOpen: !prevState.isOpen }));
                                }}
                            >
                                {icon}
                            </InfoIcon>
                        </div>
                    )}
                />
            </PopupContainer>
        );
    }
}
