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
  NavItem,
  NavLocation,
  ParentConnectConfig,
  ParentPromisedApi,
} from './api';
import { mergeIntersecting } from './utils';

export function createProxies(methods: ChildApi): ChildApi {
  return {
    getNavItems(): NavItem[] {
      return methods.getNavItems();
    },
    navigate(location: NavLocation): void {
      return methods.navigate(location);
    },
  };
}

export function connectToParent({
  parentOrigin,
  methods = {},
}: ParentConnectConfig): Promise<ParentPromisedApi> {
  const proxies: ChildApi = createProxies(methods);
  const proxiedMethods = mergeIntersecting(methods, proxies);

  return Penpal.connectToParent({ parentOrigin, methods: proxiedMethods })
    .promise;
}
