/*
 * Copyright 2019-2020 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

import { wrapWithTimeout } from '../utils/wrap-with-timeout';

import {
  ChildApi,
  ChildConnectConfig,
} from './api';

const wrapChildMethodsWithTimeout = (methods: ChildApi, timeout: number) => {
  const beforeNavigation = methods.beforeNavigation;
  delete methods.beforeNavigation;

  const wrappedMethods = wrapWithTimeout(methods, timeout);

  wrappedMethods.beforeNavigation = beforeNavigation;

  return wrappedMethods;
};

/**
 * Method to connect to an application in an iframe window.
 *
 * @param iframe The iframe element that should contain another instance of the navigation
 *               communication library looking to connect
 * @param methods The api the parent exposes to the child
 * @param connectionTimeout The time in ms after which an error will be thrown if the child has failed to connect
 * @param methodInvocationTimeout The time in ms after which an error will be thrown if a method doesn't return a response
 */
export async function connectToChild({
  iframe,
  methods,
  connectionTimeout,
  methodInvocationTimeout,
}: ChildConnectConfig): Promise<ChildApi> {
  const childMethods = await Penpal.connectToChild({ iframe, methods, timeout: connectionTimeout }).promise;

  return wrapChildMethodsWithTimeout(childMethods, methodInvocationTimeout);
}
