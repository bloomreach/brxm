/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Typed } from 'emittery';
import { Events } from './events';

/**
 * Channel Manager SPA window.
 */
export interface Window {
  SPA?: SpaApi;
}

interface SpaApi {
  init(api: CmsApi): void;
  renderComponent(id: string, properties: object): void;
}

interface CmsApi {
  sync(): void;
}

/**
 * CMS integration layer.
 */
export interface Cms {
  /**
   * Initializes integration with the CMS.
   */
  initialize(): void;
}

export class Cms implements Cms {
  constructor(protected eventBus: Typed<Events>, protected window?: Window) {}

  initialize() {
    if (!this.window || this.window.SPA) {
      return;
    }

    this.window.SPA = {
      init: this.onInit.bind(this),
      renderComponent: this.onRenderComponent.bind(this),
    };
  }

  protected onInit(api: CmsApi) {
    this.eventBus.on('page.ready', api.sync.bind(api));
  }

  protected onRenderComponent(id: string, properties: object) {
    this.eventBus.emit('cms.update', { id, properties });
  }
}
