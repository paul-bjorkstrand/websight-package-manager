export const reorderCollection = (list, sourceIndex, destinationIndex) => {
    const result = Array.from(list);
    const [removed] = result.splice(sourceIndex, 1);
    result.splice(destinationIndex, 0, removed);
    return result;
};

export const getOptionByValue = (value, options) => {
    return (options || []).find(option => option.value === value);
};

export const extendFunction = (toBeExtended, toBeAdded) => {
    if (toBeAdded) {
        const originalFunction = toBeExtended;
        toBeExtended = (...args) => {
            originalFunction(...args);
            toBeAdded(...args);
        }
    }
    return toBeExtended;
};
