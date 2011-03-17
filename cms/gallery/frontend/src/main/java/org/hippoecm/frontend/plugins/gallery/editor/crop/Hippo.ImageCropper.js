/*
 *  Copyright 2011 Hippo.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

(Hippo.ImageCropper = function(){

    var initialX = 10;
    var initialY = 10;

    var originalImageWidth = ${originalImageWidth};
    var originalImageHeight = ${originalImageHeight};
    var thumbnailWidth = ${thumbnailWidth};
    var thumbnailHeight = ${thumbnailHeight};

    var imgCrop = new YAHOO.widget.ImageCropper(
            '${originalImageMarkupId}',
            {
                keyTick:4,
                initialXY:[initialX, initialY],
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

        //initial values
        previewImage.style.top = "-" + initialX + "px";
        previewImage.style.left = "-" + initialY + "px";
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
