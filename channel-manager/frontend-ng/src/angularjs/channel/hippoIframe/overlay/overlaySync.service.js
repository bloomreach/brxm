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

function throttle(callback, limit) {
  let wait = false;            // Initially, we're not waiting
  return () => {         // We return a throttled function
    if (!wait) {               // If we're not waiting
      callback.call();         // Execute users function
      wait = true;             // Prevent future invocations
      setTimeout(() => { // After a period of time
        wait = false;          // And allow future invocations
      }, limit);
    }
  };
}

function getParentOffset(structureElement) {
  if (structureElement.type === 'component') {
    const containerRect = structureElement.container.getJQueryElement('iframe')[0].getBoundingClientRect();

    return [containerRect.top, containerRect.left];
  }
  return [0, 0];
}

function syncElement(structureElement) {
  const overlayJQueryElement = structureElement.getJQueryElement('overlay');
  const iframeJQueryElement = structureElement.getJQueryElement('iframe');

  const rect = iframeJQueryElement[0].getBoundingClientRect();
  const [parentTop, parentLeft] = getParentOffset(structureElement);

  overlayJQueryElement.css('top', (rect.top - parentTop) + 'px');
  overlayJQueryElement.css('left', (rect.left - parentLeft) + 'px');
  overlayJQueryElement.css('height', rect.height + 'px');
  overlayJQueryElement.css('width', rect.width + 'px');
}

export class OverlaySyncService {

  constructor($rootScope, $window) {
    'ngInject';

    this.$rootScope = $rootScope;

    this.overlayElements = [];
    this.observer = new MutationObserver(throttle(() => this._syncIframe(), 100));
    $($window).on('resize', () => this._syncIframe());
  }

  startObserving(iframe, overlay) {
    this.iframe = iframe;
    this.overlay = overlay;
    this.observer.observe(iframe[0].contentWindow.document, {
      childList: true,
      attributes: true,
      characterData: true,
      subtree: true,
    });

    $(this.iframe[0].contentWindow).on('beforeunload', () => {
      this.$rootScope.$apply(() => {
        this.overlayElements = [];
        this.observer.disconnect();
      });
    });
  }

  registerElement(structureElement) {
    this.overlayElements.push(structureElement);
    syncElement(structureElement);
  }

  _syncIframe() {
    this._syncHeight();
    this._syncOverlayElements();
  }

  _syncHeight() {
    if (this.iframe && this.overlay) {
      const html = this.iframe[0].contentWindow.document.documentElement;

      if (html !== null) {
        const height = html.offsetHeight;

        // Prevent weird twitching at certain widths
        html.style.overflow = 'hidden';

        this.iframe.height(height);
        this.overlay.height(height);
      }
    }
  }

  _syncOverlayElements() {
    this.overlayElements.forEach((element) => syncElement(element));
  }

}
