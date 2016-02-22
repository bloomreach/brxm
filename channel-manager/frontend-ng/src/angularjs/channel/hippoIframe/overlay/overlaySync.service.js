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

    this.overlayElements = [];
    this.observer = new MutationObserver(ThrottleService.throttle(() => this._syncIframe(), 100));
    $($window).on('resize', () => this._syncIframe());
  }

  init(iframeJQueryElement, overlayJQueryElement) {
    this.iframeJQueryElement = iframeJQueryElement;
    this.overlayJQueryElement = overlayJQueryElement;
  }

  startObserving() {
    this.observer.observe(this.iframeJQueryElement[0].contentWindow.document, {
      childList: true,
      attributes: true,
      characterData: true,
      subtree: true,
    });

    $(this.iframeJQueryElement[0].contentWindow).on('beforeunload', () => {
      this.$rootScope.$apply(() => {
        this.overlayElements = [];
        this.observer.disconnect();
      });
    });
  }

  registerElement(structureElement) {
    this.overlayElements.push(structureElement);
    this._syncElement(structureElement);
  }

  _syncIframe() {
    this._syncHeight();
    this._syncOverlayElements();
  }

  _syncHeight() {
    if (this.iframeJQueryElement && this.overlayJQueryElement) {
      const html = this.iframeJQueryElement[0].contentWindow.document.documentElement;

      if (html !== null) {
        const height = html.offsetHeight;

        // Prevent weird twitching at certain widths
        html.style.overflow = 'hidden';

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

    overlayJQueryElement.css('top', rect.top + 'px');
    overlayJQueryElement.css('left', rect.left + 'px');
    overlayJQueryElement.css('height', rect.height + 'px');
    overlayJQueryElement.css('width', rect.width + 'px');
  }

}
