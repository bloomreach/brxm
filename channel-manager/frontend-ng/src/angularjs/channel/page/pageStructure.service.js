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

  constructor($log, HstConstants, ChannelService) {
    'ngInject';

    // Injected
    this.$log = $log;
    this.HST = HstConstants;
    this.ChannelService = ChannelService;

    this.clearParsedElements();
  }

  clearParsedElements() {
    this.containers = [];
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
        container.addComponent(new ComponentElement(commentDomElement, metaData, container));
        break;

      case this.HST.TYPE_PAGE:
        const channelId = metaData[this.HST.CHANNEL_ID];
        if (channelId !== this.ChannelService.getId()) {
          this.ChannelService.switchToChannel(channelId);
        }
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
