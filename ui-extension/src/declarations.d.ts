/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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

type Callable<T = unknown, U extends unknown[] = unknown[]> = (...args: U) => T;

// jsdom does not support MutationObserver (https://github.com/jsdom/jsdom/issues/639) so declare it here
declare module 'MutationObserver' {
  export default MutationObserver;
}

declare module 'penpal' {
  type ERR_CONNECTION_DESTROYED = 'ConnectionDestroyed';
  type ERR_NOT_IN_IFRAME = 'NotInIframe';

  export type ConnectionMethods<T = {}> = { [P in keyof T]: () => Promise<any> };

  export type AsyncMethodReturns<T, K extends keyof T = keyof T> = {
    [KK in K]: T[KK] extends (...args: any[]) => PromiseLike<any>
      ? T[KK]
      : T[KK] extends (...args: infer A) => infer R
      ? (...args: A) => Promise<R>
      : T[KK]
  };

  export interface IConnectionObject<Methods extends ConnectionMethods> {
    promise: Promise<AsyncMethodReturns<Methods>>;
    destroy: () => {};
  }

  export interface IConnectionOptions {
    methods?: ConnectionMethods;
    timeout?: number;
  }

  export interface IParentConnectionOptions extends IConnectionOptions {
    parentOrigin?: string;
  }

  export interface PenpalStatic {
    connectToParent<Methods extends ConnectionMethods = any>(
      options?: IParentConnectionOptions,
    ): // tslint:disable-next-line: no-unnecessary-generics
    IConnectionObject<Methods>;

    ERR_CONNECTION_DESTROYED: ERR_CONNECTION_DESTROYED;
    ERR_NOT_IN_IFRAME: ERR_NOT_IN_IFRAME;
  }

  const Penpal: PenpalStatic;
  export default Penpal;
}
