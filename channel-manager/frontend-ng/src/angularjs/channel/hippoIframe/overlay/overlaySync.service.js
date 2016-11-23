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

import debounce from 'lodash.debounce';
import MutationSummary from 'mutation-summary';

class OverlaySyncService {

  constructor($rootScope, $log, $window, DomService) {
    'ngInject';

    this.$rootScope = $rootScope;
    this.$log = $log;
    this.$window = $window;
    this.DomService = DomService;

    this.overlayElements = [];

    this.syncIframeDebounced = debounce(() => this.syncIframe(), 250, {
      maxWait: 1000,
      leading: true,
      trailing: true,
    });

    this.viewPortWidth = 0;
  }

  init($base, $sheet, $iframe, $overlay) {
    this.$base = $base;
    this.$sheet = $sheet;
    this.$iframe = $iframe;
    this.$overlay = $overlay;

    this.$iframe.on('load', () => this._onLoad());
  }

  registerElement(structureElement) {
    this.overlayElements.push(structureElement);
    this._syncElement(structureElement);
  }

  unregisterElement(structureElement) {
    const index = this.overlayElements.indexOf(structureElement);
    if (index > -1) {
      this.overlayElements.splice(index, 1);
    }
  }

  _onLoad() {
    this._setHeight(0);
    this.syncIframe();

    const document = this._getIframeDocument();
    if (!document) {
      this.$log.warn('Cannot find document inside iframe');
      return;
    }
    this.observer = new MutationSummary({
      callback: () => this.onDOMChanged(),
      rootNode: document,
      observeOwnChanges: true,
      queries: [{ all: true }],
    });

    $(this.$window).on('resize.overlaysync', () => this.syncIframe());

    const iframeWindow = this._getIframeWindow();
    $(iframeWindow).on('unload', () => this._onUnLoad());

    // on iframe resize, only sync the overlay elements and not the iframe dimensions to avoid a loop
    // (resize iframe -> resize handler triggers -> resize iframe while syncing dimensions -> etc)
    $(iframeWindow).on('resize.overlaysync', () => this._syncOverlayElements());
  }

  _onUnLoad() {
    this.$rootScope.$apply(() => {
      this.overlayElements = [];
      this.observer.disconnect();
      $(this.$window).off('.overlaysync');
      $(this._getIframeWindow()).off('.overlaysync');
    });
  }

  onDOMChanged() {
    this.syncIframeDebounced();
  }

  syncIframe() {
    this._syncDimensions();
    this._syncOverlayElements();
  }

  setViewPortWidth(viewPortWidth) {
    this.viewPortWidth = viewPortWidth;
  }

  _syncDimensions() {
    if (!this.$iframe) {
      return;
    }

    const iframeDocument = this._getIframeDocument();
    if (!iframeDocument) {
      return;
    }

    this._constrainWidth();

    let iframeWidth = this._getIframeWidth(iframeDocument);
    const iframeHeight = this._getIframeHeight(iframeDocument);
    const visibleHeight = this.$base.height();

    if (iframeHeight > visibleHeight) {
      iframeWidth -= this.DomService.getScrollBarWidth();
    }

    this._setWidth(iframeWidth);
    this._setHeight(iframeHeight);
  }

  _constrainWidth() {
    this.$iframe.css('min-width', '0');

    if (this.viewPortWidth === 0) {
      // desktop mode
      this.$iframe.css('min-width', '1280px');
      this.$sheet.css('max-width', 'none');
      this.$iframe.width('');
      this.$overlay.width('');
    } else {
      // tablet or phone mode
      const width = `${this.viewPortWidth}px`;
      this.$sheet.css('max-width', width);
      this.$iframe.width(width);
    }
  }

  _getIframeWidth(iframeDocument) {
    return $(iframeDocument).width();
  }

  _getIframeHeight(iframeDocument) {
    const heightMarker = $('.hippo-channel-manager-page-height-marker', iframeDocument);
    if (heightMarker.length > 0) {
      return this.DomService.getBottom(heightMarker[0]);
    }
    return this.DomService.getLowestElementBottom(iframeDocument);
  }

  _setWidth(width) {
    this.$iframe.width(width);
    this.$iframe.css('min-width', `${width}px`);
    this.$overlay.width(width);
  }

  _setHeight(height) {
    this.$sheet.height(height);
    this.$iframe.height(height);
    this.$overlay.height(height);
  }

  _syncOverlayElements() {
    this.overlayElements.forEach(element => this._syncElement(element));
  }

  _syncElement(structureElement) {
    const overlayJQueryElement = structureElement.getOverlayElement();
    const iframeJQueryElement = structureElement.getBoxElement();

    if (iframeJQueryElement.hasClass('hst-cmseditlink')) {
      if (this.DomService.isVisible(iframeJQueryElement)) {
        overlayJQueryElement.show();
        this._drawElement(overlayJQueryElement, iframeJQueryElement);
      } else {
        overlayJQueryElement.hide();
      }
    } else {
      this._drawElement(overlayJQueryElement, iframeJQueryElement);
    }
  }

  _drawElement(overlayJQueryElement, iframeJQueryElement) {
    const rect = iframeJQueryElement[0].getBoundingClientRect();
    overlayJQueryElement.css('top', `${rect.top}px`);
    overlayJQueryElement.css('left', `${rect.left}px`);
    overlayJQueryElement.css('height', `${rect.height}px`);
    overlayJQueryElement.css('width', `${rect.width}px`);
  }

  _getIframeWindow() {
    return this.$iframe[0].contentWindow;
  }

  _getIframeDocument() {
    try {
      return this._getIframeWindow().document;
    } catch (e) {
      // ignore if cannot get document in the iframe
      return undefined;
    }
  }

}

export default OverlaySyncService;
