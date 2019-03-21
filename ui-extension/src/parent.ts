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
 * Handles all interaction with the parent frame using the Penpal library.
 * @module parent
 * @see https://github.com/Aaronius/penpal
 */

/**
 * Disable TSLint for imports that start with an uppercase letter
 * @see https://github.com/Microsoft/tslint-microsoft-contrib/issues/387
 */
import Emittery from 'emittery'; // tslint:disable-line:import-name
import Penpal from 'penpal';     // tslint:disable-line:import-name
import { UiExtensionError, UiExtensionErrorCode } from './api';

type Callable<T = unknown, U extends unknown[] = unknown[]> = (...args: U) => T;
type MethodOf<T> = keyof Pick<T, {
  [K in keyof T]: T[K] extends Callable ? K : never;
}[keyof T]>;
type PromisedOf<T extends Callable> = T extends Callable<Promise<infer U>> ? U : never;
type ArgumentsOf<T extends Callable> = T extends Callable<any, infer U> ? U : never;

type PenpalError = Error & { code?: string };

export type ParentMethod<T = void, U extends unknown[] = []> = Callable<Promise<T>, U>;
export interface Parent {
  [K: string]: ParentMethod<any, any[]>;
}

class ParentError extends Error implements UiExtensionError {
  constructor(public code: UiExtensionErrorCode,
              public message: string) {
    super(message);

    Object.setPrototypeOf(this, new.target.prototype);
  }

  toPromise(): Promise<any> {
    return Promise.reject(this);
  }

  static fromPenpal(error: PenpalError): ParentError {
    const errorCode = ParentError.convertPenpalErrorCode(error);
    return new ParentError(errorCode, error.message);
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

export class ParentConnection<T extends Parent = any> {
  constructor(private _parent: T) {}

  call<M extends MethodOf<T>>(method: M, ...args: ArgumentsOf<T[M]>): Promise<PromisedOf<T[M]>> {
    if (!this._parent[method]) {
      return new ParentError(UiExtensionErrorCode.IncompatibleParent, `missing ${method}()`).toPromise();
    }
    try {
      return this._parent[method](...args)
        .catch(ParentConnection.convertPenpalError);
    } catch (error) {
      return ParentConnection.convertPenpalError(error);
    }
  }

  private static convertPenpalError(error: PenpalError): Promise<any> {
    return ParentError.fromPenpal(error).toPromise();
  }
}

export function connect<T extends Parent = any>(
  parentOrigin: string,
  eventEmitter: Emittery,
): Promise<ParentConnection<T>> {
  try {
    return Penpal.connectToParent({
      parentOrigin,
      methods: {
        emitEvent: eventEmitter.emit.bind(eventEmitter),
      },
    }).promise.then((parent: T) => new ParentConnection(parent));
  } catch (penpalError) {
    return ParentError.fromPenpal(penpalError).toPromise();
  }
}
