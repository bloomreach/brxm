/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

import HstConstants from '../../../constants/hst.constants';

class PageStructureElement {
  constructor(type, metaData, startCommentDomElement, endCommentDomElement, boxDomElement) {
    this.type = type;
    this.metaData = metaData;
    this.jQueryElements = {};
    this.headContributions = [];

    this.setStartComment($(startCommentDomElement));
    this.setEndComment($(endCommentDomElement));
    this.setBoxElement($(boxDomElement));
  }

  setJQueryElement(elementType, element) {
    this.jQueryElements[elementType] = element;
  }

  getJQueryElement(elementType) {
    return this.jQueryElements[elementType];
  }

  getId() {
    return this.metaData.uuid;
  }

  getLabel() {
    let label = this.metaData[HstConstants.LABEL];
    if (label === 'null') {
      label = this.type; // no label available, fallback to type.
    }
    return label;
  }

  hasLabel() {
    return true;
  }

  getLastModified() {
    const lastModified = this.metaData[HstConstants.LAST_MODIFIED];
    return lastModified ? parseInt(lastModified, 10) : 0;
  }

  isLocked() {
    return angular.isDefined(this.getLockedBy());
  }

  getLockedBy() {
    return this.metaData[HstConstants.LOCKED_BY];
  }

  isLockedByCurrentUser() {
    return this.metaData[HstConstants.LOCKED_BY_CURRENT_USER] === 'true';
  }

  hasNoIFrameDomElement() {
    return this.metaData.hasNoDom;
  }

  getRenderUrl() {
    return this.metaData[HstConstants.RENDER_URL];
  }

  getType() {
    return this.type;
  }

  /**
   * Replace container DOM elements with the given markup
   */
  replaceDOM($newMarkup, onLoadCallback) {
    const startComment = this.getStartComment();
    const endComment = this.getEndComment();

    const node = this._removeSiblingsUntil(startComment[0], endComment[0]);

    if (!node) {
      throw new Error('Inconsistent PageStructureElement: startComment and endComment elements should be sibling');
    }

    // For containers of type NoMarkup, D&D may have placed a moved component after the end comment.
    // This would lead to lingering, duplicate components in the DOM. To get rid of these "misplaced"
    // elements, we also remove all subsequent elements. See CHANNELMGR-1030.
    if (PageStructureElement.isXTypeNoMarkup(this.metaData)) {
      this._removeSiblingsUntil(endComment[0].nextSibling); // Don't remove the end marker
    }

    // Delay the onLoad callback until all images are fully downloaded. Called once per image.
    const images = $newMarkup.find('img, [type="image"]').one('load', onLoadCallback);

    endComment.replaceWith($newMarkup);

    // If no images are being loaded we can execute the onLoad callback right away.
    if (images.length === 0) {
      onLoadCallback();
    }
  }

  _removeSiblingsUntil(startNode, endNode) {
    const parentNode = startNode.parentNode;
    let node = startNode;
    while (node && node !== endNode) {
      const toBeRemoved = node;
      node = node.nextSibling;

      // IE11 does not understand node.remove(), so use parentNode.removeChild() instead
      parentNode.removeChild(toBeRemoved);
    }
    return node;
  }

  static isXTypeNoMarkup(metaData) {
    const metaDataXType = metaData[HstConstants.XTYPE];

    return metaDataXType !== undefined && metaDataXType.toUpperCase() === HstConstants.XTYPE_NOMARKUP.toUpperCase();
  }

  getStartComment() {
    return this.getJQueryElement('iframeStartComment');
  }

  setStartComment(newJQueryStartComment) {
    this.setJQueryElement('iframeStartComment', newJQueryStartComment);
  }

  getEndComment() {
    return this.getJQueryElement('iframeEndComment');
  }

  setEndComment(newJQueryEndComment) {
    this.setJQueryElement('iframeEndComment', newJQueryEndComment);
  }

  getBoxElement() {
    return this.getJQueryElement('iframeBoxElement');
  }

  setBoxElement(newJQueryBoxElement) {
    this.setJQueryElement('iframeBoxElement', newJQueryBoxElement);
  }

  prepareBoxElement() {
    let boxElement = this.getBoxElement();
    if (!boxElement || boxElement.length === 0) {
      boxElement = this._insertGeneratedBoxElement();
      this.setBoxElement(boxElement);
      this.isBoxElementGenerated = true;
    }
    return boxElement;
  }

  _insertGeneratedBoxElement() {
    // sub-classes can override this method to generate a custom placeholder box element
    const startComment = this.getStartComment();
    const generatedBox = this.generateBoxElement();

    if (startComment.next().length > 0) {
      // this should always be the case due to the presence of the end comment
      generatedBox.insertAfter(startComment);
    } else {
      generatedBox.appendTo(startComment.parent());
    }

    return generatedBox;
  }

  generateBoxElement() {
    return $('<div class="hippo-overlay-box-empty"></div>');
  }

  isBoxElementGenerated() {
    return this.isBoxElementGenerated;
  }

  getOverlayElement() {
    return this.getJQueryElement('overlay');
  }

  setOverlayElement(newJQueryOverlayElement) {
    this.setJQueryElement('overlay', newJQueryOverlayElement);
  }

  containsDomElement(domElement) {
    const endCommentNode = this.getEndComment()[0];
    let node = this.getStartComment()[0].nextSibling;
    while (node && node !== endCommentNode) {
      // IE only supports contains() for elements, which have nodeType 1
      if (node.nodeType === 1 && node.contains(domElement)) {
        return true;
      }
      node = node.nextSibling;
    }
    return false;
  }

  setHeadContributions(headContributions) {
    this.headContributions = headContributions;
  }

  getHeadContributions() {
    return this.headContributions;
  }
}

export default PageStructureElement;
