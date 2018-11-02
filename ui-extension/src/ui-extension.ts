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

// disable TSLint for imports that start with an uppercase letter (workaround for issue 387)
import Emittery = require('emittery');  // tslint:disable-line
import Penpal from 'penpal';            // tslint:disable-line

interface UiProperties {
  baseUrl: string;
  extension: {
    config: string,
  };
  locale: string;
  timeZone: string;
  user: string;
  version: string;
}

export interface PageProperties {
  channel: {
    id: string;
  };
  id: string;
  sitemapItem: {
    id: string;
  };
  url: string;
}

interface PageEvents extends Emittery.Events {
  load: PageProperties;
}

export interface Parent {
  getPage: () => Promise<PageProperties>;
  getProperties: () => Promise<UiProperties>;
  refreshChannel: () => Promise<void>;
  refreshPage: () => Promise<void>;
}

type ParentMethod = keyof Parent;
type ParentMethodPromisedValue<M extends ParentMethod> = ReturnType<Parent[M]> extends Promise<infer U> ? U : never;

type PenpalError = Error & { code?: string };

enum UiExtensionErrorCode {
  'NotInIframe' = 'NotInIframe',
  'IncompatibleParent' = 'IncompatibleParent',
  'ConnectionDestroyed' = 'ConnectionDestroyed',
  'InternalError' = 'InternalError',
}

class UiExtensionError extends Error {
  constructor(public code: UiExtensionErrorCode,
              public message: string) {
    super(message);

    Object.setPrototypeOf(this, new.target.prototype);
  }

  toPromise(): Promise<any> {
    return Promise.reject(this);
  }

  static fromPenpal(error: PenpalError): UiExtensionError {
    const errorCode = UiExtensionError.convertPenpalErrorCode(error);
    return new UiExtensionError(errorCode, error.message);
  }

  private static convertPenpalErrorCode(error: PenpalError): UiExtensionErrorCode {
    switch (error.code) {
      case Penpal.ERR_NOT_IN_IFRAME:
        return UiExtensionErrorCode.NotInIframe;
      case Penpal.ERR_CONNECTION_DESTROYED:
        return UiExtensionErrorCode.ConnectionDestroyed;
      default:
        return UiExtensionErrorCode.InternalError;
    }
  }
}

abstract class UiScope {
  protected constructor(protected _parent: Parent) {
  }

  protected callParent<M extends ParentMethod>(method: M): Promise<ParentMethodPromisedValue<M>> {
    if (!this._parent[method]) {
      return new UiExtensionError(UiExtensionErrorCode.IncompatibleParent, `missing ${method}()`).toPromise();
    }
    try {
      return this._parent[method]()
        .catch(UiScope.convertPenpalError);
    } catch (error) {
      return UiScope.convertPenpalError(error);
    }
  }

  protected static convertPenpalError(error: PenpalError): Promise<any> {
    return UiExtensionError.fromPenpal(error).toPromise();
  }
}

class Page extends UiScope {

  constructor(parent: Parent, private _eventEmitter: Emittery.Typed<PageEvents>) {
    super(parent);
  }

  get(): Promise<PageProperties> {
    return this.callParent('getPage');
  }

  refresh(): Promise<void> {
    return this.callParent('refreshPage');
  }

  on<Name extends Extract<keyof PageEvents, string>>
      (event: Name, listener: (eventData: PageEvents[Name]) => any): Emittery.UnsubscribeFn {
    return this._eventEmitter.on(event, listener);
  }
}

class Channel extends UiScope {

  page: Page;

  constructor(_parent: Parent, pageEventEmitter: Emittery.Typed<PageEvents>) {
    super(_parent);
    this.page = new Page(_parent, pageEventEmitter);
  }

  refresh(): Promise<void> {
    return this.callParent('refreshChannel');
  }
}

export class Ui extends UiScope implements UiProperties {
  baseUrl: string;
  channel: Channel;
  extension: {
    config: string,
  };
  locale: string;
  timeZone: string;
  user: string;
  version: string;

  constructor(_parent: Parent, pageEventEmitter: Emittery.Typed<PageEvents>) {
    super(_parent);
    this.channel = new Channel(_parent, pageEventEmitter);
  }

  init(): Promise<Ui> {
    return this.callParent('getProperties')
      .then(properties => Object.assign(this, properties));
  }
}

export default class UiExtension {
  static register(): Promise<Ui> {
    const pageEventEmitter = new Emittery.Typed<PageEvents>();

    return UiExtension.connect(pageEventEmitter)
      .then(parent => new Ui(parent, pageEventEmitter).init());
  }

  private static connect(pageEventEmitter: Emittery.Typed<PageEvents>): Promise<Parent> {
    const parentOrigin = new URLSearchParams(window.location.search).get('br.parentOrigin');
    try {
      return Penpal.connectToParent({
        parentOrigin,
        methods: {
          emitPageEvent: pageEventEmitter.emit.bind(pageEventEmitter),
        },
      }).promise;
    } catch (penpalError) {
      return UiExtensionError.fromPenpal(penpalError).toPromise();
    }
  }
}

// enable UiExtension.register() in ui-extension.min.js
export const register = UiExtension.register;
