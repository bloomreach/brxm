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

export class OverlaySyncService {

  constructor($rootScope, $window, ThrottleService, DomService) {
    'ngInject';

    this.$rootScope = $rootScope;
    this.$window = $window;
    this.DomService = DomService;

    this.overlayElements = [];
    this.observer = new MutationObserver(ThrottleService.throttle(() => this.syncIframe(), 100));
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

    const iframeWindow = this._getIframeWindow();
    this.observer.observe(iframeWindow.document, {
      childList: true,
      attributes: true,
      characterData: true,
      subtree: true,
    });
    $(iframeWindow).on('unload', () => this._onUnLoad());
    $(this.$window).on('resize.overlaysync', () => this.syncIframe());
  }

  _onUnLoad() {
    this.$rootScope.$apply(() => {
      this.overlayElements = [];
      this.observer.disconnect();
      $(this.$window).off('.overlaysync');
    });
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
      const doc = this._getIframeWindow().document;

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

        // Reset the height
        this.$iframe.height('');
        this.$scrollX.height('');

        // if set to true, the scrollbar height is added to the iframe height,
        const horizontalScrollBar = this._syncWidth(doc);
        this._syncHeight(doc, horizontalScrollBar);

        // restore scrolltop
        this.$base.scrollTop(currentScrollTop);
      }
    }
  }

  /**
   * Sync the width of the iframe and overlay. The width can be constrained by the viewPortWidth. If it detects that
   * the site in the iframe is wider than the sheet, it returns true.
   *
   * @param doc The document object
   * @returns {boolean} true when displaying a horizontal scrollbar
   * @private
   */
  _syncWidth(doc) {
    // reset min-width on iframe
    this.$iframe.css('min-width', '0');

    if (this.viewPortWidth === 0) {
      // Desktop mode - no width constraints
      this.$sheet.css('max-width', 'none');
      this.$iframe.width('');
      this.$overlay.width('');
    } else {
      // viewport is constrained
      const width = `${this.viewPortWidth}px`;
      this.$sheet.css('max-width', width);
      this.$iframe.width(width);

      const docWidth = $(doc).width();
      if (docWidth <= this.viewPortWidth) {
        this.$overlay.width(width);
      } else {
        // site has min-width bigger than viewport
        this.$iframe.width(docWidth);
        this.$iframe.css('min-width', `${docWidth}px`);
        this.$overlay.width(docWidth);
        return true;
      }
    }
    return false;
  }

  _syncHeight(doc, horizontalScrollBar) {
    const height = $(doc.body).height() + (horizontalScrollBar ? this.DomService.getScrollBarWidth() : 0);
    this.$iframe.height(height);
    this.$overlay.height(height);
    // setting the absolute height on scrollX ensures that the scroll-position can be maintained when scaling
    this.$scrollX.height(height);
  }

  _syncOverlayElements() {
    this.overlayElements.forEach((element) => this._syncElement(element));
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

}
