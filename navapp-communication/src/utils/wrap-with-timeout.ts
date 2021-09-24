/*
 * Copyright 2019-2021 BloomReach. All rights reserved. (https://www.bloomreach.com/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Methods } from './methods';
import { DEFAULT_COMMUNICATION_TIMEOUT } from './utils';

const wrapMethod =
  <R>(method: (...arg: any[]) => R, methodName: string, timeout: number): ((...arg: any[]) => Promise<R>) =>
  (...args) =>
    // eslint-disable-next-line no-async-promise-executor
    new Promise(async (resolve, reject) => {
      const timer = setTimeout(() => {
        // eslint-disable-next-line prefer-promise-reject-errors
        reject(`${methodName} call timed out`);
      }, timeout);

      try {
        const value = await method(...args);
        clearTimeout(timer);
        resolve(value);
      } catch (error) {
        reject(error);
      }
    });

export function wrapWithTimeout<T extends Methods>(api: T, timeout = DEFAULT_COMMUNICATION_TIMEOUT): T {
  if (!timeout || timeout < 0) {
    return api;
  }

  return Object.keys(api).reduce((wrappedApi, methodName) => {
    wrappedApi[methodName] = wrapMethod(api[methodName].bind(api), methodName, timeout);

    return wrappedApi;
  }, {} as T);
}
