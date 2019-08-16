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

import { of } from 'rxjs';

import { createSpyObj } from '../../../test-utilities';
import { NavConfigService } from '../../services/nav-config.service';
import { ClientApp } from '../models/client-app.model';

import { ClientAppService } from './client-app.service';

describe('ClientAppService', () => {
  let navConfigService: NavConfigService;
  let service: ClientAppService;

  const navItemsMock = [
    {
      id: 'item1',
      appIframeUrl: 'http://app1.com',
      path: 'some-path',
    },
    {
      id: 'item2',
      appIframeUrl: 'http://app1.com',
      path: 'some/path/to/another/resource',
    },
    {
      id: 'item3',
      appIframeUrl: 'http://app2.com',
      path: 'some-path',
    },
  ];

  beforeEach(() => {
    navConfigService = createSpyObj([], {
      navItems$: of(navItemsMock),
    });

    service = new ClientAppService(navConfigService);
  });

  it('should exist', () => {
    expect(service).toBeDefined();
  });

  it('should init', () => {
    const expected = [
      new ClientApp('http://app1.com'),
      new ClientApp('http://app2.com'),
    ];
    let actual: ClientApp[];

    service.apps$.subscribe(apps => (actual = apps));
    service.init();

    expect(actual).toEqual(expected);
  });

  describe('when initialized', () => {
    beforeEach(() => {
      service.init();
    });

    it('should return an app by id', () => {
      const expected = new ClientApp('http://app1.com');

      const actual = service.getApp('http://app1.com');

      expect(expected).toEqual(actual);
    });

    it('should return undefined before some application has been activated', () => {
      expect(service.activeApp).toBeUndefined();
    });

    it('should return the active application', () => {
      const expected = new ClientApp('http://app1.com');

      service.activateApplication('http://app1.com');

      const actual = service.activeApp;

      expect(actual).toEqual(expected);
    });

    describe('when connected', () => {
      const clientApiWithSitesSupport = {
        updateSelectedSite: () => Promise.resolve(),
      };

      beforeEach(() => {
        service.addConnection('http://app1.com', {});
        service.addConnection('http://app2.com', clientApiWithSitesSupport);

        service.activateApplication('http://app2.com');
      });

      it('should check whether the application supports sites with success', () => {
        const expected = true;

        const actual = service.doesActiveAppSupportSites;

        expect(expected).toBe(actual);
      });

      it('should check whether the application supports sites without success', () => {
        service.activateApplication('http://app1.com');

        const expected = false;

        const actual = service.doesActiveAppSupportSites;

        expect(expected).toBe(actual);
      });
    });
  });
});
