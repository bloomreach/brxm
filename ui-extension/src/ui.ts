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
import Emittery from 'emittery'; // tslint:disable-line:import-name
import {
  ChannelScope,
  ChannelScopeEvents,
  DialogProperties,
  DialogScope,
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
const FIELD_DOM_OBSERVER = Symbol('fieldDomObserver');
const FIELD_OVERFLOW_STYLE = Symbol('fieldOverflowStyle');
const FIELD_HEIGHT = Symbol('fieldHeight');

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
    this.page = new Page(parent, eventEmitter, SCOPE_PAGE);
  }

  refresh() {
    return this[PARENT].call('refreshChannel');
  }
}

class Document extends Scope<DocumentParent> implements DocumentScope {
  get(): Promise<DocumentProperties> {
    return this[PARENT].call('getDocument');
  }
  field = new Field(this[PARENT]);
}

class Field extends Scope<DocumentParent> implements FieldScope {
  constructor(parent: ParentConnection) {
    super(parent);
    this[FIELD_DOM_OBSERVER] = new MutationObserver(this.updateHeight.bind(this));
    this[FIELD_HEIGHT] = null;
  }

  getValue() {
    return this[PARENT].call('getFieldValue');
  }

  getCompareValue(): Promise<string> {
    return this[PARENT].call('getFieldCompareValue');
  }

  setValue(value: string) {
    return this[PARENT].call('setFieldValue', value);
  }

  setHeight(pixels: number) {
    if (this[FIELD_HEIGHT] !== pixels) {
      this[FIELD_HEIGHT] = null;
    }
    return this[PARENT].call('setFieldHeight', pixels);
  }

  updateHeight() {
    const height = document.body.scrollHeight;

    // cache the field height to avoid unnecessary method calls
    if (this[FIELD_HEIGHT] !== height) {
      this[FIELD_HEIGHT] = height;
      return this.setHeight(height);
    }
  }

  autoUpdateHeight() {
    this[FIELD_DOM_OBSERVER].observe(document.body, {
      attributes: true,
      characterData: true,
      childList: true,
      subtree: true,
    });

    this[FIELD_OVERFLOW_STYLE] = document.body.style.overflowY;
    document.body.style.overflowY = 'hidden';

    return () => {
      this[FIELD_DOM_OBSERVER].disconnect();
      document.body.style.overflowY = this[FIELD_OVERFLOW_STYLE];
    };
  }
}

class Dialog extends Scope<DialogParent> implements DialogScope {
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
  version: string;

  constructor(parent: ParentConnection, eventEmitter: Emittery) {
    super(parent);
    this.channel = new Channel(parent, eventEmitter, SCOPE_CHANNEL);
    this.dialog = new Dialog(parent);
    this.document = new Document(parent);
  }

  init(): Promise<Ui> {
    return this[PARENT].call('getProperties')
      .then(properties => Object.assign(this, properties));
  }
}
