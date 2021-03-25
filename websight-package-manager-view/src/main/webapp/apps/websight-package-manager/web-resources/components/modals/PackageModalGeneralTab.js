import React from 'react';
import { Checkbox } from '@atlaskit/checkbox';
import { Fieldset } from '@atlaskit/form';
import { CreatableSelect } from '@atlaskit/select';
import TextArea from '@atlaskit/textarea';
import Textfield from '@atlaskit/textfield';
import styled from 'styled-components';

import FilePicker from 'websight-admin/FilePicker';

import { getOptionByValue } from '../../utils/CommonUtils.js';
import PackageThumbnail from '../PackageThumbnail.js';

const GeneralTabContainer = styled.div`
    width: 100%;
`;

const getGroupsAsOptions = (groups) => {
    return groups
        .filter(group => group.name !== ':no_group')
        .map((group) => (
            { label: group.name, value: group.name }
        ));
};

export default class PackageModalGeneralTab {

    constructor(props) {
        this.props = props;
    }

    render() {
        const { packageData, defaultGroup, showThumbnail, showDeleteThumbnailCheckbox } = this.props;

        const groupsOptions = getGroupsAsOptions(this.props.groups);
        const actualDefaultGroup = defaultGroup || packageData.group || 'my_packages';

        return (
            <GeneralTabContainer>
                <Textfield
                    autocomplete='off'
                    defaultValue={packageData.name}
                    isRequired
                    name='name'
                    label='Name'
                    autoFocus={true}
                />
                <CreatableSelect
                    isClearable
                    name='group'
                    label='Group'
                    options={groupsOptions}
                    defaultValue={getOptionByValue(actualDefaultGroup, groupsOptions)}
                />
                <Textfield
                    autocomplete='off'
                    defaultValue={packageData.version}
                    name='version'
                    label='Version'
                />
                <TextArea
                    autocomplete='off'
                    defaultValue={packageData.description}
                    name='description'
                    label='Description'
                    resize='vertical'
                />
                <Fieldset legend='Thumbnail'>
                    {showThumbnail && packageData.thumbnail && (
                        <div
                            style={{ display: 'flex', marginBottom: '16px' }}
                            onMouseEnter={() => this.props.onShowDeleteThumbnailChange(true)}
                            onMouseLeave={() => this.props.onShowDeleteThumbnailChange(false)}>
                            <PackageThumbnail
                                thumbnailData={packageData.thumbnail}
                                style={{
                                    marginTop: '8px'
                                }}
                                size={64}
                            />
                            {(showDeleteThumbnailCheckbox || this.props.deleteThumbnail) &&
                            <div style={{ display: 'flex', alignItems: 'center' }}>
                                <Checkbox
                                    hideLabel={true}
                                    label='Delete thumbnail'
                                    name='deleteThumbnail'
                                    value='true'
                                    onChange={() => {
                                        // TODO: Correct this workaround when WS-376 is fixed
                                        this.props.onDeleteThumbnailChange('thumbnail');
                                    }}
                                />
                            </div>
                            }
                        </div>
                    )}
                    {!this.props.deleteThumbnail && (
                        <div style={{ marginTop: '-6px' }}>
                            <FilePicker
                                name='thumbnail'
                                accept='image/png'
                            />
                        </div>
                    )}
                </Fieldset>
            </GeneralTabContainer>
        )
    }
}
