/*
 * (C) Copyright 2019 Bloomreach. All rights reserved. (https://www.bloomreach.com)
 */

import { Renderer2, RendererFactory2 } from '@angular/core';
import { of } from 'rxjs';

import { NavigationConfigurationService } from '../../services';
import { navigationConfigurationMapMock } from '../../test-mocks';
import { ClientApplicationHandler } from '../models';

import { ClientApplicationsManagerService } from './client-applications-manager.service';
import { ClientApplicationsRegistryService } from './client-applications-registry.service';

describe('ClientApplicationsRegistryService', () => {
  const registryServiceMock: ClientApplicationsRegistryService = {
    set: () => {},
    has: () => {},
    get: () => {},
    getAll: () => {},
  } as any;

  const navConfigServiceMock: NavigationConfigurationService = {
    navigationConfiguration$: of(navigationConfigurationMapMock),
  } as any;

  const rendererMock: Renderer2 = {
    createElement: () => {},
  } as any;

  const rendererFactoryMock: RendererFactory2 = {
    createRenderer: () => rendererMock,
  } as any;

  const fakeIframeEl: HTMLIFrameElement = {} as any;

  let service: ClientApplicationsManagerService;

  beforeEach(() => {
    spyOn(rendererMock, 'createElement').and.returnValue(fakeIframeEl);

    service = new ClientApplicationsManagerService(registryServiceMock, navConfigServiceMock, rendererFactoryMock);
  });

  it('should create and return an application handler if it doesn\'t exist', () => {
    const actual = service.getApplicationHandler('iframe1-url');
    const expected = new ClientApplicationHandler('iframe1-url', fakeIframeEl);

    expect(actual).toEqual(expected);
  });

  it('should emit applicationCreated$ value when a new application is created', () => {
    let actual;
    const expected = new ClientApplicationHandler('iframe1-url', fakeIframeEl);

    service.applicationCreated$.subscribe(app => actual = app);
    service.getApplicationHandler('iframe1-url');

    expect(actual).toEqual(expected);
  });

  it('should return existing app when it exists in the registry', () => {
    const fakeApp = new ClientApplicationHandler('iframe1-url', fakeIframeEl);
    spyOn(registryServiceMock, 'get').and.returnValue(fakeApp);

    const expected = new ClientApplicationHandler('iframe1-url', fakeIframeEl);

    const actual = service.getApplicationHandler('iframe1-url');

    expect(actual).toEqual(expected);
  });

  it('should activate an application', () => {
    const iframes: HTMLIFrameElement[] = [
      { classList: jasmine.createSpyObj(['add', 'remove']) },
      { classList: jasmine.createSpyObj(['add', 'remove']) },
      { classList: jasmine.createSpyObj(['add', 'remove']) },
    ] as any;

    const fakeApps = [
      new ClientApplicationHandler('iframe1-url', iframes[0]),
      new ClientApplicationHandler('iframe2-url', iframes[1]),
      new ClientApplicationHandler('iframe3-url', iframes[2]),
    ];

    spyOn(registryServiceMock, 'has').and.callFake(id => fakeApps.some(app => app.url === id));
    spyOn(registryServiceMock, 'getAll').and.returnValue(fakeApps);

    service.activateApplication('iframe2-url');

    expect(iframes[0].classList.add).toHaveBeenCalledWith('hidden');
    expect(iframes[0].classList.remove).not.toHaveBeenCalled();
    expect(iframes[1].classList.add).not.toHaveBeenCalled();
    expect(iframes[1].classList.remove).toHaveBeenCalledWith('hidden');
    expect(iframes[2].classList.add).toHaveBeenCalledWith('hidden');
    expect(iframes[2].classList.remove).not.toHaveBeenCalled();
  });
});
