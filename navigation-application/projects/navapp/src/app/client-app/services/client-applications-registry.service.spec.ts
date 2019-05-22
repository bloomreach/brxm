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

import { ClientApplicationHandler } from '../models';

import { ClientApplicationsRegistryService } from './client-applications-registry.service';

describe('ClientApplicationsRegistryService', () => {
  let service: ClientApplicationsRegistryService;

  beforeEach(() => {
    service = new ClientApplicationsRegistryService();
  });

  it('should set a key', () => {
    const handler = new ClientApplicationHandler('some/url', undefined);
    service.set('some-key', handler);

    const actual = service.has('some-key');

    expect(actual).toBeTruthy();
  });

  it('should return the set key', () => {
    const handler = new ClientApplicationHandler('some/url', undefined);
    service.set('some-key', handler);

    const actual = service.get('some-key');
    const expected = new ClientApplicationHandler('some/url', undefined);

    expect(actual).toEqual(expected);
  });
});
