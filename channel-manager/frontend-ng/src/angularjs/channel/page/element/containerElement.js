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

import HstConstants from '../../../constants/hst.constants';
import PageStructureElement from './pageStructureElement';

class ContainerElement extends PageStructureElement {
  constructor(startCommentDomElement, metaData, commentProcessor) {
    const elements = commentProcessor.locateComponent(metaData.uuid, startCommentDomElement);
    const endCommentDomElement = elements[1];
    let boxDomElement = elements[0];

    if (PageStructureElement.isXTypeNoMarkup(metaData)) {
      boxDomElement = startCommentDomElement.parentNode;
    }

    super('container', metaData, startCommentDomElement, endCommentDomElement, boxDomElement);

    this.items = [];
  }

  isEmpty() {
    return this.items.length === 0;
  }

  isDisabled() {
    return this.isInherited() || (this.isLocked() && !this.isLockedByCurrentUser());
  }

  isInherited() {
    return this.metaData[HstConstants.INHERITED] === 'true';
  }

  getDragDirection() {
    return this.metaData[HstConstants.XTYPE] === HstConstants.XTYPE_SPAN ? 'horizontal' : 'vertical';
  }

  addComponent(component) {
    this.items.push(component);
    component.setContainer(this);
  }

  addComponentBefore(component, nextComponent) {
    const nextIndex = nextComponent ? this.items.indexOf(nextComponent) : -1;
    if (nextIndex > -1) {
      this.items.splice(nextIndex, 0, component);
    } else {
      this.items.push(component);
    }

    component.setContainer(this);
  }

  removeComponent(component) {
    const index = this.items.indexOf(component);
    if (index > -1) {
      this.items.splice(index, 1);
      component.setContainer(null);
    }
  }

  getComponents() {
    return this.items;
  }

  getComponent(componentId) {
    return this.items.find(item => item.getId() === componentId);
  }

  getComponentByIframeElement(iframeElement) {
    return this.items.find(item => item.getBoxElement().is(iframeElement));
  }

  replaceComponent(oldComponent, newComponent) {
    const index = this.items.indexOf(oldComponent);
    if (index !== -1) {
      this.items[index] = newComponent;
    }
  }

  hasComponent(component) {
    return this.items.indexOf(component) !== -1;
  }

  getHstRepresentation() {
    return {
      id: this.getId(),
      lastModifiedTimestamp: this.getLastModified(),
      children: this.items.map(item => item.getId()),
    };
  }
}

export default ContainerElement;
