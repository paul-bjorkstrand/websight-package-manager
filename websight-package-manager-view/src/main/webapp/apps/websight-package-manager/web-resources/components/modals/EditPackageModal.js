import React from 'react';

import Button, { ButtonGroup } from '@atlaskit/button';

import PackageModal from './PackageModal.js';
import PackageService from '../../services/PackageService.js';
import { extendFunction } from '../../utils/CommonUtils.js';

export default class EditPackageModal extends React.Component {

    constructor(props) {
        super(props);

        this.open = this.open.bind(this);
        this.close = this.close.bind(this);

        this.getFormFooterContent = this.getFormFooterContent.bind(this);

        this.onEditFormSubmit = this.onEditFormSubmit.bind(this);
    }

    open() {
        this.packageModal.open();
    }

    close() {
        this.packageModal.close();
    }

    onEditFormSubmit(requestData, onSuccess, onValidationFailure, onComplete) {
        requestData = { ...requestData, path: this.props.packageToEdit.path };
        if (this.props.onEditSuccess) {
            onSuccess = extendFunction(onSuccess, () => this.props.onEditSuccess());
        }
        PackageService.updatePackage(requestData, onSuccess, onValidationFailure, onComplete);
    }

    getFormFooterContent({ submitted }) {
        return (
            <ButtonGroup>
                <Button
                    appearance='primary'
                    type='submit'
                    isLoading={submitted}
                >
                    Save
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
        const { groups, packageToEdit } = this.props;

        return (
            <PackageModal
                actionTitle='Edit Package'
                packageToEdit={packageToEdit}
                groups={groups}
                showThumbnail
                formFooterContent={this.getFormFooterContent}
                onSubmit={this.onEditFormSubmit}
                ref={(element) => this.packageModal = element}
            />
        )
    }
}