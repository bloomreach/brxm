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

export async function wrapWithTimeout(api: ChildApi): Promise<ChildPromisedApi> {
  let communicationTimeout: number;

  if (api.getConfig) {
    const config = await api.getConfig();
    communicationTimeout = config.communicationTimeout;
  }

  return Object
    .keys(api)
    .reduce((wrappedApi, methodName) => {
      wrappedApi[methodName] = (...args) => {
        return new Promise(async (resolve, reject) => {
          const timer = setTimeout(() => {
            reject(`${methodName} call timed out`);
          }, communicationTimeout || DEFAULT_COMMUNICATION_TIMEOUT);

          try {
            const value = await api[methodName](args);
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
 * @param parentOrigin TODO document or remove
 * @param methods TODO document or remove
 */
export function connectToParent({
  parentOrigin,
  methods,
}: ParentConnectConfig): Promise<ParentPromisedApi> {
  return wrapWithTimeout(methods)
    .then(proxyMethods => Penpal.connectToParent({ parentOrigin, methods: proxyMethods }).promise);
}
