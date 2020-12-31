import React from 'react';
import Button, { ButtonGroup } from '@atlaskit/button';
import { Checkbox } from '@atlaskit/checkbox';
import ModalDialog, { ModalTransition } from '@atlaskit/modal-dialog';

import ConfirmationModal from 'websight-admin/ConfirmationModal';
import FilePicker from 'websight-admin/FilePicker';
import Form, { FormFooter } from 'websight-rest-atlaskit-client/Form';

import PackageService from '../../services/PackageService.js';

const ACTION_UPLOAD = 'upload';
const ACTION_UPLOAD_AND_INSTALL = 'upload-and-install';

export default class UploadPackageModal extends React.Component {

    constructor(props) {
        super(props);

        this.state = {
            isOpen: false,
            fileName: null
        };

        this.open = this.open.bind(this);
        this.close = this.close.bind(this);
        this.onSubmit = this.onSubmit.bind(this);
        this.onUploadFormSubmit = this.onUploadFormSubmit.bind(this);
        this.onUploadAndInstallFormSubmit = this.onUploadAndInstallFormSubmit.bind(this);
        this.onSubmitSuccess = this.onSubmitSuccess.bind(this);
    }

    open() {
        this.setState({ isOpen: true });
    }

    close() {
        this.setState({ isOpen: false, fileName: null });
    }

    onSubmit(...submitArgs) {
        if (this.actionClicked === ACTION_UPLOAD_AND_INSTALL) {
            this.onUploadAndInstallFormSubmit(...submitArgs);
        } else {
            this.onUploadFormSubmit(...submitArgs);
        }
    }

    onUploadAndInstallFormSubmit(requestData, onSuccess, onValidationFailure, onComplete) {
        if (this.props.onUploadAndInstallSuccess) {
            const originalOnSuccess = onSuccess;
            onSuccess = (data) => {
                originalOnSuccess(data);
                this.props.onUploadAndInstallSuccess(data.entity.path);
            };
        }
        const onFailure = (data) => {
            onComplete(data);
            if (data.entity && data.entity.firstActionDone) {
                this.onSubmitSuccess();
                if (this.props.onUploadSuccess) {
                    this.props.onUploadSuccess();
                }
            }
        }
        PackageService.uploadAndInstallPackage(requestData, onSuccess, onValidationFailure, onFailure, onComplete);
    }

    onUploadFormSubmit(requestData, onSuccess, onValidationFailure, onComplete) {
        if (this.props.onUploadSuccess) {
            const originalOnSuccess = onSuccess;
            onSuccess = (data) => {
                originalOnSuccess(data);
                this.props.onUploadSuccess(data.entity.path);
            };
        }
        PackageService.uploadPackage(requestData, onSuccess, onValidationFailure, onComplete);
    }

    onSubmitSuccess() {
        this.close();
    }

    render() {
        const form = (
            <Form
                ref={(element) => this.form = element}
                onSubmit={this.onSubmit}
                onSuccess={this.onSubmitSuccess}
            >
                {({ submitted }) => (
                    <>
                        <FilePicker
                            name='file'
                            placeholder='Browse Package File'
                            onChange={(event) => {
                                const file = event.target.files[0];
                                const fileName = file ? file.name : null;
                                this.setState({ fileName: fileName })
                            }}
                        />
                        <Checkbox
                            hideLabel={true}
                            label='Overwrite existing package'
                            name='force'
                            value='true'
                        />
                        <FormFooter>
                            <ButtonGroup>
                                <Button
                                    appearance='primary'
                                    type='submit'
                                    isLoading={submitted && this.actionClicked === ACTION_UPLOAD_AND_INSTALL}
                                    isDisabled={!this.state.fileName || submitted && this.actionClicked !== ACTION_UPLOAD_AND_INSTALL}
                                    onClick={event => {
                                        event.preventDefault();
                                        this.uploadAndInstallConfirmationModal.open();
                                    }}
                                >
                                    Upload & Install
                                </Button>
                                <Button
                                    appearance='default'
                                    type='submit'
                                    isLoading={submitted && this.actionClicked === ACTION_UPLOAD}
                                    isDisabled={!this.state.fileName || submitted && this.actionClicked !== ACTION_UPLOAD}
                                    onClick={() => {
                                        this.actionClicked = ACTION_UPLOAD;
                                    }}
                                >
                                    Upload
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
                        <ConfirmationModal
                            buttonText='Upload & Install'
                            heading='Upload & Install package'
                            message='Do you really want to upload and install this package?'
                            onConfirm={() => {
                                this.actionClicked = ACTION_UPLOAD_AND_INSTALL;
                                this.form.submit();
                                this.uploadAndInstallConfirmationModal.close();
                            }}
                            ref={(element) => this.uploadAndInstallConfirmationModal = element}
                        />
                    </>
                )}
            </Form>
        );

        return (
            <ModalTransition>
                {this.state.isOpen && (
                    <ModalDialog
                        heading='Upload Package'
                        onClose={this.close}
                    >
                        {form}
                    </ModalDialog>
                )}
            </ModalTransition>
        );
    }
}