import React from 'react';
import Button, { ButtonGroup } from '@atlaskit/button';

import ConfirmationModal from 'websight-admin/ConfirmationModal';

import PackageService from '../../services/PackageService.js';
import { extendFunction } from '../../utils/CommonUtils.js';
import PackageModal from './PackageModal.js';

const ACTION_CREATE = 'create';
const ACTION_CREATE_AND_BUILD = 'create-and-build';

export default class CreatePackageModal extends React.Component {

    constructor(props) {
        super(props);

        this.open = this.open.bind(this);
        this.close = this.close.bind(this);

        this.getFormFooterContent = this.getFormFooterContent.bind(this);

        this.onSubmit = this.onSubmit.bind(this);
        this.onCreateAndBuildFormSubmit = this.onCreateAndBuildFormSubmit.bind(this);
        this.onCreateFormSubmit = this.onCreateFormSubmit.bind(this);
    }

    open() {
        this.packageModal.open();
    }

    close() {
        this.packageModal.close();
    }

    onCreateAndBuildFormSubmit(requestData, onSuccess, onValidationFailure, onComplete) {
        if (this.props.onCreateAndBuildSuccess) {
            onSuccess = extendFunction(onSuccess, data => this.props.onCreateAndBuildSuccess(data.entity.path));
        }
        const onFailure = (data) => {
            onComplete(data);
            if (data.entity && (data.entity.firstActionDone || data.entity.path)) {
                this.onSubmitSuccess();
                if (this.props.onCreateSuccess) {
                    this.props.onCreateSuccess();
                }
            }
        }
        PackageService.createAndBuildPackage(requestData, onSuccess, onValidationFailure, onFailure, onComplete);
    }

    onCreateFormSubmit(requestData, onSuccess, onValidationFailure, onComplete) {
        if (this.props.onCreateSuccess) {
            onSuccess = extendFunction(onSuccess, (data) => this.props.onCreateSuccess(data.entity.path));
        }
        const onFailure = (data) => {
            onComplete(data);
            if (data.entity && data.entity.path) {
                this.onSubmitSuccess();
                if (this.props.onCreateSuccess) {
                    this.props.onCreateSuccess();
                }
            }
        }
        PackageService.createPackage(requestData, onSuccess, onValidationFailure, onFailure, onComplete);
    }

    onSubmit(...submitArgs) {
        if (this.actionClicked === ACTION_CREATE_AND_BUILD) {
            this.onCreateAndBuildFormSubmit(...submitArgs);
        } else {
            this.onCreateFormSubmit(...submitArgs);
        }
    }

    getFormFooterContent({ submitted }) {
        return (
            <ButtonGroup>
                <Button
                    appearance='primary'
                    type='submit'
                    isLoading={submitted && this.actionClicked === ACTION_CREATE_AND_BUILD}
                    isDisabled={submitted && this.actionClicked !== ACTION_CREATE_AND_BUILD}
                    onClick={event => {
                        event.preventDefault();
                        this.createAndBuildConfirmationModal.open();
                    }}
                >
                    Create & Build
                </Button>
                <Button
                    appearance='default'
                    type='submit'
                    isLoading={submitted && this.actionClicked === ACTION_CREATE}
                    isDisabled={submitted && this.actionClicked !== ACTION_CREATE}
                    onClick={() => {
                        this.actionClicked = ACTION_CREATE;
                    }}
                >
                    Create
                </Button>
                <Button
                    appearance='subtle'
                    onClick={this.close}
                    isDisabled={submitted}
                >
                    Cancel
                </Button>
            </ButtonGroup>
        )
    }

    render() {
        const { groups, defaultGroup } = this.props;

        return (
            <>
                <PackageModal
                    actionTitle='Create Package'
                    groups={groups}
                    defaultGroup={defaultGroup}
                    formFooterContent={this.getFormFooterContent}
                    onSubmit={this.onSubmit}
                    ref={(element) => this.packageModal = element}
                />
                <ConfirmationModal
                    buttonText='Create & Build'
                    heading='Create & Build package'
                    message='Do you really want to create and build this package?'
                    onConfirm={() => {
                        this.actionClicked = ACTION_CREATE_AND_BUILD;
                        this.packageModal.submit();
                        this.createAndBuildConfirmationModal.close();
                    }}
                    ref={(element) => this.createAndBuildConfirmationModal = element}
                />
            </>
        )
    }
}