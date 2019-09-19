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
  ChildConnectConfig,
  ChildPromisedApi,
} from './api';

/**
 * Method to connect to an application in an iframe window.
 *
 * @param iframe The iframe element that should contain another instance of the navigation
 *               communication library looking to connect
 * @param methods The api the parent exposes to the child
 * @param timeout The time in ms after which an error will be thrown if the child has failed to connect
 */
export function connectToChild({
  iframe,
  methods,
  timeout,
}: ChildConnectConfig): Promise<ChildPromisedApi> {
  return Penpal.connectToChild({ iframe, methods, timeout }).promise;
}
