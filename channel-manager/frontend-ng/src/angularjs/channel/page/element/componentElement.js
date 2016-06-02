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

/* eslint-disable prefer-const */

import { PageStructureElement } from './pageStructureElement';

export class ComponentElement extends PageStructureElement {
  constructor(startCommentDomElement, metaData, container, commentProcessor) {
    let [boxDomElement, endCommentDomElement] = commentProcessor.locateComponent(metaData.uuid, startCommentDomElement);

    if (!PageStructureElement.isXTypeNoMarkup(container.metaData)) {
      boxDomElement = startCommentDomElement.parentNode;
    }

    super('component', metaData, startCommentDomElement, endCommentDomElement, boxDomElement);

    this.container = container;
  }

  getContainer() {
    return this.container;
  }

  setContainer(container) {
    this.container = container;
  }
}
