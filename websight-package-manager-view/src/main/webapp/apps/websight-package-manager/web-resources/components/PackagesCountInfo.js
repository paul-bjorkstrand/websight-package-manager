import React from 'react';

import TableItemsCountInfo from 'websight-admin/components/TableItemsCountInfo';

import CloseableSectionMessage from './CloseableSectionMessage.js';

const PackagesCountInfo = (props) => {
    const { numberOfFoundPackages, packagesLimit, packagesLimitExceeded } = props;
    return (
        <>
            {packagesLimitExceeded && (
                <CloseableSectionMessage id='packages-limit-reached-message' appearance='warning'>
                    <p>
                        Sorting is disabled due to the high number of packages in this view
                        (more than {packagesLimit})
                    </p>
                </CloseableSectionMessage>
            )}
            <TableItemsCountInfo
                isHidden={numberOfFoundPackages === null || packagesLimitExceeded}
                itemName='package'
                numberOfFoundItems={numberOfFoundPackages}
            />
        </>
    )
}

export default PackagesCountInfo;