/*
 * Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
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
import {
  ChannelScope,
  ChannelScopeEvents,
  DocumentProperties,
  DocumentScope,
  Emitter,
  EventHandler,
  FieldScope,
  PageProperties,
  PageScope,
  PageScopeEvents,
  UiProperties,
  UiScope,
} from './api';
import { Parent, ParentConnection, ParentMethod } from './parent';

const SCOPE_CHANNEL = 'channel';
const SCOPE_PAGE = `${SCOPE_CHANNEL}.page`;

// Symbols are unique and not listed in object keys so we can hide private methods.
const PARENT = Symbol('parent');
const EVENT_EMITTER = Symbol('eventEmitter');
const EVENT_SCOPE = Symbol('eventScope');

abstract class Scope<T extends Parent = Parent> {
  protected [PARENT]: ParentConnection<T>;

  protected constructor(parent: ParentConnection<T>) {
    this[PARENT] = parent;
  }
}

abstract class ScopeEmitter<Events, T extends Parent> extends Scope<T> implements Emitter<Events> {
  private [EVENT_EMITTER]: Emittery;
  private [EVENT_SCOPE]: string;

  constructor(parent: ParentConnection, eventEmitter: Emittery, eventScope: string) {
    super(parent);

    this[EVENT_EMITTER] = eventEmitter;
    this[EVENT_SCOPE] = eventScope;
  }

  on(eventName: keyof Events, callback: EventHandler<Events>) {
    return this[EVENT_EMITTER].on(`${this[EVENT_SCOPE]}.${eventName}`, callback);
  }
}

interface UiParent extends Parent {
  getProperties: ParentMethod<UiProperties>;
}

interface ChannelParent extends UiParent {
  getPage: ParentMethod<PageProperties>;
  refreshChannel: ParentMethod;
  refreshPage: ParentMethod;
}

interface DocumentParent extends UiParent {
  getDocument: ParentMethod<DocumentProperties>;
  getFieldValue: ParentMethod<string>;
  getFieldCompareValue: ParentMethod<string>;
  setFieldValue: ParentMethod<void, [string]>;
  setFieldHeight: ParentMethod<void, [number]>;
}

class Page extends ScopeEmitter<PageScopeEvents, ChannelParent> implements PageScope {
  get() {
    return this[PARENT].call('getPage');
  }

  refresh() {
    return this[PARENT].call('refreshPage');
  }
}

class Channel extends ScopeEmitter<ChannelScopeEvents, ChannelParent> implements ChannelScope {
  page: Page;

  constructor(parent: ParentConnection, eventEmitter: Emittery, eventScope: string) {
    super(parent, eventEmitter, eventScope);
    this.page = new Page(parent, eventEmitter, SCOPE_PAGE);
  }

  refresh() {
    return this[PARENT].call('refreshChannel');
  }
}

class Document extends Scope<DocumentParent> implements DocumentScope {
  get (): Promise<DocumentProperties> {
    return this[PARENT].call('getDocument');
  }
  field = new Field(this[PARENT]);
}

class Field extends Scope<DocumentParent> implements FieldScope {
  getValue() {
    return this[PARENT].call('getFieldValue');
  }

  getCompareValue (): Promise<string> {
    return this[PARENT].call('getFieldCompareValue');
  }

  setValue(value: string) {
    return this[PARENT].call('setFieldValue', value);
  }

  setHeight(pixels: number) {
    return this[PARENT].call('setFieldHeight', pixels);
  }
}

export class Ui extends Scope implements UiScope {
  baseUrl: string;
  channel: ChannelScope;
  document: DocumentScope;
  extension: {
    config: string,
  };
  locale: string;
  timeZone: string;
  user: {
    id: string,
    firstName: string,
    lastName: string,
    displayName: string,
  };
  version: string;

  constructor(parent: ParentConnection, eventEmitter: Emittery) {
    super(parent);
    this.channel = new Channel(parent, eventEmitter, SCOPE_CHANNEL);
    this.document = new Document(parent);
  }

  init(): Promise<Ui> {
    return this[PARENT].call('getProperties')
      .then(properties => Object.assign(this, properties));
  }
}
