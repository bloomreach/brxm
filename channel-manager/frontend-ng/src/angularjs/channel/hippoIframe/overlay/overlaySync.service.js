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

  constructor($rootScope, $window, ThrottleService) {
    'ngInject';

    this.$rootScope = $rootScope;
    this.$window = $window;

    this.overlayElements = [];
    this.observer = new MutationObserver(ThrottleService.throttle(() => this.syncIframe(), 100));
  }

  init(iframeJQueryElement, overlayJQueryElement) {
    this.iframeJQueryElement = iframeJQueryElement;
    this.overlayJQueryElement = overlayJQueryElement;

    this.iframeJQueryElement.on('load', () => this._onLoad());
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
    $(iframeWindow).on('beforeunload', () => this._onUnLoad());
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
    this._syncHeight();
    this._syncOverlayElements();
  }

  _syncHeight() {
    if (this.iframeJQueryElement && this.overlayJQueryElement) {
      const doc = this._getIframeWindow().document;

      if (doc) {
        // Reset the height, as the document height will always be at least the iframe height
        this.iframeJQueryElement.height('');
        // Prevent weird twitching at certain widths
        $(doc.documentElement).css('overflow', 'hidden');

        const height = $(doc).height();
        this.iframeJQueryElement.height(height);
        this.overlayJQueryElement.height(height);
      }
    }
  }

  _syncOverlayElements() {
    this.overlayElements.forEach((element) => this._syncElement(element));
  }

  _syncElement(structureElement) {
    const overlayJQueryElement = structureElement.getJQueryElement('overlay');
    const iframeJQueryElement = structureElement.getJQueryElement('iframe');

    const rect = iframeJQueryElement[0].getBoundingClientRect();

    overlayJQueryElement.css('top', `${rect.top}px`);
    overlayJQueryElement.css('left', `${rect.left}px`);
    overlayJQueryElement.css('height', `${rect.height}px`);
    overlayJQueryElement.css('width', `${rect.width}px`);
  }

  _getIframeWindow() {
    return this.iframeJQueryElement[0].contentWindow;
  }
}
