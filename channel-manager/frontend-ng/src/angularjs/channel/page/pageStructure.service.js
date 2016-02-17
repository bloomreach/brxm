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

export class PageStructureService {

  constructor($log, HST_CONSTANT) {
    'ngInject';

    // Injected
    this.$log = $log;
    this.HST = HST_CONSTANT;

    this.clearParsedElements();
  }

  clearParsedElements() {
    this.containers = [];
  }

  registerParsedElement(commentDomElement, metaData) {
    switch (metaData[this.HST.TYPE]) {
      case this.HST.TYPE_CONTAINER:
        this.containers.push(new ContainerElement($(commentDomElement).next()));
        break;

      case this.HST.TYPE_COMPONENT:
        if (this.containers.length === 0) {
          this.$log.warn('Unable to register component outside of a container context.');
          return;
        }

        const container = this.containers[this.containers.length - 1];
        container.addComponent(new ComponentElement($(commentDomElement).parent(), container));
        break;

      default:
        break;
    }
  }

  printParsedElements() {
    this.containers.forEach((container, index) => {
      this.$log.debug(`Container ${index}`, container);
      container.items.forEach((component, itemIndex) => {
        this.$log.debug(`  Component ${itemIndex}`, component);
      });
    });
  }
}

class PageStructureElement {
  constructor(type, jQueryElement) {
    this.type = type;
    this.jQueryElements = {};

    this.setJQueryElement('iframe', jQueryElement);
  }

  setJQueryElement(elementType, element) {
    this.jQueryElements[elementType] = element;
  }

  getJQueryElement(elementType) {
    return this.jQueryElements[elementType];
  }
}

class ContainerElement extends PageStructureElement {
  constructor(jQueryElement) {
    super('container', jQueryElement);

    this.items = [];
  }

  addComponent(component) {
    this.items.push(component);
  }

  getComponents() {
    return this.items;
  }
}

class ComponentElement extends PageStructureElement {
  constructor(jQueryElement, container) {
    super('component', jQueryElement);

    this.container = container;
  }
}
