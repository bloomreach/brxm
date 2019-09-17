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
import { NavItemMock } from 'projects/navapp/src/app/models/dto/nav-item.mock';

import { ChildApi, ChildConfig, NavItem } from './api';
import { connectToParent, wrapWithTimeout } from './parent';

describe('connectToParent', () => {
  const navItemsMock = [
    new NavItemMock(),
    new NavItemMock(),
  ];

  beforeEach(() => {
    spyOn(Penpal, 'connectToParent').and.returnValue({ promise: Promise.resolve() });
  });

  it('should pass the config methods to penpal', async () => {
    const parentOrigin = 'about:blank';
    const config = {
      parentOrigin,
      methods: {},
    };

    await connectToParent(config);

    expect(Penpal.connectToParent).toHaveBeenCalledWith({
      parentOrigin,
      methods: {},
    });
  });

  it('should wrap provided api in promises if timeout value is provided', async () => {
    const api: ChildApi = {
      getConfig: (): ChildConfig => ({
        communicationTimeout: 100,
      }),
      getNavItems: (): NavItem[] => navItemsMock,
    };

    const promisedApi = await wrapWithTimeout(api);
    const navItems = await promisedApi.getNavItems();
    expect(navItems).toEqual(navItemsMock);
  });

  it('wrapped api methods should reject after timeout', async () => {
    const api: ChildApi = {
      getConfig: (): ChildConfig => ({
        communicationTimeout: 100,
      }),
      navigate: (): Promise<void> => new Promise(resolve => {
        setTimeout(resolve, 101);
      }),
    };

    const promisedApi = await wrapWithTimeout(api);

    try {
      await promisedApi.navigate({ path: 'test' });
    } catch (err) {
      // Error should be thrown because of timeout, so all is good
      return;
    }

    throw new Error('Promise should have been rejected because of timeout');
  });

  it('wrapped api methods should pass be able to get rejected', async () => {
    const errorMessage = 'Throwing error from method';
    const api: ChildApi = {
      getConfig: (): ChildConfig => ({
        communicationTimeout: 100,
      }),
      navigate: (): Promise<void> => Promise.reject(errorMessage),
    };

    const promisedApi = await wrapWithTimeout(api);

    try {
      await promisedApi.navigate({ path: 'test' });
    } catch (err) {
      expect(err).toEqual(errorMessage);
    }
  });

});
