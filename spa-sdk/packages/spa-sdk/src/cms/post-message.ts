/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

import { injectable, inject, optional } from 'inversify';
import { isMatched } from '../url';
import { Events, Message, Procedures, Rpc } from './rpc';
import { Logger } from '../logger';

export const PostMessageService = Symbol.for('PostMessageService');

const GLOBAL_WINDOW = typeof window === 'undefined' ? undefined : window;

export interface PostMessageOptions {
  /**
   * The brXM origin to verify an integration with the Experience Manager.
   * This option should be used when the brXM is accessible from a host other than the Page Model API.
   * By default, the origin from the `apiBaseUrl` or `endpoint` parameters is used.
   */
  origin?: string;

  /**
   * The window reference for the CMS integration.
   * By default the global window object will be used.
   */
  window?: Window;
}

@injectable()
export class PostMessage<
  TRemoteProcedures extends Procedures = Procedures,
  TRemoteEvents extends Events = Events,
  TProcedures extends Procedures = Procedures,
  TEvents extends Events = Events,
> extends Rpc<TRemoteProcedures, TRemoteEvents, TProcedures, TEvents> {
  private origin?: string;

  private window?: Window;

  constructor(@inject(Logger) @optional() private logger?: Logger) {
    super();
    this.onMessage = this.onMessage.bind(this);
  }

  initialize({ origin, window = GLOBAL_WINDOW }: PostMessageOptions) {
    this.window?.removeEventListener('message', this.onMessage, false);
    this.origin = origin;
    this.window = window;
    this.window?.addEventListener('message', this.onMessage, false);
  }

  protected send(message: Message) {
    if (!this.origin) {
      return;
    }

    this.logger?.debug('[OUTGOING]', `[${this.origin}]`, message);
    this.window?.parent?.postMessage(message, this.origin);
  }

  private onMessage(event: MessageEvent) {
    if (!event.data || !isMatched(event.origin, this.origin === '*' ? '' : this.origin)) {
      return;
    }

    if (event.data?.type) {
      this.logger?.debug('[INCOMING]', `[${event.origin}]`, event.data);
    }

    this.process(event.data);
  }
}
