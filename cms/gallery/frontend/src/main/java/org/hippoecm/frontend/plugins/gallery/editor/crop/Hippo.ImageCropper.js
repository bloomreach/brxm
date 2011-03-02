(Hippo.ImageCropper = function(){

    var originalImageWidth = ${originalImageWidth};
    var originalImageHeight = ${originalImageHeight};
    var thumbnailWidth = ${thumbnailWidth};
    var thumbnailHeight = ${thumbnailHeight};

    var imgCrop = new YAHOO.widget.ImageCropper(
            '${originalImageMarkupId}',
            {keyTick:5});

    var previewContainer = YAHOO.util.Dom.get('${imagePreviewContainerMarkupId}');
    var previewImage = YAHOO.util.Dom.getFirstChild(previewContainer);
    var regionInput = YAHOO.util.Dom.get('${regionInputMarkupId}');




    imgCrop.on('moveEvent', function() {
        var region = imgCrop.getCropCoords();
        regionInput.value = YAHOO.lang.JSON.stringify(region);
        previewImage.style.top = '-' + region.top + 'px';
        previewImage.style.left = '-' + region.left + 'px';
        previewContainer.style.height = region.height + 'px';
        previewContainer.style.width = region.width + 'px';
    });

    //resizeThumbnail

})();
