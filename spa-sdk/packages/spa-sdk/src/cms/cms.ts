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
}

interface SpaProcedures extends Procedures {
}

interface SpaEvents {
}

export class CmsImpl implements Cms {
  constructor(
    protected eventBus: Typed<Events>,
    protected rpcClient: RpcClient<CmsProcedures, CmsEvents>,
    protected rpcServer: RpcServer<SpaProcedures, SpaEvents>,
  ) {
    this.onPageReady = this.onPageReady.bind(this);
  }

  initialize() {
    this.eventBus.on('page.ready', this.onPageReady);
  }

  protected onPageReady() {
    this.rpcClient.call('sync');
  }
}
