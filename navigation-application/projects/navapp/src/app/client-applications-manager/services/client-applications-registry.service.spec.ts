/*
 * (C) Copyright 2019 Bloomreach. All rights reserved. (https://www.bloomreach.com)
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
