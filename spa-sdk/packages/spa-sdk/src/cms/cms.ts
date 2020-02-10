/*
 * Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
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
import { Events, CmsUpdateEvent } from '../events';
import { RpcClient, RpcServer, Procedures } from './rpc';

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

interface CmsProcedures extends Procedures {
  sync(): void;
}

interface CmsEvents {
  update: CmsUpdateEvent;
}

interface SpaProcedures extends Procedures {
}

interface SpaEvents {
  ready: never;
}

export class CmsImpl implements Cms {
  private window?: Window;

  constructor(
    protected eventBus: Typed<Events>,
    protected rpcClient: RpcClient<CmsProcedures, CmsEvents>,
    protected rpcServer: RpcServer<SpaProcedures, SpaEvents>,
  ) {
    this.onPageReady = this.onPageReady.bind(this);
    this.onUpdate = this.onUpdate.bind(this);
    this.inject = this.inject.bind(this);
  }

  initialize({ window = GLOBAL_WINDOW }: CmsOptions) {
    if (this.window === window) {
      return;
    }

    this.window = window;
    this.notifyOnReady();
    this.eventBus.on('page.ready', this.onPageReady);
    this.rpcClient.on('update', this.onUpdate);
    this.rpcServer.register('inject', this.inject);
  }

  private notifyOnReady() {
    const notify = () => this.rpcServer.trigger('ready', undefined as never);
    const onStateChange = () => {
      if (this.window!.document!.readyState === 'loading') {
        return;
      }

      notify();
      this.window!.document!.removeEventListener('readystatechange', onStateChange);
    };

    if (this.window?.document?.readyState !== 'loading') {
      return notify();
    }

    this.window?.document?.addEventListener('readystatechange', onStateChange);
  }

  protected onPageReady() {
    this.rpcClient.call('sync');
  }

  protected onUpdate(event: CmsUpdateEvent) {
    this.eventBus.emit('cms.update', event);
  }

  protected inject(resource: string) {
    if (!this.window?.document) {
      return Promise.reject(new Error('SPA document is not ready.'));
    }

    return new Promise((resolve, reject) => {
      const script = this.window!.document.createElement('script');

      script.type = 'text/javascript';
      script.src = resource;
      script.addEventListener('load', () => resolve());
      script.addEventListener('error', () => reject(new Error(`Failed to load resource '${resource}'.`)));
      this.window!.document.body.appendChild(script);
    });
  }
}
