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

  init($base, $sheet, $scrollX, $iframe, $overlay) {
    this.$base = $base;
    this.$sheet = $sheet;
    this.$scrollX = $scrollX;
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

  getViewPortWidth() {
    return this.viewPortWidth;
  }

  _syncDimensions() {
    if (this.$iframe && this.$overlay) {
      const doc = this._getIframeDocument();

      if (doc) {
        // Avoid scrollbars from the site as they are controlled by the application.
        // Changing a style attribute on Firefox will always invoke a MutationObserver callback, even if the value has
        // not changed. To prevent ending up in a loop, only set it when the value is not already 'hidden'.
        const docEl = $(doc.documentElement);
        if (docEl.css('overflow') !== 'hidden') {
          docEl.css('overflow', 'hidden');
        }

        // resetting the height will also reset the scroll position so save (and restore) it
        const currentScrollTop = this.$base.scrollTop();

        // reset the height
        this.$sheet.height('');
        this.$iframe.height('');
        this.$scrollX.height('');

        // if there is a horizontal scrollbar (because the site is wider than the viewport),
        // the scrollbar height must be added to the iframe height.
        const isHorizontalScrollBarVisible = this._syncWidth(doc);
        this._syncHeight(doc, isHorizontalScrollBarVisible);

        // restore scroll position
        this.$base.scrollTop(currentScrollTop);
      }
    }
  }

  /**
   * Sync the width of the iframe and overlay. The width can be constrained by the viewPortWidth.
   *
   * @param iframeDocument The document object of the rendered channel in the iframe.
   * @returns {boolean} true when the site in the iframe is wider than the viewport
   * @private
   */
  _syncWidth(iframeDocument) {
    // reset min-width on iframe
    this.$iframe.css('min-width', '0');

    if (this.viewPortWidth === 0) {
      // Desktop mode
      this.$iframe.css('min-width', '1280px');
      this.$sheet.css('max-width', 'none');
      this.$iframe.width('');
      this.$overlay.width('');
    } else {
      // viewport is constrained
      const width = `${this.viewPortWidth}px`;
      this.$sheet.css('max-width', width);
      this.$iframe.width(width);

      const iframeDocumentWidth = $(iframeDocument).width();
      if (iframeDocumentWidth <= this.viewPortWidth) {
        this.$overlay.width(width);
      } else {
        // site has min-width bigger than viewport, so it needs a horizontal scrollbar
        this.$iframe.width(iframeDocumentWidth);
        this.$iframe.css('min-width', `${iframeDocumentWidth}px`);
        this.$overlay.width(iframeDocumentWidth);
        return true;
      }
    }
    return false;
  }

  _syncHeight(iframeDocument, isHorizontalScrollBarVisible) {
    // because we set 'overflow: hidden on the <html> element of a site (to be able to control scrolling), we need to
    // query both the <html> and the <body> height to find an accurate value
    const htmlHeight = $(iframeDocument.documentElement).height();
    const bodyHeight = $(iframeDocument.body).height();
    const height = Math.max(htmlHeight, bodyHeight);

    this.$sheet.height(height);
    this.$iframe.height(height);
    this.$overlay.height(height);

    // setting the absolute height on scrollX ensures that the scroll-position will be maintained when scaling
    const scrollHeight = height + (isHorizontalScrollBarVisible ? this.DomService.getScrollBarWidth() : 0);
    this.$scrollX.height(scrollHeight);
  }

  _syncOverlayElements() {
    this.overlayElements.forEach(element => this._syncElement(element));
  }

  _syncElement(structureElement) {
    const overlayJQueryElement = structureElement.getOverlayElement();
    const iframeJQueryElement = structureElement.getBoxElement();

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
