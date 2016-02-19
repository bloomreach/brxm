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

export class OverlayElementCtrl {
  constructor($scope, OverlaySyncService) {
    'ngInject';

    this.OverlaySyncService = OverlaySyncService;
    this.ensureVisibilityOfEmptyContainer($scope);
  }

  // overlay sync'ing can only start after the structure element was bound to the overlay dom element.
  onLink(overlayJQueryElement) {
    this.structureElement.setJQueryElement('overlay', overlayJQueryElement);
    this.OverlaySyncService.registerElement(this.structureElement);
  }

  ensureVisibilityOfEmptyContainer($scope) {
    if (this.structureElement.type === 'container' && this.structureElement.isEmpty()) {
      const iframeDomElement = this.structureElement.getJQueryElement('iframe')[0];
      const minHeight = iframeDomElement.style.minHeight || 'auto';
      iframeDomElement.style.minHeight = '40px';

      // reset styling when element is destroyed
      $scope.$on('$destroy', () => {
        iframeDomElement.style.minHeight = minHeight;
      });
    }
  }
}
