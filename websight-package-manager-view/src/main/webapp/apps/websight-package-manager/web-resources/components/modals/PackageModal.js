import React from 'react';
import ModalDialog, { ModalTransition } from '@atlaskit/modal-dialog';
import Tabs from '@atlaskit/tabs';

import ConfirmationModal from 'websight-admin/ConfirmationModal';
import ListenForKeyboardShortcut from 'websight-admin/ListenForKeyboardShortcut';
import Form, { FormFooter } from 'websight-rest-atlaskit-client/Form';

import PackageModalGeneralTab from './PackageModalGeneralTab.js';
import PackageModalFiltersTab from './PackageModalFiltersTab.js';
import PackageModalAdvancedTab from './PackageModalAdvancedTab.js';

const emptyPackageData = {
    group: '',
    name: '',
    version: '',
    description: '',
    thumbnail: '',
    requiresRestart: false
};

const getFiltersFromPackage = (packageData) => {
    return (packageData.filters || []).map(filter => {
        const rules = (filter.rules || []).map(rule => ({
            include: rule.include,
            path: rule.pattern
        }));
        return {
            mode: filter.mode.toLowerCase(),
            root: filter.root,
            rules: rules
        }
    })
}

const getDependenciesFromPackage = (packageData) => {
    return (packageData.dependencies || []).map(dependency => dependency.dependency);
}

export default class PackageModal extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            isOpen: false,
            currentTab: 0,
            packageData: emptyPackageData,
            deleteThumbnail: false,
            filters: [],
            dependencies: [],
            showDeleteThumbnailCheckbox: false,
            dataChanged: false
        };

        this.open = this.open.bind(this);
        this.close = this.close.bind(this);

        this.onDeleteThumbnailChange = this.onDeleteThumbnailChange.bind(this);
        this.onShowDeleteThumbnailChange = this.onShowDeleteThumbnailChange.bind(this);

        this.updateFilters = this.updateFilters.bind(this);

        this.updateDependencies = this.updateDependencies.bind(this);

        this.submit = this.submit.bind(this);
        this.onSubmit = this.onSubmit.bind(this);
        this.onSubmitSuccess = this.onSubmitSuccess.bind(this);
    }

    open() {
        let packageData = emptyPackageData;
        let filters = [];
        let dependencies = [];
        const packageToEdit = this.props.packageToEdit;
        if (packageToEdit) {
            packageData = {
                ...packageData,
                name: packageToEdit.name,
                downloadName: packageToEdit.downloadName,
                group: packageToEdit.group,
                version: packageToEdit.version,
                description: packageToEdit.description,
                thumbnail: packageToEdit.thumbnail,
                acHandling: packageToEdit.acHandling,
                requiresRestart: packageToEdit.requiresRestart
            }
            filters = getFiltersFromPackage(packageToEdit);
            dependencies = getDependenciesFromPackage(packageToEdit);
        }
        this.setState({
            isOpen: true,
            currentTab: 0,
            packageData: packageData,
            deleteThumbnail: false,
            filters: filters,
            dependencies: dependencies,
            selectedGroup: this.props.selectedGroup,
            showDeleteThumbnailCheckbox: false,
            dataChanged: false
        });
    }

    close() {
        if (this.state.dataChanged) {
            this.closeConfirmationModal.open();
        } else {
            this.confirmedClose();
        }
    }

    confirmedClose() {
        if (this.closeConfirmationModal) {
            this.closeConfirmationModal.close();
        }
        this.setState({ isOpen: false });
    }

    onDeleteThumbnailChange(thumbnailParamName) {
        this.setState(prevState => ({ deleteThumbnail: !prevState.deleteThumbnail }));
        if (!this.state.deleteThumbnail) {
            this.form.removeField(thumbnailParamName);
        }
    }

    onShowDeleteThumbnailChange(showDeleteThumbnailCheckbox) {
        this.setState({ showDeleteThumbnailCheckbox: showDeleteThumbnailCheckbox });
    }

    updateFilters(filters) {
        this.setState({ filters: filters, dataChanged: true });
    }

    updateDependencies(dependencies) {
        this.setState({ dependencies: dependencies, dataChanged: true })
    }

    onSubmitSuccess() {
        this.confirmedClose();
    }

    submit() {
        this.form.submit();
    }

    onSubmit(requestData, onSuccess, onValidationFailure, onComplete) {
        const newRequestData = {
            ...requestData,
            filters: JSON.stringify(this.state.filters),
            dependencies: JSON.stringify(this.state.dependencies)
        };
        this.props.onSubmit(newRequestData, onSuccess, onValidationFailure, onComplete);
    }

    render() {
        const { packageData, filters, dependencies, showDeleteThumbnailCheckbox } = this.state;

        const generalTab = (new PackageModalGeneralTab(
            {
                packageData: packageData,
                groups: this.props.groups,
                defaultGroup: this.props.defaultGroup,
                showThumbnail: this.props.showThumbnail,
                showDeleteThumbnailCheckbox: showDeleteThumbnailCheckbox,
                deleteThumbnail: this.state.deleteThumbnail,
                onDeleteThumbnailChange: this.onDeleteThumbnailChange,
                onShowDeleteThumbnailChange: this.onShowDeleteThumbnailChange
            }
        )).render();

        const filtersTab = (new PackageModalFiltersTab(
            {
                filters: filters,
                updateFilters: this.updateFilters
            }
        )).render();

        const advancedTab = (new PackageModalAdvancedTab(
            {
                packageData: packageData,
                dependencies: dependencies,
                updateDependencies: this.updateDependencies
            }
        )).render();

        const tabs = [
            { label: 'General', content: generalTab },
            { label: 'Filters', content: filtersTab },
            { label: 'Advanced', content: advancedTab }
        ];

        const modalContent = (
            <Form
                ref={(element) => this.form = element}
                onFormDataChange={() => this.setState({ dataChanged: true })}
                onSubmit={this.onSubmit}
                onSuccess={this.onSubmitSuccess}
            >
                {(formProps) => (
                    <>
                        <Tabs
                            onSelect={(_tab, index) => this.setState({ currentTab: index })}
                            selected={this.state.currentTab}
                            style={{ width: '100%' }}
                            tabs={tabs}
                        />
                        <FormFooter>
                            {this.props.formFooterContent(formProps)}
                        </FormFooter>
                    </>
                )}
            </Form>
        );

        return (
            <ModalTransition>
                {this.state.isOpen && (
                    <>
                        <ListenForKeyboardShortcut
                            key='Escape'
                            keyCodes={['Escape']}
                            callback={() => this.close()}
                        />
                        <ModalDialog
                            shouldCloseOnEscapePress={false}
                            width={'700px'}
                            heading={this.props.actionTitle}
                            onClose={this.close}
                            ref={(elementRef) => this.modalRef = elementRef}
                        >
                            {modalContent}
                        </ModalDialog>
                        <ConfirmationModal
                            buttonText={'Close'}
                            heading={this.props.actionTitle}
                            message={(
                                <>
                                    Form data modification detected. Do you really want to close the dialog without
                                    saving changes?
                                </>
                            )}
                            appearance='warning'
                            onConfirm={() => this.confirmedClose()}
                            ref={(element) => this.closeConfirmationModal = element}
                        />
                    </>
                )}
            </ModalTransition>
        );
    }
}