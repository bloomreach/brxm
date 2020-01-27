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

/// <reference path="./cms.d.ts" />
import { Typed } from 'emittery';
import { Events } from './events';

const GLOBAL_WINDOW = typeof window === 'undefined' ? undefined : window;

export interface CmsOptions {
  /**
   * The window reference for the CMS integration.
   * By default the global window object will be used.
   */
  window?: Window;
}

/**
 * CMS integration layer.
 */
export interface Cms {
  /**
   * Initializes integration with the CMS.
   * @param options The CMS integration options.
   */
  initialize(options: CmsOptions): void;
}

export class Cms implements Cms {
  private api?: CmsApi;
  private postponed: Function[] = [];

  constructor(protected eventBus: Typed<Events>) {}

  private async flush() {
    this.postponed
      .splice(0)
      .forEach(task => task());
  }

  private postpone<T extends (...args: any[]) => any>(task: T) {
    return (...args: Parameters<T>) => {
      if (this.api) {
        return task.apply(this, args);
      }

      this.postponed.push(task.bind(this, ...args));
    };
  }

  initialize({ window = GLOBAL_WINDOW }) {
    if (this.api || !window || window.SPA) {
      return;
    }

    this.eventBus.on('page.ready', this.postpone(this.sync));

    window.SPA = {
      init: this.onInit.bind(this),
      renderComponent: this.onRenderComponent.bind(this),
    };
  }

  protected onInit(api: CmsApi) {
    this.api = api;
    this.flush();
  }

  protected onRenderComponent(id: string, properties: object) {
    this.eventBus.emit('cms.update', { id, properties });
  }

  protected sync() {
    this.api!.sync();
  }
}
