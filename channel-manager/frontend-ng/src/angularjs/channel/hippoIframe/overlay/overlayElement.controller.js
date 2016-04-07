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

import { ComponentElement } from '../../page/element/componentElement';

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
    let boxJQueryElement = this.structureElement.getBoxElement();
    let insertedTemporaryBox;

    if (boxJQueryElement.length === 0) {
      boxJQueryElement = this._createAndInsertTemporaryBox();
      this.structureElement.setJQueryElement('iframeBoxElement', boxJQueryElement);
      insertedTemporaryBox = true;
    }

    // Set a minimal height of the element to ensure visibility / clickability.
    const originalMinHeight = boxJQueryElement[0].style.minHeight || 'unset';
    this._setMinHeightSafely(boxJQueryElement[0], MIN_HEIGHT);

    $scope.$on('$destroy', () => {
      this.OverlaySyncService.unregisterElement(this.structureElement);
      if (insertedTemporaryBox) {
        boxJQueryElement.detach();
        this.structureElement.setJQueryElement('iframe', undefined); // reset
      } else {
        this._setMinHeightSafely(boxJQueryElement[0], originalMinHeight);
      }
    });
  }

  _setMinHeightSafely(element, minHeight) {
    try {
      element.style.minHeight = minHeight;
    } catch (ignoredError) {
      // IE11 throws an error when accessing a property of an element that's no longer part of the DOM
      // (e.g. part of a previous iframe document). Ignore these errors silently.
    }
  }

  _createAndInsertTemporaryBox() {
    const startCommentJQueryElement = this.structureElement.getStartComment();
    const div = document.createElement('div');

    if (startCommentJQueryElement[0].nextSibling !== null) {
      // this should always be the case due to the presence of the HST-End marker
      startCommentJQueryElement.parent()[0].insertBefore(div, startCommentJQueryElement[0].nextSibling);
    } else {
      startCommentJQueryElement.parent()[0].appendChild(div);
    }

    return $(div);
  }

  hasExperiment() {
    return (this.structureElement instanceof ComponentElement) && !!this.structureElement.getExperimentId();
  }

  getExperimentStateIcon() {
    return (this.structureElement instanceof ComponentElement) && this.structureElement.getExperimentStateIcon();
  }
}
