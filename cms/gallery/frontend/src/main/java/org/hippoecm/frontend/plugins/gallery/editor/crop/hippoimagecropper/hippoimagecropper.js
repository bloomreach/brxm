/*
 *  Copyright 2011-2018 Hippo B.V. (http://www.onehippo.com)
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
  (function () {
    "use strict";

    var Dom = YAHOO.util.Dom,
      Lang = YAHOO.lang,
      VIEW_MARGIN = 10,
      DIALOG_FOOTER_HEIGHT = 48;

    YAHOO.hippo.ImageCropper = function (id, config) {
      YAHOO.hippo.ImageCropper.superclass.constructor.apply(this, arguments);
      this.container = this.el.parentNode;
      this.originalImage = this.el;
      this.fullScreenMode = false;
      this.fitView = config.fitView;

      // NOTE: these are 0 at this stage, re-initialized in render method
      this.original_width = this.originalImage.offsetWidth;
      this.original_height = this.originalImage.offsetHeight;

      this.regionInputId = config.regionInputId;
      this.imagePreviewContainerId = config.imagePreviewContainerId;
      this.initialX = config.initialX;
      this.initialY = config.initialY;
      this.initialWidth = config.initialWidth;
      this.initialHeight = config.initialHeight;
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

      // Set the max preview width and height (used to determine the scaling of the preview container/image)
      this.maxPreviewWidth = 200;
      this.maxPreviewHeight = 300;

      var lca = Dom.getElementsByClassName('left-crop-area', 'div');
      this.leftCropArea = lca.length === 1 ? lca[0] : null;

      this.cropper = null;
      this.previewImage = null;
      this.previewContainer = null;
      this.previewLabelTemplate = null;
    };

    YAHOO.extend(YAHOO.hippo.ImageCropper, YAHOO.hippo.Widget, {

      render: function () {
        var scalingFactor;
        if (this.previewVisible) {
          this.previewImage = Dom.getFirstChild(this.imagePreviewContainerId);
          this.previewContainer = Dom.get(this.imagePreviewContainerId);

          // initial values
          Dom.setStyle(this.previewImage, 'top', '-' + this.initialX + 'px');
          Dom.setStyle(this.previewImage, 'left', '-' + this.initialY + 'px');

          scalingFactor = this.determinePreviewScalingFactor(this.thumbnailWidth, this.thumbnailHeight);
          Dom.setStyle(this.previewImage, 'width', Math.floor(scalingFactor * this.originalImageWidth) + 'px');
          Dom.setStyle(this.previewImage, 'height', Math.floor(scalingFactor * this.originalImageHeight) + 'px');

        }
        this.previewLabelTemplate = Dom.get(this.thumbnailSizeLabelId).innerHTML;
        var img = new Image();
        var self = this;
        img.onload = function () {
          self._render();
        };
        img.src = this.el.src;
      },

      // this phase of the render method should only start after the image has loaded completely
      _render: function () {
        this.original_width = this.originalImage.offsetWidth;
        this.original_height = this.originalImage.offsetHeight;

        this.ratio = 1;

        if (this.leftCropArea !== null) {
          var dialogCenter = this.leftCropArea.parentNode;
          var dialogCenterRegion = Dom.getRegion(dialogCenter);
          // set height of left crop area to dialog-center height
          Dom.setStyle(this.leftCropArea, 'height', (dialogCenterRegion.height - (2 * VIEW_MARGIN)) + 'px');
          this.leftCropAreaRegion = Dom.getRegion(this.leftCropArea);
        }

        this.cropper = new YAHOO.widget.ImageCropper(this.id,
          {
            keyTick: 4,
            initialXY: [this.initialX, this.initialY],
            initHeight: this.initialHeight,
            initWidth: this.initialWidth,
            ratio: this.fixedDimension === 'both',
            minWidth: this.minimumWidth,
            minHeight: this.minimumHeight,
            status: this.status
          }
        );
        this.cropper.on('moveEvent', this.onMove, null, this);
        this.updateRegionInputValue(this.cropper.getCropCoords());
        this.updatePreviewLabel(this.thumbnailWidth, this.thumbnailHeight);

        this.subscribe();
        if (this.fitView) {
          this._scaleToFit(this.ratio);
        }
      },

      subscribe: function () {
        if (Wicket.Window.current) {
          var e = Wicket.Window.current.event;
          e.afterInitScreen.subscribe(this._normalSize, this);
          e.afterFullScreen.subscribe(this._fullSize, this);
          e.resizeFullScreen.subscribe(this._fullResize, this);
        }
      },

      _normalSize: function (type, args, me) {
        me.fullScreenMode = false;
        me._setLeftCropAreaSize(me.leftCropAreaRegion.width, me.leftCropAreaRegion.height);
        me._scaleToFit();
      },

      _fullSize: function (type, args, me) {
        me.fullScreenMode = true;
        me._setFullSize(args[0]);
      },

      _fullResize: function (type, args, me) {
        me._setFullSize(args[0]);
      },

      _setFullSize: function(dim) {
        // Left crop area has left & top margin so subtract it from width & height to prevent unwanted scrollbars.
        // Subtract footer height from height to ensure the buttons in the footer are visible.
        var margin = 2 * VIEW_MARGIN;
        this._setLeftCropAreaSize(dim.w - margin, dim.h - margin - DIALOG_FOOTER_HEIGHT);
        this._scaleToFit();
      },

      // Called from Wicket
      fitInView: function(fitView) {
        this.fitView = fitView;
        this._scaleToFit();
      },

      _setLeftCropAreaSize: function(width, height) {
        Dom.setStyle(this.leftCropArea, 'width', width + 'px');
        Dom.setStyle(this.leftCropArea, 'height', height + 'px');
      },

      onMove: function () {
        var coords = this.cropper.getCropCoords();
        this.updateRegionInputValue(coords);
        this.updatePreviewImage(coords);
      },

      updatePreviewImage: function (coords) {
        var targetScalingFactor = 1, scalingFactor,
          previewImageWidth, previewImageHeight,
          previewContainerWidth, previewContainerHeight;

        if (this.fixedDimension === 'both') {
          // Since the ratio is fixed, both height and width change by the same percentage
          targetScalingFactor = this.thumbnailWidth / coords.width;
          previewImageWidth = this.thumbnailWidth;
          previewImageHeight = this.thumbnailHeight;
        } else if (this.fixedDimension === 'width') {
          targetScalingFactor = this.thumbnailWidth / coords.width;
          previewImageWidth = this.thumbnailWidth;
          previewImageHeight = Math.floor(targetScalingFactor * coords.height);
        } else if (this.fixedDimension === 'height') {
          targetScalingFactor = this.thumbnailHeight / coords.height;
          previewImageWidth = Math.floor(targetScalingFactor * coords.width);
          previewImageHeight = this.thumbnailHeight;
        }

        // Check for scaling to max preview width
        scalingFactor = this.determinePreviewScalingFactor(coords.width, coords.height);

        if (scalingFactor < targetScalingFactor) {
          previewContainerWidth = Math.floor(scalingFactor * coords.width);
          previewContainerHeight = Math.floor(scalingFactor * coords.height);
        } else {
          scalingFactor = targetScalingFactor;
          previewContainerWidth = previewImageWidth;
          previewContainerHeight = previewImageHeight;
        }

        this.updatePreviewLabel(previewImageWidth, previewImageHeight);
        if (this.previewVisible && this.previewImage !== null) {
          this.updatePreviewImageDimensions(coords, previewContainerWidth, previewContainerHeight, scalingFactor);
        }
      },

      updatePreviewImageDimensions: function (coords, previewWidth, previewHeight, scalingFactor) {
        // set the preview box dimensions
        Dom.setStyle(this.previewContainer, 'width', previewWidth + 'px');
        Dom.setStyle(this.previewContainer, 'height', previewHeight + 'px');

        var w = Math.floor(this.originalImageWidth * scalingFactor),
          h = Math.floor(this.originalImageHeight * scalingFactor),
          x = Math.floor(coords.top * scalingFactor),
          y = Math.floor(coords.left * scalingFactor);

        Dom.setStyle(this.previewImage, 'top', '-' + x + 'px');
        Dom.setStyle(this.previewImage, 'left', '-' + y + 'px');
        Dom.setStyle(this.previewImage, 'width', w + 'px');
        Dom.setStyle(this.previewImage, 'height', h + 'px');
      },

      determinePreviewScalingFactor: function (previewWidth, previewHeight) {
        var widthBasedScaling = 1, heightBasedScaling = 1;

        if (previewWidth > this.maxPreviewWidth) {
          widthBasedScaling = this.maxPreviewWidth / previewWidth;
        }

        if (previewHeight > this.maxPreviewHeight) {
          heightBasedScaling = this.maxPreviewHeight / previewHeight;
        }

        if (heightBasedScaling < widthBasedScaling) {
          return heightBasedScaling;
        } else {
          return widthBasedScaling;
        }
      },

      updateRegionInputValue: function (coords) {
        var regionInput = Dom.get(this.regionInputId);
        if (regionInput) {
          if (this.fitView) {
            var newWidth = this.originalImage.offsetWidth;
            var newHeight = this.originalImage.offsetHeight;
            var width_ratio = this.original_width / newWidth;
            var height_ratio = this.original_height / newHeight;
            var original_top = Math.round(coords.top * height_ratio);
            var original_left = Math.round(coords.left * width_ratio);
            coords.top = original_top;
            coords.left = original_left;
            coords.height = Math.round(coords.height * height_ratio);
            coords.width = Math.round(coords.width * width_ratio);
          }
          regionInput.value = Lang.JSON.stringify(coords);
        }
      },

      updatePreviewLabel: function (w, h) {
        var label = this.previewLabelTemplate.replace('width', w).replace('height', h);
        Dom.get(this.thumbnailSizeLabelId).innerHTML = label;
      },

      _scaleToFit: function () {
        // undo old scaling of cropper
        this._scaleCropper(1 / this.ratio);

        // scale image
        this.ratio = this._calculateRatio();

        var scaledImageWidth = this.original_width * this.ratio;
        var scaledImageHeight = this.original_height * this.ratio;

        function fit(el) {
          Dom.setStyle(el, 'width', scaledImageWidth + 'px');
          Dom.setStyle(el, 'height', scaledImageHeight + 'px');
        }

        fit(this.container);
        fit(this.originalImage);
        fit(this.cropper.getMaskEl());
        fit(this.cropper.getWrapEl());

        // fix background behind the mask, so crop result (preview) is shown properly
        var croppedImageEl = this.cropper.getResizeMaskEl();
        Dom.setStyle(croppedImageEl, "background-size", scaledImageWidth + 'px ' + scaledImageHeight + 'px');

        this._scaleCropper(this.ratio);
      },

      _scaleCropper: function (ratio) {
        var coords, resizeObj, resizeEl;

        coords = this.cropper.getCropCoords();
        resizeObj = this.cropper.getResizeObject();

        resizeObj.set('width', coords.width * ratio);
        resizeObj.set('height', coords.height * ratio);
        resizeObj.set('minWidth', resizeObj.get('minWidth') * ratio);
        resizeObj.set('minHeight', resizeObj.get('minHeight') * ratio);

        resizeEl = this.cropper.getResizeEl();
        Dom.setStyle(resizeEl, 'left', (coords.left * ratio) + 'px');
        Dom.setStyle(resizeEl, 'top', (coords.top * ratio) + 'px');

        resizeObj._setCache();
        this.cropper._handleResizeMaskEl();
        this.cropper._syncBackgroundPosition();
     },

      _calculateRatio: function () {
        if (!this.fitView) {
          return 1;
        }
        var maxWidth = this.fullScreenMode ? Dom.getViewportWidth() - (2 * VIEW_MARGIN) : this.leftCropAreaRegion.width;
        var maxHeight = this.fullScreenMode ? Dom.getViewportHeight() - (2 * VIEW_MARGIN) : this.leftCropAreaRegion.height;
        var ratio = Math.min(maxWidth / this.original_width, maxHeight / this.original_height);

        // don't make images larger than the original
        if (ratio > 1) {
          ratio = 1;
        }

        return ratio;
      }
    });
  })();

  YAHOO.register("HippoImageCropper", YAHOO.hippo.ImageCropper, {
    version: "2.9.0", build: "2800"
  });
}
