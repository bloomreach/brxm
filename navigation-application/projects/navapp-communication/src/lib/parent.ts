/*
 * Copyright 2019 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

import Penpal from 'penpal';

import {
  ChildApi,
  ChildPromisedApi,
  ParentConnectConfig,
  ParentPromisedApi,
} from './api';
import { DEFAULT_COMMUNICATION_TIMEOUT } from './utils';

export async function getTimeoutValue(api: ChildApi): Promise<number> {
  let timeout = DEFAULT_COMMUNICATION_TIMEOUT;

  if (api.getConfig) {
    const config = await api.getConfig();

    if (config.communicationTimeout) {
      timeout = config.communicationTimeout;
    }
  }

  return timeout;
}

export async function wrapWithTimeout(api: ChildApi, timeout: number): Promise<ChildPromisedApi> {
  return Object
    .keys(api)
    .reduce((wrappedApi, methodName) => {
      wrappedApi[methodName] = (...args) => {
        return new Promise(async (resolve, reject) => {
          const timer = setTimeout(() => {
            reject(`${methodName} call timed out`);
          }, timeout);

          try {
            const value = await api[methodName](...args);
            clearTimeout(timer);
            resolve(value);
          } catch (error) {
            reject(error);
          }
        });
      };

      return wrappedApi;
    }, {});
}
/**
 * Method to connect to a parent window.
 *
 * @param parentOrigin the origin should match the parent origin or a connection wont be allowed
 * @param methods the api that the child app exposes to the parent
 */
export async function connectToParent({
  parentOrigin,
  methods,
}: ParentConnectConfig): Promise<ParentPromisedApi> {
  const timeout = await getTimeoutValue(methods);
  const proxyMethods = await wrapWithTimeout(methods, timeout);
  return Penpal.connectToParent({ parentOrigin, methods: proxyMethods }).promise;
}
