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

import { fakeAsync, tick } from '@angular/core/testing';
import { NavItemDtoMock } from 'projects/navapp/src/app/models/dto/nav-item-dto.mock';

import { ChildApi, NavigationTrigger, NavItem } from '../lib/api';

import { DEFAULT_COMMUNICATION_TIMEOUT } from './utils';
import { wrapWithTimeout } from './wrap-with-timeout';

describe('wrapWithTimeout', () => {
  const navItemsMock: NavItem[] = [
    new NavItemDtoMock(),
    new NavItemDtoMock(),
  ];

  it('should wrap provided api', async () => {
    const api: ChildApi = {
      getNavItems: async () => navItemsMock,
    };

    const promisedApi = wrapWithTimeout(api, 1);
    const navItems = await promisedApi.getNavItems();
    expect(navItems).toEqual(navItemsMock);
  });

  it('should wrap provided api in promises with default timeout', fakeAsync(() => {
    const api: ChildApi = {
      getNavItems: () => new Promise(() => {}),
    };

    const promisedApi = wrapWithTimeout(api);

    promisedApi.getNavItems().catch(e => {
      expect(e).toBe('getNavItems call timed out');
    });

    tick(DEFAULT_COMMUNICATION_TIMEOUT);
  }));

  it('should not wrap the provided api if the timeout is not set', () => {
    const api: ChildApi = {
      getNavItems: async () => navItemsMock,
    };

    const promisedApi = wrapWithTimeout(api, null);

    expect(promisedApi).toEqual(api);
  });

  it('should not wrap the provided api if the timeout is negative', () => {
    const api: ChildApi = {
      getNavItems: async () => navItemsMock,
    };

    const promisedApi = wrapWithTimeout(api, -1);

    expect(promisedApi).toEqual(api);
  });

  it('should reject after timeout', fakeAsync(() => {
    const api: ChildApi = {
      navigate: (): Promise<void> => new Promise(resolve => {
        setTimeout(resolve, 101);
      }),
    };

    const promisedApi = wrapWithTimeout(api, 100) as ChildApi;

    promisedApi.navigate({ path: 'test' }, NavigationTrigger.NotDefined).catch(e => {
      expect(e).toBe('navigate call timed out');
    });

    tick(101);
  }));

  it('should be able to get rejected', async () => {
    const errorMessage = 'Throwing error from method';
    const api: ChildApi = {
      navigate: (): Promise<void> => Promise.reject(errorMessage),
    };

    const promisedApi = wrapWithTimeout(api, 100);

    try {
      await promisedApi.navigate({ path: 'test' }, NavigationTrigger.NotDefined);
    } catch (err) {
      expect(err).toEqual(errorMessage);
    }
  });
});
