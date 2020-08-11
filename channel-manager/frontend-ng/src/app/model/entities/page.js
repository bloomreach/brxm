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

export class Page {
  constructor(meta, containers, links) {
    this._meta = meta;
    this._containers = containers;
    this._links = links;
  }

  getMeta() {
    return this._meta;
  }

  getLinks() {
    return this._links;
  }

  getComponentById(id) {
    // eslint-disable-next-line no-plusplus
    for (let i = 0; i < this._containers.length; i++) {
      const component = this._containers[i].getComponent(id);

      if (component) {
        return component;
      }
    }

    return null;
  }

  hasContainer(container) {
    return this._containers.includes(container);
  }

  getContainers() {
    return this._containers;
  }

  getContainerById(id) {
    return this._containers.find(item => item.getId() === id);
  }

  replaceContainer(oldContainer, newContainer) {
    const index = this._containers.indexOf(oldContainer);
    if (index === -1) {
      throw new Error('Cannot find container.');
    }

    if (!newContainer) {
      this._containers.splice(index, 1);

      return null;
    }

    this._containers[index] = newContainer;

    return newContainer;
  }
}
