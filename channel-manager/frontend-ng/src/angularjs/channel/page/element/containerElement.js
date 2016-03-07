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
import { PageStructureElement } from './pageStructureElement';

export class ContainerElement extends PageStructureElement {
  constructor(commentDomElement, metaData) {
    let jQueryElement;

    if (PageStructureElement.isTransparentXType(metaData)) {
      jQueryElement = $(commentDomElement).parent();
    } else {
      jQueryElement = $(commentDomElement).next();
    }

    super('container', jQueryElement, metaData);

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

  addComponent(component) {
    this.items.push(component);
  }

  getComponents() {
    return this.items;
  }

  getComponent(componentId) {
    const component = this.items.find((item) => {
      return item.getId() === componentId;
    });
    return angular.isDefined(component) ? component : null;
  }

}
