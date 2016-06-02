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

  constructor($scope, $document, $element, $translate, ExperimentStateService, OverlaySyncService) {
    'ngInject';

    this.$document = $document;
    this.$scope = $scope;
    this.$translate = $translate;
    this.ExperimentStateService = ExperimentStateService;
    this.OverlaySyncService = OverlaySyncService;

    this.structureElement.setJQueryElement('overlay', $element);
    this._prepareIframeElement($scope);
    $element.attr('qa-label', this.structureElement.getLabel());

    OverlaySyncService.registerElement(this.structureElement);
  }

  isLabelIconVisible() {
    return this._isComponent() && this.ExperimentStateService.hasExperiment(this.structureElement);
  }

  getLabelText() {
    return this.ExperimentStateService.getExperimentStateLabel(this.structureElement) || this.structureElement.getLabel();
  }

  isLockIconVisible() {
    return this._isContainer() && this.structureElement.isDisabled();
  }

  getQaExperimentId() {
    return this._isComponent() ? this.ExperimentStateService.getExperimentId(this.structureElement) : undefined;
  }

  getQaLockedLabel() {
    return this.structureElement.isInherited() ? 'inheritance' : this.structureElement.getLockedBy();
  }

  getLockedByText() {
    let result;
    if (this.structureElement.isInherited()) {
      result = this.$translate.instant('CONTAINER_INHERITED');
    } else {
      result = this.$translate.instant('CONTAINER_LOCKED_BY', { user: this.structureElement.getLockedBy() });
    }
    return result;
  }

  _isComponent() {
    return this.structureElement.type === 'component';
  }

  _isContainer() {
    return this.structureElement.type === 'container';
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
    const div = this.$document[0].createElement('div');

    if (startCommentJQueryElement[0].nextSibling !== null) {
      // this should always be the case due to the presence of the HST-End marker
      startCommentJQueryElement.parent()[0].insertBefore(div, startCommentJQueryElement[0].nextSibling);
    } else {
      startCommentJQueryElement.parent()[0].appendChild(div);
    }

    return angular.element(div);
  }
}
