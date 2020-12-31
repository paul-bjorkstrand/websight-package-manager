import React from 'react';
import { LinkItem } from '@atlaskit/menu';

import { NavigationItemIcon } from 'websight-admin/Icons';

import { PACKAGE_MANAGER_ROOT_PATH } from '../../../../utils/PackageManagerConstants.js';

export default class NavigationItemFragment extends React.Component {
    render() {
        return (
            <LinkItem
                href={PACKAGE_MANAGER_ROOT_PATH}
                elemBefore={<NavigationItemIcon className='material-icons'>dvr</NavigationItemIcon>}
            >
                Package Manager
            </LinkItem>
        );
    }
}
