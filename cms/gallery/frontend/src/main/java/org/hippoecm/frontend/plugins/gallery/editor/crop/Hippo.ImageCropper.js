(Hippo.ImageCropper = function(){

    var originalImageWidth = ${originalImageWidth};
    var originalImageHeight = ${originalImageHeight};
    var thumbnailWidth = ${thumbnailWidth};
    var thumbnailHeight = ${thumbnailHeight};

    var imgCrop = new YAHOO.widget.ImageCropper(
            '${originalImageMarkupId}',
            {
                keyTick:4,
                initialXY:[10, 10],
                initHeight: thumbnailHeight,
                initWidth: thumbnailWidth,
                ratio: true,
                minHeight: 16,
                minWidth: 16
            });

    var regionInput = YAHOO.util.Dom.get('${regionInputMarkupId}');
    regionInput.value = YAHOO.lang.JSON.stringify(imgCrop.getCropCoords());

    var isPreviewVisible = ${isPreviewVisible};
    if(isPreviewVisible){
        var previewContainer = YAHOO.util.Dom.get('${imagePreviewContainerMarkupId}');
        var previewImage = YAHOO.util.Dom.getFirstChild(previewContainer);
    }

    imgCrop.on('moveEvent', function() {
        var region = imgCrop.getCropCoords();
        regionInput.value = YAHOO.lang.JSON.stringify(region);

        if(isPreviewVisible){
            /*Resize preview image
            Since the ratio is fixed, both height and width change by the same percentage*/
            var scalingFactor = thumbnailWidth / region.width;

            previewImage.style.width = Math.floor(originalImageWidth * scalingFactor) + 'px';
            previewImage.style.height = Math.floor(originalImageHeight * scalingFactor) + 'px';

            previewImage.style.top = '-' + (Math.floor(region.top * scalingFactor)) + 'px';
            previewImage.style.left = '-' + (Math.floor(region.left * scalingFactor)) + 'px';
        }
    });


})();
