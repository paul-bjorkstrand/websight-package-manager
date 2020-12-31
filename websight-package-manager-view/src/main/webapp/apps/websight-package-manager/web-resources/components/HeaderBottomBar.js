import React from 'react';
import Select, { CheckboxSelect } from '@atlaskit/select';
import Tooltip from '@atlaskit/tooltip';

import {
    FilterOptionsContainer,
    FilterPatternContainer,
    FilterSortByContainer,
    HeaderFiltersContainer
} from 'websight-admin/Containers';
import { debounce } from 'websight-admin/Utils';
import StatefulFilterInput from 'websight-admin/components/StatefulFilterInput';

const FILTER_OPTION_BUILT = 'built';
const FILTER_OPTION_NOT_BUILT = 'notBuilt';
const FILTER_OPTION_INSTALLED = 'installed';
const FILTER_OPTION_NOT_INSTALLED = 'notInstalled';

const filterOptions = [
    { label: 'Created by me', value: 'createdByMe' },
    { label: 'Built', value: FILTER_OPTION_BUILT },
    { label: 'Not built', value: FILTER_OPTION_NOT_BUILT },
    { label: 'Installed', value: FILTER_OPTION_INSTALLED },
    { label: 'Not installed', value: FILTER_OPTION_NOT_INSTALLED },
    { label: 'Modified', value: 'modified' },
    { label: 'Scheduled', value: 'scheduled' }
]

const getFilterOptionsByValues = (values) => {
    if (!values) {
        return [];
    }
    return filterOptions.filter(option => values.includes(option.value));
}

const extractValuesFromOptions = (options) => {
    options = options || [];
    return options.map(option => option.value);
}

const adjustFilterOptionsValuesOnChange = (newValues, previousValues) => {
    newValues = newValues || [];
    previousValues = previousValues || [];
    if (valueAdded(FILTER_OPTION_BUILT, newValues, previousValues)) {
        removeFromValues(FILTER_OPTION_NOT_BUILT, newValues)
    } else if (valueAdded(FILTER_OPTION_NOT_BUILT, newValues, previousValues)) {
        removeFromValues(FILTER_OPTION_BUILT, newValues)
    } else if (valueAdded(FILTER_OPTION_INSTALLED, newValues, previousValues)) {
        removeFromValues(FILTER_OPTION_NOT_INSTALLED, newValues)
    } else if (valueAdded(FILTER_OPTION_NOT_INSTALLED, newValues, previousValues)) {
        removeFromValues(FILTER_OPTION_INSTALLED, newValues)
    }
    return newValues;
}

const valueAdded = (optionValue, newValues, previousValues) => {
    return newValues.includes(optionValue) && !previousValues.includes(optionValue);
}

const removeFromValues = (value, values) => {
    const indexToRemove = values.indexOf(value);
    if (indexToRemove >= 0) {
        values.splice(indexToRemove, 1);
    }
}

const defaultSortBySelectOption = { label: 'Last used', value: 'lastUsed' };

const sortBySelectOptions = [
    defaultSortBySelectOption,
    { label: 'Name', value: 'name' },
    { label: 'Last modified', value: 'lastModified' },
    { label: 'Installation date', value: 'installationDate' },
    { label: 'Recently added', value: 'recentlyAdded' }
]

const getSortByOptionByValue = (value) => {
    if (!value) {
        return defaultSortBySelectOption;
    }
    return sortBySelectOptions.find(option => option.value === value);
}

export default class HeaderBottomBar extends React.Component {

    render() {
        const { params, limitExceeded } = this.props;

        return (
            <HeaderFiltersContainer>
                <FilterPatternContainer>
                    <StatefulFilterInput
                        onClear={() => {
                            this.props.findPackages({
                                filter: '',
                                pageNumber: 1
                            })
                        }}
                        onChange={(event) => {
                            const targetValue = event.target.value;
                            this.debounceTimerId = debounce(() => this.props.findPackages({
                                filter: targetValue,
                                pageNumber: 1
                            }), this.debounceTimerId);
                        }}
                        value={params.filter}
                    />
                </FilterPatternContainer>
                <FilterOptionsContainer>
                    <CheckboxSelect
                        placeholder="Choose a filter"
                        spacing="compact"
                        options={filterOptions}
                        value={getFilterOptionsByValues(params.filterOptions)}
                        onChange={(selectedOptions) => {
                            const adjustedValues = adjustFilterOptionsValuesOnChange(
                                extractValuesFromOptions(selectedOptions),
                                params.filterOptions
                            );
                            this.props.findPackages({
                                filterOptions: adjustedValues,
                                pageNumber: 1
                            })
                        }}
                    />
                </FilterOptionsContainer>
                <FilterSortByContainer>
                    {limitExceeded ?
                        <Tooltip content='Sorting disabled due to high number of packages' delay={0}>
                            <Select
                                placeholder='Sort By'
                                spacing='compact'
                                isDisabled={true} />
                        </Tooltip> :
                        <Select
                            placeholder='Sort By'
                            spacing='compact'
                            options={sortBySelectOptions}
                            defaultValue={defaultSortBySelectOption}
                            value={getSortByOptionByValue(params.sortBy)}
                            onChange={(selectedOption) => {
                                this.props.findPackages({
                                    sortBy: selectedOption.value,
                                    pageNumber: 1
                                })
                            }}
                        />
                    }
                </FilterSortByContainer>
            </HeaderFiltersContainer>
        )
    }
}