/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

/**
 * @module ui-extension
 * Main entry point of the ui-extension library. Implements the public API defined in the
 * api module and communicates with the parent frame using the parent module.
 *
 * @see module:api
 * @see module:parent
 */

/**
 * Disable TSLint for imports that start with an uppercase letter
 * @see https://github.com/Microsoft/tslint-microsoft-contrib/issues/387
 */
import Emittery = require('emittery'); // tslint:disable-line:import-name
import { ChannelScope, Emitter, EventCallback, PageScope, PageScopeEvents, UiScope } from './api';
import { connect, ParentConnection } from './parent';

abstract class Scope {
  protected constructor(protected _parent: ParentConnection) {
  }
}

abstract class ScopeEmitter<Events> extends Scope implements Emitter<Events> {
  constructor(parent: ParentConnection, private _eventEmitter: Emittery, private _eventScope: string) {
    super(parent);
  }

  on(eventName: keyof Events, listener: EventCallback<Events>) {
    const scopedEventName = `${this._eventScope}.${eventName}`;
    return this._eventEmitter.on(scopedEventName, listener);
  }
}

class Page extends ScopeEmitter<PageScopeEvents> implements PageScope {
  get() {
    return this._parent.call('getPage');
  }

  refresh() {
    return this._parent.call('refreshPage');
  }
}

class Channel extends Scope implements ChannelScope {
  page: Page;

  constructor(parent: ParentConnection, eventEmitter: Emittery) {
    super(parent);
    this.page = new Page(parent, eventEmitter, 'channel.page');
  }

  refresh() {
    return this._parent.call('refreshChannel');
  }
}

class Ui extends Scope implements UiScope {
  baseUrl: string;
  channel: ChannelScope;
  extension: {
    config: string,
  };
  locale: string;
  timeZone: string;
  user: string;
  version: string;

  constructor(parent: ParentConnection, eventEmitter: Emittery) {
    super(parent);
    this.channel = new Channel(parent, eventEmitter);
  }

  init(): Promise<Ui> {
    return this._parent.call('getProperties')
      .then(properties => Object.assign(this, properties));
  }
}

export default class UiExtension {
  static register(): Promise<UiScope> {
    const parentOrigin = new URLSearchParams(window.location.search).get('br.parentOrigin');
    const eventEmitter = new Emittery();

    return connect(parentOrigin, eventEmitter)
      .then(parentConnection => new Ui(parentConnection, eventEmitter).init());
  }
}

// enable UiExtension.register() in ui-extension.min.js
export const register = UiExtension.register;
