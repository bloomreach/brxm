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

import { ContainerElement } from './element/containerElement';
import { ComponentElement } from './element/componentElement';

export class PageStructureService {

  constructor($q, $log, HstConstants, hstCommentsProcessorService, HstService, ChannelService, CmsService, PageMetaDataService) {
    'ngInject';

    // Injected
    this.$q = $q;
    this.$log = $log;
    this.HST = HstConstants;
    this.HstService = HstService;
    this.ChannelService = ChannelService;
    this.CmsService = CmsService;
    this.hstCommentsProcessorService = hstCommentsProcessorService;
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
        this.containers.push(new ContainerElement(commentDomElement, metaData));
        break;

      case this.HST.TYPE_COMPONENT:
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

      case this.HST.TYPE_PAGE:
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

      default:
        break;
    }
  }

  getComponent(componentId) {
    let component = null;
    this.containers.some((container) => {
      component = container.getComponent(componentId);
      return component;
    });
    return component;
  }

  /**
   * Remove the component identified by given Id
   * @param componentId
   * @returns {*} a promise with removed successfully component
   */
  removeComponent(componentId) {
    let component = null;
    let foundContainer = this.containers.find((container) => {
      component = container.removeComponent(componentId);
      return component;
    });

    if (!foundContainer) {
      return this.$q.reject();
    }
    // request back-end to remove component
    return this._removeHstComponent(foundContainer.getId(), componentId)
      .then(() => {
        component.removeFromDOM();
        return component;
      });
  }

  _removeHstComponent(containerId, componentId) {
    return this.HstService.doGet(containerId, 'delete', componentId);
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
