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

/**
 * @description
 * <p>
 * Image cropper
 * </p>
 * @namespace YAHOO.hippo
 * @requires yahoo, dom, hippowidget, imagecropper
 * @module hippoimagecropper
 * @beta
 */


YAHOO.namespace('hippo');

if (!YAHOO.hippo.ImageCropper) {
    (function() {
        var Dom = YAHOO.util.Dom, Lang = YAHOO.lang;

        YAHOO.hippo.ImageCropper = function(id, config) {
            YAHOO.hippo.ImageCropper.superclass.constructor.apply(this, arguments);

            this.regionInputId = config.regionInputId;
            this.imagePreviewContainerId = config.imagePreviewContainerId;

            this.initialX = config.initialX;
            this.initialY = config.initialY;
            this.minimumWidth = config.minimumWidth;
            this.minimumHeight = config.minimumHeight;
            
            this.originalImageWidth = config.originalWidth;
            this.originalImageHeight = config.originalHeight;
            this.thumbnailWidth = config.thumbnailWidth;
            this.thumbnailHeight = config.thumbnailHeight;
            
            this.upscalingEnabled = config.upscalingEnabled;
            this.previewVisible = config.previewVisible;
            this.status = config.status;
            
            if(!this.upscalingEnabled) {
                this.minimumWidth = this.thumbnailWidth;
                this.minimumHeight = this.thumbnailHeight;
            }
            
            this.cropper = null;
            this.previewImage = null;
        };

        YAHOO.extend(YAHOO.hippo.ImageCropper, YAHOO.hippo.Widget, {
            
            render: function() {
                if(this.previewVisible) {
                    this.previewImage = Dom.getFirstChild(this.imagePreviewContainerId);

                    //initial values
                    this.previewImage.style.top = "-" + this.initialX + "px";
                    this.previewImage.style.left = "-" + this.initialY + "px";
                }

                this.cropper = new YAHOO.widget.ImageCropper(this.id,
                    {
                        keyTick:4,
                        initialXY:[this.initialX, this.initialY],
                        initHeight: this.thumbnailHeight,
                        initWidth: this.thumbnailWidth,
                        ratio: true,
                        minWidth: this.minimumWidth,
                        minHeight: this.minimumHeight,
                        status : this.status
                    }
                );
                this.cropper.on('moveEvent', this.onMove, null, this);

                this.updateRegionInputValue(this.cropper.getCropCoords());                
            },
            
            onMove : function(e) {
                var coords = this.cropper.getCropCoords();
                this.updateRegionInputValue(coords);
                
                if(this.previewVisible && this.previewImage != null){
                    // Resize preview image
                    // Since the ratio is fixed, both height and width change by the same percentage*/
                    var scalingFactor = this.thumbnailWidth / coords.width;

                    this.previewImage.style.width  = Math.floor(this.originalImageWidth  * scalingFactor) + 'px';
                    this.previewImage.style.height = Math.floor(this.originalImageHeight * scalingFactor) + 'px';

                    this.previewImage.style.top  = '-' + (Math.floor(coords.top  * scalingFactor)) + 'px';
                    this.previewImage.style.left = '-' + (Math.floor(coords.left * scalingFactor)) + 'px';
                }
            },
            
            updateRegionInputValue : function(coords) {
                var regionInput = Dom.get(this.regionInputId);
                if(regionInput) {
                    regionInput.value = Lang.JSON.stringify(coords);    
                }
            }
            
        });
    })();

    YAHOO.register("HippoImageCropper", YAHOO.hippo.ImageCropper, {
        version: "2.9.0", build: "2800"
    });
}