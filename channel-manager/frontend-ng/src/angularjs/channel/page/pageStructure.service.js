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

/* eslint-disable prefer-arrow-callback */

import { ContainerElement } from './element/containerElement';
import { ComponentElement } from './element/componentElement';

export class PageStructureService {

  constructor($log, $q, HstConstants, hstCommentsProcessorService, RenderingService, OverlaySyncService,
              ChannelService, CmsService, PageMetaDataService, HstService, MaskService) {
    'ngInject';

    // Injected
    this.$log = $log;
    this.$q = $q;
    this.HST = HstConstants;
    this.HstService = HstService;
    this.ChannelService = ChannelService;
    this.CmsService = CmsService;
    this.hstCommentsProcessorService = hstCommentsProcessorService;
    this.RenderingService = RenderingService;
    this.OverlaySyncService = OverlaySyncService;
    this.PageMetaDataService = PageMetaDataService;
    this.MaskService = MaskService;

    this.clearParsedElements();
  }

  clearParsedElements() {
    this.containers = [];
    this.PageMetaDataService.clear();
  }

  registerParsedElement(commentDomElement, metaData) {
    switch (metaData[this.HST.TYPE]) {
      case this.HST.TYPE_CONTAINER:
        this.containers.push(new ContainerElement(commentDomElement, metaData, this.hstCommentsProcessorService));
        break;

      case this.HST.TYPE_COMPONENT: {
        if (this.containers.length === 0) {
          this.$log.warn('Unable to register component outside of a container context.');
          return;
        }

        const container = this.containers[this.containers.length - 1];
        try {
          container.addComponent(new ComponentElement(commentDomElement, metaData,
            container, this.hstCommentsProcessorService));
        } catch (exception) {
          this.$log.debug(exception, metaData);
        }
        break;
      }

      case this.HST.TYPE_PAGE: {
        delete metaData[this.HST.TYPE];
        this.PageMetaDataService.add(metaData);
        break;
      }

      default:
        break;
    }
  }

  getComponentById(id) {
    let component = null;
    this.containers.some((container) => {
      component = container.getComponent(id);
      return component;
    });
    return component;
  }

  hasContainer(container) {
    return this.containers.indexOf(container) !== -1;
  }

  /**
   * Remove the component identified by given Id
   * @param componentId
   * @return a promise with the object { oldContainer, newContainer }
   */
  removeComponentById(componentId) {
    const component = this.getComponentById(componentId);

    if (component) {
      const oldContainer = component.getContainer();
      return this.HstService.removeHstComponent(oldContainer.getId(), componentId)
        .then(() => {
          this.ChannelService.recordOwnChange();
          return this.renderContainer(oldContainer).then((newContainer) => { // eslint-disable-line arrow-body-style
            return { oldContainer, newContainer };
          });
        });
      // TODO handle error
    }
    this.$log.debug(`Could not remove component with ID '${componentId}' because it does not exist in the page structure.`);
    return this.$q.reject();
  }

  getContainerByIframeElement(containerIFrameElement) {
    return this.containers.find((container) => container.getBoxElement().is(containerIFrameElement));
  }

  showComponentProperties(componentElement) {
    if (!componentElement) {
      this.$log.warn('Problem opening the component properties dialog: no component provided.');
      return;
    }

    this.CmsService.publish('show-component-properties', {
      component: {
        id: componentElement.getId(),
        label: componentElement.getLabel(),
        lastModified: componentElement.getLastModified(),
      },
      container: {
        isDisabled: componentElement.container.isDisabled(),
        isInherited: componentElement.container.isInherited(),
      },
      page: this.PageMetaDataService.get(),
    });
    this.MaskService.add();
  }

  printParsedElements() {
    this.containers.forEach((container, index) => {
      this.$log.debug(`Container ${index}`, container);
      container.items.forEach((component, itemIndex) => {
        this.$log.debug(`  Component ${itemIndex}`, component);
      });
    });
  }

  renderComponent(componentId, propertiesMap) {
    const component = this.getComponentById(componentId);
    if (component) {
      this.RenderingService.fetchComponentMarkup(component, propertiesMap).then((response) => {
        this._updateComponent(component, response.data);
      });
    } else {
      this.$log.warn(`Cannot render unknown component '${componentId}'`);
    }
  }

  /**
   * Update the component with the new markup
   */
  _updateComponent(component, newMarkup) {
    const jQueryNodeCollection = component.replaceDOM(newMarkup);
    this._replaceComponent(component, this._createComponent(jQueryNodeCollection, component.getContainer()));
    this.OverlaySyncService.syncIframe();
  }

  /**
   * Lets the back-end re-render a container.
   * @param container
   * @returns {*} a promise with the new container object
   * @private
   */
  renderContainer(container) {
    return this.RenderingService.fetchContainerMarkup(container)
      .then((markup) => this._updateContainer(container, markup));
  }

  _updateContainer(container, newMarkup) {
    // consider following three actions to be an atomic operation
    return this._replaceContainer(container, this._createContainer(container.replaceDOM(newMarkup)));
  }

  _replaceComponent(oldComponent, newComponent) {
    const container = oldComponent.getContainer();
    if (this.hasContainer(container) && container.hasComponent(oldComponent)) {
      container.replaceComponent(oldComponent, newComponent);
    } else {
      this.$log.warn('Cannot find component', oldComponent);
    }
  }

  addComponentToContainer(catalogComponent, container) {
    return this.HstService.addHstComponent(catalogComponent, container.getId())
      .then((newComponentJson) => {
        this.ChannelService.recordOwnChange();
        return this.renderContainer(container).then(() => this.getComponentById(newComponentJson.id));
      });
    // TODO: handle error
  }

  getContainerByOverlayElement(overlayElement) {
    return this.containers.find((container) => container.getOverlayElement()[0] === overlayElement);
  }

  _replaceContainer(oldContainer, newContainer) {
    const index = this.containers.indexOf(oldContainer);
    if (index === -1) {
      this.$log.warn('Cannot find container', oldContainer);
      return null;
    }
    if (newContainer) {
      this.containers[index] = newContainer;
    } else {
      this.containers.splice(index, 1);
    }
    return newContainer;
  }

  /**
   * Create a new container with meta-data from the given markup value
   * @param markup
   * @returns {*}
   * @private
   */
  _createContainer(jQueryNodeCollection) {
    let container = null;

    this.hstCommentsProcessorService.processFragment(jQueryNodeCollection, (commentDomElement, metaData) => {
      try {
        switch (metaData[this.HST.TYPE]) {
          case this.HST.TYPE_CONTAINER:
            if (!container) {
              container = new ContainerElement(commentDomElement, metaData, this.hstCommentsProcessorService);
            } else {
              this.$log.warn('More than one container in the DOM Element!');
              return;
            }
            break;

          case this.HST.TYPE_COMPONENT:
            if (!container) {
              this.$log.warn('Unable to register component outside of a container context.');
              return;
            }

            try {
              container.addComponent(new ComponentElement(commentDomElement, metaData,
                container, this.hstCommentsProcessorService));
            } catch (exception) {
              this.$log.debug(exception, metaData);
            }
            break;

          default:
            break;
        }
      } catch (exception) {
        this.$log.debug(exception, metaData);
      }
    });

    if (!container) {
      this.$log.error('Failed to create a new container');
    }

    return container;
  }

  /**
   * Create a new component with meta-data from the given markup value
   */
  _createComponent(jQueryNodeCollection, container) {
    let component = null;

    this.hstCommentsProcessorService.processFragment(jQueryNodeCollection, (commentDomElement, metaData) => {
      switch (metaData[this.HST.TYPE]) {
        case this.HST.TYPE_COMPONENT:
          try {
            component = new ComponentElement(commentDomElement, metaData, container, this.hstCommentsProcessorService);
          } catch (exception) {
            this.$log.debug(exception, metaData);
          }
          break;

        default:
          break;
      }
    });

    if (!component) {
      this.$log.error('Failed to create a new component');
    }

    return component;
  }
}
