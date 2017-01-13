/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

import MutationSummary from 'mutation-summary';
import contentLinkSvg from '../../../images/html/edit-document.svg';
import menuLinkSvg from '../../../images/html/edit-menu.svg';

class OverlayService {

  constructor($rootScope, $log, $translate, CmsService, DomService, PageStructureService, ScalingService) {
    'ngInject';

    this.$rootScope = $rootScope;
    this.$log = $log;
    this.$translate = $translate;
    this.CmsService = CmsService;
    this.DomService = DomService;
    this.PageStructureService = PageStructureService;
    this.ScalingService = ScalingService;

    PageStructureService.registerChangeListener(() => this.sync());
  }

  init(iframeJQueryElement) {
    this.iframeJQueryElement = iframeJQueryElement;
    this.iframeJQueryElement.on('load', () => this._onLoad());
  }

  onEditMenu(callback) {
    this.editMenuHandler = callback;
  }

  _onLoad() {
    this.iframeWindow = this.iframeJQueryElement[0].contentWindow;

    this._initOverlay();

    this.observer = new MutationSummary({
      callback: () => this.sync(),
      rootNode: this.iframeWindow.document,
      queries: [{ all: true }],
    });

    const win = $(this.iframeWindow);
    win.on('unload', () => this._onUnload());
    win.on('resize', () => this.sync());
  }

  _onUnload() {
    this.ScalingService.onIframeUnload();
    this.$rootScope.$apply(() => {
      this.observer.disconnect();
      delete this.overlay;
    });
  }

  _initOverlay() {
    this.overlay = $('<div id="hippo-overlay"></div>');
    $(this.iframeWindow.document.body).append(this.overlay);
    this._updateModeClass();

    this.overlay.mousedown((event) => {
      // let right-click trigger context-menu instead of starting dragging
      event.preventDefault();

      // we already dispatch a mousedown event on the same location, so don't propagate this one to avoid that
      // dragula receives one mousedown event too many
      event.stopPropagation();

      this._onOverlayMouseDown(event);
    });
  }

  setMode(mode) {
    this.mode = mode;
    this._updateModeClass();
  }

  _updateModeClass() {
    if (this.iframeWindow) {
      const html = $(this.iframeWindow.document.documentElement);
      html.toggleClass('hippo-mode-edit', this._isEditMode());
      // don't call sync() explicitly: the DOM mutation will trigger it automatically
    }
  }

  _isEditMode() {
    return this.mode === 'edit';
  }

  sync() {
    if (this.overlay && !this.ScalingService.isAnimating()) {
      const currentOverlayElements = new Set();

      this._forAllStructureElements((structureElement) => {
        this._syncElement(structureElement);

        const overlayElement = structureElement.getOverlayElement()[0];
        currentOverlayElements.add(overlayElement);
      });

      this._tidyOverlay(currentOverlayElements);
    }
  }

  attachComponentMouseDown(callback) {
    this.componentMouseDownCallback = callback;
  }

  detachComponentMouseDown() {
    this.componentMouseDownCallback = null;
  }

  _onOverlayMouseDown(event) {
    const target = $(event.target);
    if (target.hasClass('hippo-overlay-element-component') && this.componentMouseDownCallback) {
      const component = this.PageStructureService.getComponentByOverlayElement(target);
      if (component) {
        this.componentMouseDownCallback(event, component);
      }
    }
  }

  _forAllStructureElements(callback) {
    this.PageStructureService.getContainers().forEach((container) => {
      callback(container);
      container.getComponents().forEach(callback);
    });
    this.PageStructureService.getEmbeddedLinks().forEach(callback);
  }

  _syncElement(structureElement) {
    const overlayElement = structureElement.getOverlayElement();

    if (overlayElement) {
      this._syncElements(structureElement, overlayElement);
    } else {
      this._addOverlayElement(structureElement);
    }
  }

  _addOverlayElement(structureElement) {
    const overlayElement = $(`
      <div class="hippo-overlay-element hippo-overlay-element-${structureElement.getType()}">
      </div>`);

    this._addLabel(structureElement, overlayElement);
    this._addMarkupAndBehavior(structureElement, overlayElement);

    structureElement.setOverlayElement(overlayElement);
    structureElement.prepareBoxElement();

    this._syncElements(structureElement, overlayElement);

    this.overlay.append(overlayElement);
  }

  _addLabel(structureElement, overlayElement) {
    const escapedLabel = this.DomService.escapeHtml(structureElement.getLabel());
    if (escapedLabel.length > 0) {
      overlayElement.append(`
        <span class="hippo-overlay-label">
          <span class="hippo-overlay-label-text">${escapedLabel}</span>
        </span>
      `);
    }
  }

  _addMarkupAndBehavior(structureElement, overlayElement) {
    switch (structureElement.getType()) {
      case 'component':
        this._addComponentClickHandler(structureElement, overlayElement);
        break;
      case 'content-link':
        this._addLinkMarkup(overlayElement, contentLinkSvg, 'IFRAME_OPEN_DOCUMENT');
        this._addContentLinkClickHandler(structureElement, overlayElement);
        break;
      case 'menu-link':
        this._addLinkMarkup(overlayElement, menuLinkSvg, 'IFRAME_EDIT_MENU');
        this._addMenuLinkClickHandler(structureElement, overlayElement);
        break;
      default:
        break;
    }
  }

  _addComponentClickHandler(structureElement, overlayElement) {
    overlayElement.click((event) => {
      event.stopPropagation();
      this.PageStructureService.showComponentProperties(structureElement);
    });
  }

  _addLinkMarkup(overlayElement, svg, titleKey) {
    overlayElement.addClass('hippo-overlay-element-link');
    overlayElement.attr('title', this.$translate.instant(titleKey));
    overlayElement.append(svg);
  }

  _addContentLinkClickHandler(structureElement, overlayElement) {
    this._addClickHandler(overlayElement, () => {
      this.CmsService.publish('open-content', structureElement.getUuid());
    });
  }

  _addMenuLinkClickHandler(structureElement, overlayElement) {
    this._addClickHandler(overlayElement, () => {
      this.$rootScope.$apply(() => {
        this.editMenuHandler(structureElement.getUuid());
      });
    });
  }

  _addClickHandler(overlayElement, handler) {
    overlayElement.click((event) => {
      event.stopPropagation();
      handler();
    });
  }

  _syncElements(structureElement, overlayElement) {
    const boxElement = structureElement.getBoxElement();
    boxElement.addClass('hippo-overlay-box');

    overlayElement.toggleClass('hippo-overlay-element-visible', this._isElementVisible(structureElement, boxElement));
    overlayElement.toggleClass('hippo-overlay-element-container-empty', this._isEmptyContainer(structureElement));
    this._syncPosition(overlayElement, boxElement);
  }

  _isElementVisible(structureElement, boxElement) {
    switch (structureElement.getType()) {
      case 'container':
        return this._isEditMode() && structureElement.isEmpty();
      case 'content-link':
        return !this._isEditMode() && this.DomService.isVisible(boxElement);
      case 'menu-link':
        return this._isEditMode() && this.DomService.isVisible(boxElement);
      default:
        return this._isEditMode();
    }
  }

  _isEmptyContainer(structureElement) {
    return structureElement.getType() === 'container' && structureElement.isEmpty();
  }

  _syncPosition(overlayElement, boxElement) {
    const rect = boxElement[0].getBoundingClientRect();

    let top = rect.top;
    let left = rect.left;
    let width = rect.width;
    let height = rect.height;

    // Include scroll position since coordinates are relative to page but rect is relative to viewport.
    // IE11 does not support window.scrollX and window.scrollY, so use window.pageXOffset and window.pageYOffset
    left += this.iframeWindow.pageXOffset;
    top += this.iframeWindow.pageYOffset;

    // Compensate for the scale factor. The page elements are scaled, but their position and size is relative to the
    // viewport, which does not scale. Yet the position of the overlay elements is relative to the overlay root, which
    // is already scaled. The scaling of the page element rectangles therefore has to be reverted in their overlay
    // counterparts to avoid scaling the overlay twice.
    // In addition, the scaling transforms to the top-right corner. Hence the page shifts to the right when scaled, so
    // this shift has to be subtracted from the left of each overlay rectangle.
    const scale = this.ScalingService.getScaleFactor();
    if (scale < 1) {
      const windowWidth = this.iframeWindow.document.documentElement.clientWidth;
      const shiftX = windowWidth - (windowWidth * scale);
      left = (left - shiftX) / scale;
      top /= scale;
      width /= scale;
      height /= scale;
    }

    overlayElement.css('top', `${top}px`);
    overlayElement.css('left', `${left}px`);
    overlayElement.css('width', `${width}px`);
    overlayElement.css('height', `${height}px`);
  }

  _tidyOverlay(elementsToKeep) {
    const overlayElements = this.overlay.children();

    // to improve performance, only iterate when there are elements to remove
    if (overlayElements.length > elementsToKeep.size) {
      overlayElements.each((index, element) => {
        if (!elementsToKeep.has(element)) {
          $(element).remove();
        }
      });
    }
  }
}

export default OverlayService;
