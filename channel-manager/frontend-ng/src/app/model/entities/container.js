/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

import * as HstConstants from '../constants';
import { ComponentEntity } from './component-entity';

export class Container extends ComponentEntity {
  constructor(meta) {
    super(meta);

    this._items = [];
  }

  getType() {
    return 'container';
  }

  isEmpty() {
    return !this._items.length;
  }

  isDisabled() {
    return this.isInherited() || (this.isLocked() && !this.isLockedByCurrentUser());
  }

  isInherited() {
    return this._meta[HstConstants.INHERITED] === 'true';
  }

  isXTypeNoMarkup() {
    const metaDataXType = this._meta[HstConstants.XTYPE];

    return !!(metaDataXType && metaDataXType.toUpperCase() === HstConstants.XTYPE_NOMARKUP.toUpperCase());
  }

  isXPageEditable() {
    return this._meta[HstConstants.XPAGE_EDITABLE] === 'true';
  }

  getDragDirection() {
    return this._meta[HstConstants.XTYPE] === HstConstants.XTYPE_SPAN ? 'horizontal' : 'vertical';
  }

  addComponent(component) {
    this._items.push(component);
    component.setContainer(this);
  }

  addComponentBefore(component, nextComponent) {
    const nextIndex = nextComponent ? this._items.indexOf(nextComponent) : -1;
    if (nextIndex > -1) {
      this._items.splice(nextIndex, 0, component);
    } else {
      this._items.push(component);
    }
    component.setContainer(this);
  }

  removeComponent(component) {
    const index = this._items.indexOf(component);
    if (index > -1) {
      this._items.splice(index, 1);
      component.setContainer(null);
    }
  }

  getComponents() {
    return this._items;
  }

  getComponent(componentId) {
    return this._items.find(item => item.getId() === componentId);
  }

  replaceComponent(oldComponent, newComponent) {
    const index = this._items.indexOf(oldComponent);
    if (index !== -1) {
      this._items[index] = newComponent;
    }
  }

  hasComponent(component) {
    return this._items.indexOf(component) !== -1;
  }

  getHstRepresentation() {
    return {
      id: this.getId(),
      lastModifiedTimestamp: this.getLastModified(),
      children: this._items.map(item => item.getId()),
    };
  }

  getDropGroups() {
    const groupName = this.isXPageComponent() ? 'xpages' : 'default';
    return this.isShared() ? [`${groupName}-shared`] : [groupName];
  }
}
