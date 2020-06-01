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

import { isMatched } from '../url';
import { Events, Message, Procedures, Rpc } from './rpc';

const GLOBAL_WINDOW = typeof window === 'undefined' ? undefined : window;

export interface PostMessageOptions {
  /**
   * CMS origin to verify a message sender.
   */
  origin: string;

  /**
   * The window reference for the CMS integration.
   * By default the global window object will be used.
   */
  window?: Window;
}

export class PostMessage<
  TRemoteProcedures extends Procedures,
  TRemoteEvents extends Events,
  TProcedures extends Procedures,
  TEvents extends Events,
> extends Rpc<TRemoteProcedures, TRemoteEvents, TProcedures, TEvents> {
  private options?: PostMessageOptions;

  constructor() {
    super();

    this.onMessage = this.onMessage.bind(this);
  }

  private get window() {
    return this.options?.window || GLOBAL_WINDOW;
  }

  initialize(options: PostMessageOptions) {
    this.window?.removeEventListener('message', this.onMessage, false);
    this.options = options;
    this.window?.addEventListener('message', this.onMessage, false);
  }

  protected send(message: Message) {
    this.window?.parent?.postMessage(message, this.options!.origin);
  }

  private onMessage(event: MessageEvent) {
    if (!event.data || !this.options || !isMatched(event.origin, this.options.origin)) {
      return;
    }

    this.process(event.data);
  }
}
