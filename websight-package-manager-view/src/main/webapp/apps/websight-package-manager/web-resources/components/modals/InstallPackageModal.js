import React from 'react';
import Select from '@atlaskit/select';
import Form, { FormFooter } from 'websight-rest-atlaskit-client/Form';
import Button, { ButtonGroup } from '@atlaskit/button';
import ModalDialog, { ModalTransition } from '@atlaskit/modal-dialog';
import { Fieldset } from '@atlaskit/form';
import Panel from '@atlaskit/panel';
import { Checkbox } from '@atlaskit/checkbox';
import styled from 'styled-components';


import { acHandlingSelectOptions } from '../../utils/PackageManagerConstants.js'

const SettingsContainer = styled.span`
    display: block;
    left: 15px;
    width: 97%;
    position: relative;
    tabIndex: 0;
`;

export default class InstallPackageModal extends React.PureComponent {
    constructor(props) {
        super(props);

        this.state = {
            isOpen: false,
            isExtractSubpackages: true
        }

        this.open = this.open.bind(this);
        this.close = this.close.bind(this);
        this.onFormSubmit = this.onFormSubmit.bind(this);
    }

    open() {
        this.setState({ isOpen: true });
    }

    close() {
        this.setState({ isOpen: false });
    }

    onFormSubmit(requestData, packageData) {
        this.props.onSubmit(packageData.path, requestData);
    }

    render() {
        const { isOpen } = this.state;

        return (
            <ModalTransition>
                {isOpen && (
                    <>
                        <ModalDialog
                            onClose={this.close}
                            heading={this.props.isPackageInstalled ? 'Reinstall' : 'Install' + 'package'}
                        >
                            <>
                                Do you really want to {this.props.isPackageInstalled ? 'reinstall' : 'install'} this
                                package?<br /><br />
                                <b>{this.props.packageData.downloadName}</b>
                            </>
                            <Form
                                onSubmit={(data) => this.onFormSubmit(data, this.props.packageData)}
                                onSuccess={this.close}
                            >
                                {({ submitted }) => (
                                    <>
                                        <SettingsContainer>
                                            <Panel
                                                header="Advanced"
                                                isDefaultExpanded={false}>
                                                <Fieldset>
                                                    <Select
                                                        label='AC Handling'
                                                        placeholder=''
                                                        name='acHandling'
                                                        options={acHandlingSelectOptions}
                                                        menuPortalTarget={document.body}
                                                        styles={{
                                                            menuPortal: base => ({
                                                                ...base,
                                                                zIndex: 9999
                                                            })
                                                        }}
                                                    />
                                                    <Checkbox
                                                        isChecked={this.state.isExtractSubpackages}
                                                        defaultChecked={true}
                                                        hideLabel={true}
                                                        label='Extract Subpackages'
                                                        name='extractSubpackages'
                                                        onChange={(event) => this.setState({ isExtractSubpackages: event.target.checked })}
                                                    />
                                                </Fieldset>
                                            </Panel>
                                        </SettingsContainer>
                                        <FormFooter>
                                            <ButtonGroup>
                                                <Button
                                                    autoFocus={true}
                                                    appearance='primary'
                                                    type='submit'
                                                    isLoading={submitted}
                                                >
                                                    Install
                                                </Button>
                                                <Button
                                                    appearance='subtle'
                                                    onClick={this.close}
                                                    isDisabled={submitted}
                                                >
                                                    Cancel
                                                </Button>
                                            </ButtonGroup>
                                        </FormFooter>
                                    </>
                                )}
                            </Form>
                        </ModalDialog>
                    </>
                )}
            </ModalTransition>
        );
    }
}
