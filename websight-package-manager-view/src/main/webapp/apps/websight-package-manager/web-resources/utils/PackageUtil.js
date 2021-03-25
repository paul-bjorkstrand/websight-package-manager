export const isPackageBuilt = ({ size, buildCount }) => {
    return size || buildCount > 0;
}

export const isPackageInstalled = ({ status = {} }) => {
    return status.installation;
}

export const isPackageModified = ({ status = {} }) => {
    return status.modification;
}