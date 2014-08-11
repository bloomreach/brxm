/*
 *  Copyright 2011-2014 Hippo B.V. (http://www.onehippo.com)
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
        "use strict";

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

            this.fixedDimension = config.fixedDimension;
            this.thumbnailSizeLabelId = config.thumbnailSizeLabelId;

            if (!this.upscalingEnabled) {
                this.minimumWidth = this.thumbnailWidth;
                this.minimumHeight = this.thumbnailHeight;
            }

            var lca = Dom.getElementsByClassName('left-crop-area', 'div');
            this.leftCropArea = lca.length === 1 ? lca[0] : null;

            this.cropper = null;
            this.previewImage = null;
            this.previewContainer = null;
            this.previewLabelTemplate = null;
        };

        YAHOO.extend(YAHOO.hippo.ImageCropper, YAHOO.hippo.Widget, {

            render: function() {
                if (this.previewVisible) {
                    this.previewImage = Dom.getFirstChild(this.imagePreviewContainerId);
                    this.previewContainer = Dom.get(this.imagePreviewContainerId);

                    //initial values
                    Dom.setStyle(this.previewImage, 'top',  '-' + this.initialX + 'px');
                    Dom.setStyle(this.previewImage, 'left', '-' + this.initialY + 'px');
                }
                this.previewLabelTemplate = Dom.get(this.thumbnailSizeLabelId).innerHTML;

                // Call second render phase after image has loaded completely and add a timeout
                // to force IE to behave the same all the time.
                var img = new Image();
                img.onload = function() {
                    window.setTimeout(function() {this._render();}.bind(this), 10);
                }.bind(this);
                img.src = this.el.src;
            },
            
            // this phase of the render method should only start after the image has loaded completely
            _render: function() {

                this.cropper = new YAHOO.widget.ImageCropper(this.id,
                        {
                            keyTick: 4,
                            initialXY:[this.initialX, this.initialY],
                            initHeight: this.thumbnailHeight,
                            initWidth: this.thumbnailWidth,
                            ratio: this.fixedDimension === 'both',
                            minWidth: this.minimumWidth,
                            minHeight: this.minimumHeight,
                            status : this.status
                        }
                );
                this.cropper.on('moveEvent', this.onMove, null, this);

                this.updateRegionInputValue(this.cropper.getCropCoords());
                this.updatePreviewLabel(this.thumbnailWidth, this.thumbnailHeight);

                if (this.leftCropArea !== null) {
                    this.leftCropAreaRegion = Dom.getRegion(this.leftCropArea); 
                }
                
                this.subscribe();
            },
            
            subscribe: function() {
                var e;
                if (Wicket.Window.current) {
                    e = Wicket.Window.current.event;
                    
                    e.afterInitScreen.subscribe(this.normalSize, this);
                    e.afterFullScreen.subscribe(this.fullSize, this);
                    e.resizeFullScreen.subscribe(this.fullResize, this);
                }
            },
            
            normalSize : function(type, args, me) {
                Dom.setStyle(me.leftCropArea, 'width', me.leftCropAreaRegion.width + 'px');
                Dom.setStyle(me.leftCropArea, 'height', me.leftCropAreaRegion.height + 'px');
            },

            // left crop area has margin 5px so subtract 10px from width&height to prevent unwanted scrollbars
            fullSize: function(type, args, me) {
                var dim = args[0];
                Dom.setStyle(me.leftCropArea, 'width', (dim.w - 10) + 'px'); 
                Dom.setStyle(me.leftCropArea, 'height', (dim.h - 10) + 'px'); 
            },

            // left crop area has margin 5px so subtract 10px from width&height to prevent unwanted scrollbars
            fullResize: function(type, args, me) {
                var dim = args[0];
                Dom.setStyle(me.leftCropArea, 'width', (dim.w - 10) + 'px');
                Dom.setStyle(me.leftCropArea, 'height', (dim.h - 10) + 'px');
            },

            onMove : function(e) {
                var coords = this.cropper.getCropCoords();
                this.updateRegionInputValue(coords);
                this.updatePreviewImage(coords);
            },

            updatePreviewImage : function(coords) {
                var scalingFactor = 1, previewContainerWidth, previewContainerHeight;
                if (this.fixedDimension === 'both') {
                    // Since the ratio is fixed, both height and width change by the same percentage
                    scalingFactor = this.thumbnailWidth / coords.width;
                    previewContainerWidth = this.thumbnailWidth;
                    previewContainerHeight = this.thumbnailHeight;
                } else if (this.fixedDimension === 'width') {
                    scalingFactor = this.thumbnailWidth / coords.width;
                    previewContainerWidth = this.thumbnailWidth;
                    previewContainerHeight = Math.floor(scalingFactor * coords.height);
                } else if (this.fixedDimension === 'height') {
                    scalingFactor = this.thumbnailHeight / coords.height;
                    previewContainerWidth = Math.floor(scalingFactor * coords.width);
                    previewContainerHeight = this.thumbnailHeight;
                }
                this.updatePreviewLabel(previewContainerWidth, previewContainerHeight);

                if (this.previewVisible && this.previewImage !== null) {
                    // set the preview box dimensions
                    Dom.setStyle(this.previewContainer, 'width',  previewContainerWidth + 'px');
                    Dom.setStyle(this.previewContainer, 'height', previewContainerHeight + 'px');

                    var w = Math.floor(this.originalImageWidth  * scalingFactor),
                            h = Math.floor(this.originalImageHeight * scalingFactor),
                            x = Math.floor(coords.top  * scalingFactor),
                            y = Math.floor(coords.left * scalingFactor);

                    Dom.setStyle(this.previewImage, 'top',   '-' + x + 'px');
                    Dom.setStyle(this.previewImage, 'left',  '-' + y + 'px');
                    Dom.setStyle(this.previewImage, 'width',  w + 'px');
                    Dom.setStyle(this.previewImage, 'height', h + 'px');
                }
            },
            
            updateRegionInputValue : function(coords) {
                var regionInput = Dom.get(this.regionInputId);
                if (regionInput) {
                    regionInput.value = Lang.JSON.stringify(coords);
                }
            },

            updatePreviewLabel : function(w, h) {
                var label = this.previewLabelTemplate.replace('width', w).replace('height', h);
                Dom.get(this.thumbnailSizeLabelId).innerHTML = label;
            }

        });
    })();

    YAHOO.register("HippoImageCropper", YAHOO.hippo.ImageCropper, {
        version: "2.9.0", build: "2800"
    });
}
