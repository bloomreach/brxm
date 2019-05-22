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

import { Renderer2, RendererFactory2 } from '@angular/core';
import { of } from 'rxjs';

import { NavigationConfigurationService } from '../../services';
import { navConfig } from '../../test-mocks';
import { ClientApplicationHandler } from '../models';

import { ClientAppService } from './client-app.service';

describe('ClientApplicationsRegistryService', () => {
  const navConfigServiceMock: NavigationConfigurationService = {
    navItems$: of(navConfig),
  } as any;

  const rendererMock: Renderer2 = {
    createElement: () => {},
  } as any;

  const rendererFactoryMock: RendererFactory2 = {
    createRenderer: () => rendererMock,
  } as any;

  const fakeIframeEl: HTMLIFrameElement = {} as any;

  let service: ClientAppService;

  beforeEach(() => {
    spyOn(rendererMock, 'createElement').and.returnValue(fakeIframeEl);

    service = new ClientAppService(navConfigServiceMock, rendererFactoryMock);
  });

  it('should create and return an application handler if it doesn\'t exist', () => {
    const actual = service.getApplicationHandler('iframe1-url');
    const expected = new ClientApplicationHandler('iframe1-url', fakeIframeEl);

    expect(actual).toEqual(expected);
  });

  it('should emit applicationCreated$ value when a new application is created', () => {
    let actual;
    const expected = new ClientApplicationHandler('iframe1-url', fakeIframeEl);

    service.applicationCreated$.subscribe(app => (actual = app));
    service.getApplicationHandler('iframe1-url');

    expect(actual).toEqual(expected);
  });
});
