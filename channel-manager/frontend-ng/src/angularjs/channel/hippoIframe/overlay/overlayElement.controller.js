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

const MIN_HEIGHT = '40px';

export class OverlayElementCtrl {
  constructor($scope, $element, OverlaySyncService) {
    'ngInject';

    this.OverlaySyncService = OverlaySyncService;
    this.structureElement.setJQueryElement('overlay', $element);
    this._prepareIframeElement($scope);

    OverlaySyncService.registerElement(this.structureElement);
  }

  getLabel() {
    return this.structureElement.getLabel();
  }

  _prepareIframeElement($scope) {
    const iframeJQueryElement = this.structureElement.getJQueryElement('iframe');
    let iframeJQueryDomRoot = iframeJQueryElement;
    const isIframeJQueryElementInserted = this.structureElement.hasNoIFrameDomElement();

    if (isIframeJQueryElementInserted) {
      // iframeJQueryElement refers to HST-comment, insert a placeholder <div>
      const div = document.createElement('div');
      if (iframeJQueryElement[0].nextSibling !== null) {
        // this should always be the case due to the presence of the HST-End marker
        iframeJQueryElement.parent()[0].insertBefore(div, iframeJQueryElement[0].nextSibling);
      } else {
        iframeJQueryElement.parent()[0].appendChild(div);
      }

      iframeJQueryDomRoot = $(div);
      this.structureElement.setJQueryElement('iframe', iframeJQueryDomRoot);
    }

    // Set a minimal height of the element to ensure visibility / clickability.
    const minHeight = iframeJQueryDomRoot[0].style.minHeight || 'auto';
    iframeJQueryDomRoot[0].style.minHeight = MIN_HEIGHT;

    $scope.$on('$destroy', () => {
      this.OverlaySyncService.unregisterElement(this.structureElement);
      if (isIframeJQueryElementInserted) {
        iframeJQueryDomRoot.detach();
        this.structureElement.setJQueryElement('iframe', iframeJQueryElement); // reset
      } else {
        iframeJQueryElement[0].style.minHeight = minHeight;
      }
    });
  }
}
