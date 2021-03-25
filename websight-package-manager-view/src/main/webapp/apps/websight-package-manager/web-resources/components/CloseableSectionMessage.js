import React from 'react';
import SectionMessage from '@atlaskit/section-message';
import Tooltip from '@atlaskit/tooltip';
import styled from 'styled-components';

const Icon = styled.i`
    font-size: 18px;
    padding-left: 16px;
    cursor: pointer;
`;

const MessageContainer = styled.div`
    display: flex;
    justify-content: space-between;
`;

export default class CloseableSectionMessage extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            isClosed: false
        }
        this.onClose = this.onClose.bind(this)
    }

    componentDidMount() {
        const localStorageClosedValue = localStorage.getItem(this.props.id + '-closed');
        if (localStorageClosedValue === 'true') {
            this.setState({ isClosed: true });
        }
    }

    onClose() {
        this.setState({ isClosed: true }, () => {
            localStorage.setItem(this.props.id + '-closed', true);
        });
    }

    render() {
        const { isClosed } = this.state;
        return (
            <>
                {!isClosed && (
                    <SectionMessage {...this.props}>
                        <MessageContainer>
                            {this.props.children}
                            <Tooltip content='Close' delay={0}>
                                <Icon className='material-icons' onClick={this.onClose}>close</Icon>
                            </Tooltip>
                        </MessageContainer>
                    </SectionMessage>
                )}
            </>
        );
    }
}