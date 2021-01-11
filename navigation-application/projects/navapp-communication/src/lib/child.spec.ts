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

import { ChildApi, ChildConnectConfig, ParentApi } from './api';
import { connectToChild } from './child';

describe('connectToChild', () => {
  let connectionConfig: ChildConnectConfig;
  let childMethods: ChildApi;

  beforeEach(() => {
    const parentMethods = {
      navigate: async () => {},
    } as unknown as ParentApi;

    const iframe: HTMLIFrameElement = document.createElement('iframe');
    iframe.src = 'about:blank';

    connectionConfig = {
      iframe,
      methods: parentMethods,
      connectionTimeout: 10000,
      methodInvocationTimeout: 5000,
    };

    childMethods = {
      getConfig: async () => ({} as any),
      getNavItems: async () => [],
      getSites: async () => [],
      getSelectedSite: async () => ({} as any),
      beforeNavigation: async () => true,
      onUserActivity: async () => {},
      logout: async () => {},
      navigate: async () => {},
      updateSelectedSite: async () => {},
    };

    spyOn(Penpal, 'connectToChild').and.returnValue({
      promise: Promise.resolve(childMethods),
    });
  });

  it('should pass config to penpal connectToChild', () => {
    const expected = {
      iframe: connectionConfig.iframe,
      methods: connectionConfig.methods,
      timeout: 10000,
    };

    connectToChild(connectionConfig);

    expect(Penpal.connectToChild).toHaveBeenCalledWith(expected);
  });

  it('should wrap child methods besides "beforeNavigation()"', async () => {
    const expectedBeforeNavigation = childMethods.beforeNavigation;

    const wrappedChildMethods = await connectToChild(connectionConfig);

    expect(wrappedChildMethods.beforeNavigation).toBe(expectedBeforeNavigation);
    expect(wrappedChildMethods.getNavItems).not.toBe(childMethods.getNavItems);
    expect(wrappedChildMethods.getSites).not.toBe(childMethods.getSites);
    expect(wrappedChildMethods.getSelectedSite).not.toBe(childMethods.getSelectedSite);
    expect(wrappedChildMethods.onUserActivity).not.toBe(childMethods.onUserActivity);
    expect(wrappedChildMethods.logout).not.toBe(childMethods.logout);
    expect(wrappedChildMethods.navigate).not.toBe(childMethods.navigate);
    expect(wrappedChildMethods.updateSelectedSite).not.toBe(childMethods.updateSelectedSite);
  });
});
