import React from 'react';

const ThumbnailIcon = (props) => {
    return (
        <img
            src='/apps/websight-package-manager/web-resources/images/package.svg'
            alt='Package Icon'
            width={props.size || 32}
            height={props.size || 32}
            {...props}
            style={{ marginRight: '8px', ...props.style }} />
    );
}

const ThumbnailImage = (props) => {
    return (
        <img
            alt="Thumbnail"
            width={props.size || 32}
            height={props.size || 32}
            {...props}
            style={{ marginRight: '8px', borderRadius: '10%', ...props.style }}
        />
    )
}

const getImageExtension = (path) => {
    return path.split('.').pop();
}

const generateImageSuffix = (path) => {
    const imageExtension = getImageExtension(path);
    if (imageExtension) {
        return `/image.${imageExtension}`;
    }
    return '';
}

const PackageThumbnail = (props) => {
    const { thumbnailData } = props;
    return thumbnailData
        ? <ThumbnailImage
            src={`${thumbnailData.path}.${thumbnailData.timestamp}.res${generateImageSuffix(thumbnailData.path)}`}
            {...props}
        />
        : <ThumbnailIcon {...props} />
}

export default PackageThumbnail;
