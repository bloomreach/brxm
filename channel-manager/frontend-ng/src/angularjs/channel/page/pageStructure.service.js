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

  constructor($log, $q, HstConstants, hstCommentsProcessorService, RenderingService, OverlaySyncService, ChannelService, CmsService, PageMetaDataService, HstService) {
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
    this.pageMetaData = PageMetaDataService;

    this.clearParsedElements();
  }

  clearParsedElements() {
    this.containers = [];
    this.pageMetaData.clear();
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
        const registeredMetaData = angular.copy(metaData);
        delete registeredMetaData[this.HST.TYPE];
        this.pageMetaData.add(registeredMetaData);

        if (metaData.hasOwnProperty(this.HST.CHANNEL_ID)) {
          const channelId = metaData[this.HST.CHANNEL_ID];
          if (channelId !== this.ChannelService.getId()) {
            this.ChannelService.switchToChannel(channelId);
          }
        }
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
   */
  removeComponentById(componentId) {
    const component = this.getComponentById(componentId);

    if (component) {
      const container = component.getContainer();
      this.HstService.removeHstComponent(container.getId(), componentId)
        .then(() => this._renderContainer(container));
      // TODO handle error
    } else {
      this.$log.debug(`Was asked to remove component with ID '${componentId}', but couldn't find it in the page structure.`);
    }
  }

  getContainerByIframeElement(containerIFrameElement) {
    return this.containers.find((container) => container.getJQueryElement('iframeBoxElement').is(containerIFrameElement));
  }

  showComponentProperties(componentElement) {
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
      page: this.pageMetaData.get(),
    });
    // TODO CHANNELMGR-494: activate the mask. When/where to deactivate it? An explicit signal from ExtJS would be nice.
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

  _renderContainer(container) {
    this.RenderingService.fetchContainerMarkup(container).then((response) => {
      this._updateContainer(container, response.data);
    });
  }

  _updateContainer(container, newMarkup) {
    const jQueryContainerElement = container.replaceDOM(newMarkup);
    this._replaceContainer(container, this._createContainer(jQueryContainerElement));
    this.OverlaySyncService.syncIframe(); // necessary? mutation observer should trigger this...
  }

  _replaceComponent(oldComponent, newComponent) {
    const container = oldComponent.getContainer();
    if (this.hasContainer(container) && container.hasComponent(oldComponent)) {
      container.replaceComponent(oldComponent, newComponent);
    } else {
      this.$log.warn('Cannot find component', oldComponent);
    }
  }

  addComponentToContainer(catalogComponent, overlayDomElementOfContainer) {
    const container = this.containers.find((c) => c.getJQueryElement('overlay')[0] === overlayDomElementOfContainer);

    if (container) {
      this.HstService.addHstComponent(catalogComponent, container.getId())
        .then(() => this._renderContainer(container));
      // TODO: handle error
    } else {
      console.log('container not found');
    }
  }

  _replaceContainer(oldContainer, newContainer) {
    const index = this.containers.indexOf(oldContainer);
    if (index === -1) {
      this.$log.warn('Cannot find container', oldContainer);
      return;
    }
    this.containers[index] = newContainer;
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
