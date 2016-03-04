/*
 * Copyright 2015-2016 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
const ANGULAR_MATERIAL_SIDENAV_EASING = [0.25, 0.8, 0.25, 1];
const ANGULAR_MATERIAL_SIDENAV_ANIMATION_DURATION_MS = 400;
const SIDENAVS = ['components'];

export class ChannelCtrl {

  constructor($log, $mdSidenav, ChannelService, ComponentsService, MountService, PageMetaDataService) {
    'ngInject';

    this.$log = $log;
    this.MountService = MountService;
    this.PageMetaDataService = PageMetaDataService;

    this.$mdSidenav = $mdSidenav;

    this.iframeUrl = ChannelService.getUrl();
    this.isEditMode = false;
    this.isCreatingPreview = false;

    ComponentsService.getComponents().then((components) => {
      this.$log.info('Component toolkit:', components);
      this.components = components;
    });
  }

  toggleEditMode() {
    if (!this.isEditMode && !this.PageMetaDataService.hasPreviewConfiguration()) {
      this._createPreviewConfiguration();
    } else {
      this.isEditMode = !this.isEditMode;
    }
    this._closeSidenavs();
  }

  _closeSidenavs() {
    SIDENAVS.forEach((sidenav) => {
      if (this.isSidenavOpen(sidenav)) {
        this.$mdSidenav(sidenav).close();
      }
    });
    this.transformHippoIFrame();
  }

  _createPreviewConfiguration() {
    const currentMountId = this.PageMetaDataService.getMountId();
    this.isCreatingPreview = true;
    this.MountService.createPreviewConfiguration(currentMountId)
      .then(() => {
        this.isEditMode = true;
      })
      // TODO: handle error response
      .finally(() => {
        this.isCreatingPreview = false;
      });
  }

  toggleSidenav(name) {
    SIDENAVS.forEach((sidenav) => {
      if (sidenav !== name && this.isSidenavOpen(sidenav)) {
        this.$mdSidenav(sidenav).close();
      }
    });
    this.$mdSidenav(name).toggle();
    this.transformHippoIFrame();
  }

  isSidenavOpen(name) {
    return this.$mdSidenav(name).isOpen();
  }

  isAnySidenavOpen() {
    return SIDENAVS.some((sidenav) => {
      return this.isSidenavOpen(sidenav);
    });
  }

  transformHippoIFrame() {
    const sidenavWidth = $('.md-sidenav-left').width();
    const hippoIframe = $('hippo-iframe');

    function getCanvasWidth() {
      const iframeCanvasWidth = $('.hippo-iframe-canvas', hippoIframe).width();
      const iframeShift = parseInt(hippoIframe.css('margin-left'), 10);
      return iframeCanvasWidth + iframeShift;
    }

    function calculateIFrameShift(canvasWidth, viewportWidth) {
      const canvasBorderWidth = canvasWidth - viewportWidth;
      return Math.min(canvasBorderWidth, sidenavWidth);
    }

    function shiftIFrame(pixels) {
      hippoIframe.velocity('finish');
      hippoIframe.velocity({
        'margin-left': pixels,
      }, {
        duration: ANGULAR_MATERIAL_SIDENAV_ANIMATION_DURATION_MS,
        easing: ANGULAR_MATERIAL_SIDENAV_EASING,
      });
    }

    if (this.isAnySidenavOpen()) {
      const canvasWidth = getCanvasWidth();
      const viewportWidth = this.viewport === 'desktop' ? canvasWidth : this.viewportWidth;

      const shiftPixels = calculateIFrameShift(canvasWidth, viewportWidth);
      shiftIFrame(shiftPixels);

      const visibleCanvasWidth = canvasWidth - sidenavWidth;
      if (visibleCanvasWidth < viewportWidth) {
        this.iframeScale = visibleCanvasWidth / viewportWidth;
      } else {
        this.iframeScale = 1;
      }
    } else {
      shiftIFrame(0);
      this.iframeScale = 1;
    }
  }
}
