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

import { HstConstants } from '../../../api/hst.constants';

export class PageStructureElement {
  constructor(type, metaData, startCommentDomElement, endCommentDomElement, boxDomElement) {
    this.type = type;
    this.metaData = metaData;
    this.jQueryElements = {};

    this.setJQueryElement('iframeStartComment', $(startCommentDomElement));
    this.setJQueryElement('iframeEndComment', $(endCommentDomElement));
    this.setJQueryElement('iframeBoxElement', $(boxDomElement));
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

  /**
   * Remove all children of the start comment's parent between the start and en comments (inclusive)
   */
  removeFromDOM() {
    const start = this.getJQueryElement('iframeStartComment')[0];
    const end = this.getJQueryElement('iframeEndComment')[0];
    const parent = start.parentNode;
    const children = parent.childNodes;
    let doDelete = false;

    for (let i = 0; i < children.length; i++) {
      const child = children[i];
      if (child === start) {
        doDelete = true;
      }
      if (doDelete) {
        parent.removeChild(child);
      }
      if (child === end) {
        break;
      }
    }
  }

  replaceInIframe(htmlFragment) {
    const start = this.getJQueryElement('iframeStartComment')[0];
    const end = this.getJQueryElement('iframeEndComment')[0];
    const insertAfter = start.previousSibling;
    const insertBefore = end.nextSibling;
    const parent = start.parentNode;

    this.removeFromDOM();

    if (insertBefore) {
      $(insertBefore).before(htmlFragment);
      this.setJQueryElement('iframeEndComment', $(insertBefore.previousSibling));
    } else {
      $(parent).append(htmlFragment);
      this.setJQueryElement('iframeEndComment', $(parent.lastChild));
    }

    if (insertAfter) {
      this.setJQueryElement('iframeStartComment', $(insertAfter.nextSibling));
    } else {
      this.setJQueryElement('iframeStartComment', $(parent.firstChild));
    }
  }

  static isTransparentXType(metaData) {
    const metaDataXType = metaData[HstConstants.XTYPE];

    return metaDataXType !== undefined && metaDataXType.toUpperCase() === HstConstants.XTYPE_TRANSPARENT.toUpperCase();
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
}
