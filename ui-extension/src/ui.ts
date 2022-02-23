/*
 * Copyright 2018-2020 Hippo B.V. (http://www.onehippo.com)
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
import Emittery from 'emittery'; // tslint:disable-line:import-name
import {
  ChannelScope,
  ChannelScopeEvents,
  DialogProperties,
  DialogScope,
  DocumentProperties,
  DocumentScope,
  DynamicProperties,
  Emitter,
  EventHandler,
  FieldScope,
  PageProperties,
  PageScope,
  PageScopeEvents,
  UiProperties,
  UiScope,
  UiStyling,
} from './api';
import { Parent, ParentConnection, ParentMethod } from './parent';

type Height = 'initial' | number;

const SCOPE_CHANNEL = 'channel';
const SCOPE_PAGE = `${SCOPE_CHANNEL}.page`;
const SCOPE_DOCUMENT = 'document';
const SCOPE_FIELD = `${SCOPE_DOCUMENT}.field`;

// Symbols are unique and not listed in object keys so we can hide private methods.
const PARENT = Symbol('parent');
const EVENT_EMITTER = Symbol('eventEmitter');
const EVENT_SCOPE = Symbol('eventScope');
const FIELD_DOM_OBSERVER = Symbol('fieldDomObserver');
const FIELD_OVERFLOW_STYLE = Symbol('fieldOverflowStyle');
const FIELD_HEIGHT = Symbol('fieldHeight');
const FIELD_HEIGHT_MODE = Symbol('fieldHeightMode');
const FIELD_HEIGHT_LISTENER = Symbol('fieldHeightListener');
const KEY_ESCAPE = 27;

function defineLazyGetter<T extends object, K extends keyof T>(object: T, property: K, getter: Callable<T[K]>) {
  let value: T[K];

  Object.defineProperty(object, property, {
    get: () => {
      if (value === undefined) {
        value = getter();
      }

      return value;
    },
    configurable: false,
    enumerable: true,
  });
}

abstract class Scope<T extends Parent = Parent> {
  protected [PARENT]: ParentConnection<T>;

  constructor(parent: ParentConnection<T>) {
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
  emitEvent: ParentMethod<void, [string]>;
}

interface ChannelParent extends UiParent {
  getPage: ParentMethod<PageProperties>;
  refreshChannel: ParentMethod;
  refreshPage: ParentMethod;
  getChannel: ParentMethod<DynamicProperties>;
}

interface DocumentParent extends UiParent {
  getDocument: ParentMethod<DocumentProperties>;
  getFieldValue: ParentMethod<any, string[]>;
  getFieldCompareValue: ParentMethod<any, string[]>;
  navigateDocument: ParentMethod<void, [string]>;
  openDocument: ParentMethod<void, [string]>;
  setFieldValue: ParentMethod<void, [string]>;
  setFieldHeight: ParentMethod<void, [Height]>;
}

interface DialogParent extends UiParent {
  cancelDialog: ParentMethod<void>;
  closeDialog: ParentMethod<void, [any]>;
  getDialogOptions: ParentMethod<DialogProperties>;
  openDialog: ParentMethod<void, [DialogProperties]>;
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

    defineLazyGetter(this, 'page', () => new Page(parent, eventEmitter, SCOPE_PAGE));
  }

  refresh() {
    return this[PARENT].call('refreshChannel');
  }

  get() {
    return this[PARENT].call('getChannel');
  }
}

class Document extends Scope<DocumentParent> implements DocumentScope {
  field: Field;

  constructor(parent: ParentConnection<DocumentParent>) {
    super(parent);

    defineLazyGetter(this, 'field', () => new Field(this[PARENT]));
  }

  get(): Promise<DocumentProperties> {
    return this[PARENT].call('getDocument');
  }

  navigate(path: string): Promise<void> {
    return this[PARENT].call('navigateDocument', path);
  }

  open(id: string): Promise<void> {
    return this[PARENT].call('openDocument', id);
  }
}

function enableAutoHeight(this: Field) {
  if (this[FIELD_HEIGHT_MODE] === 'auto') {
    return;
  }

  document.body.addEventListener('load', this[FIELD_HEIGHT_LISTENER], true);
  this[FIELD_DOM_OBSERVER].observe(document.body, {
    attributes: true,
    characterData: true,
    childList: true,
    subtree: true,
  });

  this[FIELD_HEIGHT_MODE] = 'auto';
  this[FIELD_OVERFLOW_STYLE] = document.body.style.overflowY;
  document.body.style.overflowY = 'hidden';
}

function disableAutoHeight(this: Field) {
  if (this[FIELD_HEIGHT_MODE] !== 'auto') {
    return;
  }

  document.body.removeEventListener('load', this[FIELD_HEIGHT_LISTENER], true);
  this[FIELD_DOM_OBSERVER].disconnect();

  this[FIELD_HEIGHT_MODE] = 'manual';
  document.body.style.overflowY = this[FIELD_OVERFLOW_STYLE];
}

function updateHeight(this: Field, height: Height) {
  // cache the field height to avoid unnecessary method calls
  if (this[FIELD_HEIGHT] === height) {
    return;
  }

  this[FIELD_HEIGHT] = height;

  return this[PARENT].call('setFieldHeight', height);
}

class Field extends Scope<DocumentParent> implements FieldScope {
  private [FIELD_HEIGHT]: Height;
  private [FIELD_HEIGHT_MODE]: 'auto' | 'manual' = 'manual';
  private [FIELD_HEIGHT_LISTENER] = () => updateHeight.call(this, document.body.scrollHeight);
  private [FIELD_DOM_OBSERVER] = new MutationObserver(this[FIELD_HEIGHT_LISTENER]);

  constructor(parent: ParentConnection<DocumentParent>) {
    super(parent);

    window.addEventListener('focus', () => parent.call('emitEvent', `${SCOPE_FIELD}.focus`));
    window.addEventListener('blur', () => parent.call('emitEvent', `${SCOPE_FIELD}.blur`));
  }

  getValue(...path: string[]) {
    return this[PARENT].call('getFieldValue', ...path);
  }

  getCompareValue(...path: string[]) {
    return this[PARENT].call('getFieldCompareValue', ...path);
  }

  setValue(value: string) {
    return this[PARENT].call('setFieldValue', value);
  }

  setHeight(height: 'auto' | Height): Promise<void> {
    if (height === 'auto') {
      return enableAutoHeight.call(this) || Promise.resolve();
    }

    disableAutoHeight.call(this);

    return updateHeight.call(this, height) || Promise.resolve();
  }
}

class Dialog extends Scope<DialogParent> implements DialogScope {
  constructor(parent: ParentConnection<DialogParent>) {
    super(parent);

    window.addEventListener('keydown', ({ which }) => which === KEY_ESCAPE && this.cancel());
  }

  cancel() {
    return this[PARENT].call('cancelDialog');
  }

  close(value: any) {
    return this[PARENT].call('closeDialog', value);
  }

  open(options: DialogProperties) {
    return this[PARENT].call('openDialog', options);
  }

  options() {
    return this[PARENT].call('getDialogOptions');
  }
}

export class Ui extends Scope implements UiScope {
  baseUrl: string;
  channel: ChannelScope;
  dialog: DialogScope;
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
  styling: UiStyling;
  version: string;

  constructor(parent: ParentConnection, eventEmitter: Emittery) {
    super(parent);

    defineLazyGetter(this, 'channel', () => new Channel(parent, eventEmitter, SCOPE_CHANNEL));
    defineLazyGetter(this, 'dialog', () => new Dialog(parent));
    defineLazyGetter(this, 'document', () => new Document(parent));
  }

  init(): Promise<Ui> {
    return this[PARENT].call('getProperties')
      .then(properties => Object.assign(this, properties));
  }
}
