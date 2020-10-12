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

import { inject, injectable, optional } from 'inversify';
import { EventBus, EventBusService, CmsUpdateEvent } from './events';
import { RpcClient, RpcClientService, RpcServer, RpcServerService, Procedures } from './rpc';

export const CmsService = Symbol.for('CmsService');

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

@injectable()
export class CmsImpl implements Cms {
  private window?: Window;

  constructor(
    @inject(RpcClientService) protected rpcClient: RpcClient<CmsProcedures, CmsEvents>,
    @inject(RpcServerService) protected rpcServer: RpcServer<SpaProcedures, SpaEvents>,
    @inject(EventBusService) @optional() protected eventBus?: EventBus,
  ) {
    this.onStateChange = this.onStateChange.bind(this);
    this.eventBus?.on('page.ready', this.onPageReady.bind(this));
    this.rpcClient.on('update', this.onUpdate.bind(this));
    this.rpcServer.register('inject', this.inject.bind(this));
  }

  initialize({ window = GLOBAL_WINDOW }: CmsOptions) {
    if (this.window === window) {
      return;
    }

    this.window = window;

    if (this.window?.document?.readyState !== 'loading') {
      return this.onInitialize();
    }

    this.window?.document?.addEventListener('readystatechange', this.onStateChange);
  }

  private onInitialize() {
    this.rpcServer.trigger('ready', undefined as never);
  }

  private onStateChange() {
    if (this.window!.document!.readyState === 'loading') {
      return;
    }

    this.onInitialize();
    this.window!.document!.removeEventListener('readystatechange', this.onStateChange);
  }

  protected onPageReady() {
    this.rpcClient.call('sync');
  }

  protected onUpdate(event: CmsUpdateEvent) {
    this.eventBus?.emit('cms.update', event);
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
