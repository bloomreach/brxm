/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

class OverlayService {

  constructor($rootScope, $log, DomService, PageStructureService) {
    'ngInject';

    this.$rootScope = $rootScope;
    this.$log = $log;
    this.DomService = DomService;
    this.PageStructureService = PageStructureService;

    PageStructureService.registerChangeListener(() => this.sync());
  }

  init(iframeJQueryElement) {
    this.iframeJQueryElement = iframeJQueryElement;
    this.iframeJQueryElement.on('load', () => this._onLoad());
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
    this.$rootScope.$apply(() => {
      this.observer.disconnect();
      delete this.overlay;
    });
  }

  _initOverlay() {
    this.overlay = $('<div id="hippo-overlay"></div>');
    $(this.iframeWindow.document.body).append(this.overlay);
    this._updateModeClass();
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
    if (this.overlay) {
      const currentOverlayElements = new Set();

      this._forAllStructureElements((structureElement) => {
        this._syncElement(structureElement);

        const overlayElement = structureElement.getOverlayElement()[0];
        currentOverlayElements.add(overlayElement);
      });

      this._tidyOverlay(currentOverlayElements);
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
    const escapedLabel = this.DomService.escapeHtml(structureElement.getLabel());
    const overlayElement = $(`
      <div class="hippo-overlay-element hippo-overlay-element-${structureElement.getType()}">
        <span class="hippo-overlay-label">
          <span class="hippo-overlay-label-text">${escapedLabel}</span>
        </span>
      </div>`);

    if (structureElement.getType() === 'component') {
      overlayElement.click((event) => {
        event.stopPropagation();
        this.$rootScope.$apply(() => {
          this.PageStructureService.showComponentProperties(structureElement);
        });
      });
    }

    structureElement.setOverlayElement(overlayElement);

    const boxElement = structureElement.prepareBoxElement();
    boxElement.addClass('hippo-overlay-box');

    this._syncElements(structureElement, overlayElement);

    this.overlay.append(overlayElement);
  }

  _syncElements(structureElement, overlayElement) {
    const boxElement = structureElement.getBoxElement();

    if (this._isElementVisible(structureElement, boxElement)) {
      overlayElement.show();
      overlayElement.toggleClass('hippo-overlay-element-container-empty', this._isEmptyContainer(structureElement));
      this._syncPosition(overlayElement, boxElement);
    } else {
      overlayElement.hide();
    }
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
    overlayElement.css('top', `${rect.top + this.iframeWindow.scrollY}px`);
    overlayElement.css('left', `${rect.left + this.iframeWindow.scrollX}px`);
    overlayElement.css('height', `${rect.height}px`);
    overlayElement.css('width', `${rect.width}px`);
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
