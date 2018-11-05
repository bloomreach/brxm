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
 * Disable TSLint for imports that start with an uppercase letter
 * @see https://github.com/Microsoft/tslint-microsoft-contrib/issues/387
 */
import Emittery = require('emittery');  // tslint:disable-line:import-name
import Penpal from 'penpal';            // tslint:disable-line:import-name

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

interface PageEvents {
  navigate: PageProperties;
}

interface Parent {
  getPage: () => Promise<PageProperties>;
  getProperties: () => Promise<UiProperties>;
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

  protected call<M extends ParentMethod>(method: M): Promise<ParentMethodPromisedValue<M>> {
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

  private static convertPenpalError(error: PenpalError): Promise<any> {
    return UiExtensionError.fromPenpal(error).toPromise();
  }
}

abstract class UiScopeEmitter<Events> extends UiScope {

  constructor(parent: Parent, private _eventEmitter: Emittery, private _eventScope: string) {
    super(parent);
  }

  on(eventName: keyof Events, listener: (eventData: Events[keyof Events]) => any) {
    const scopedEventName = `${this._eventScope}.${eventName}`;
    return this._eventEmitter.on(scopedEventName, listener);
  }
}

class Page extends UiScopeEmitter<PageEvents> {

  get(): Promise<PageProperties> {
    return this.call('getPage');
  }
}

class Channel extends UiScope {

  page: Page;

  constructor(parent: Parent, eventEmitter: Emittery) {
    super(parent);
    this.page = new Page(parent, eventEmitter, 'channel.page');
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

  constructor(parent: Parent, eventEmitter: Emittery) {
    super(parent);
    this.channel = new Channel(parent, eventEmitter);
  }

  init(): Promise<Ui> {
    return this.call('getProperties')
      .then(properties => Object.assign(this, properties));
  }
}

export default class UiExtension {
  static register(): Promise<Ui> {
    const eventEmitter = new Emittery();

    return UiExtension.connect(eventEmitter)
      .then(parent => new Ui(parent, eventEmitter).init());
  }

  private static connect(eventEmitter: Emittery): Promise<Parent> {
    const parentOrigin = new URLSearchParams(window.location.search).get('br.parentOrigin');
    try {
      return Penpal.connectToParent({
        parentOrigin,
        methods: {
          emitEvent: eventEmitter.emit.bind(eventEmitter),
        },
      }).promise;
    } catch (penpalError) {
      return UiExtensionError.fromPenpal(penpalError).toPromise();
    }
  }
}

// enable UiExtension.register() in ui-extension.min.js
export const register = UiExtension.register;
