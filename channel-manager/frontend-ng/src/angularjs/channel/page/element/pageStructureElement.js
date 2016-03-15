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
    return this.metaData[HstConstants.LAST_MODIFIED] || 0;
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

  static isTransparentXType(metaData) {
    const metaDataXType = metaData[HstConstants.XTYPE];

    return metaDataXType !== undefined && metaDataXType.toUpperCase() === HstConstants.XTYPE_TRANSPARENT.toUpperCase();
  }

}
