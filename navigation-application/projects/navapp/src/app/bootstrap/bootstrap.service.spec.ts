/*!
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

import { async, fakeAsync, TestBed, tick } from '@angular/core/testing';

import { ClientAppService } from '../client-app/services/client-app.service';
import { DeepLinkingService } from '../deep-linking/deep-linking.service';
import { MenuStateService } from '../main-menu/services/menu-state.service';
import { BreadcrumbsService } from '../top-panel/services/breadcrumbs.service';

import { BootstrapService } from './bootstrap.service';

describe('BootstrapService', () => {
  let service: BootstrapService;

  let clientAppServiceInitResolve: () => void;
  const clientAppServiceMock = jasmine.createSpyObj('ClientAppService', [
    'init',
  ]);

  let menuStateServiceInitResolve: () => void;
  const menuStateServiceMock = jasmine.createSpyObj('MenuStateService', {
    init: new Promise(r => {
      menuStateServiceInitResolve = r;
    }),
  });

  let deepLinkingServiceInitialNavigationResolve: () => void;
  const deepLinkingServiceMock = jasmine.createSpyObj('BreadcrumbsService', {
    initialNavigation: new Promise(r => {
      deepLinkingServiceInitialNavigationResolve = r;
    }),
  });

  beforeEach(() => {
    clientAppServiceMock.init.and.returnValue(new Promise(r => {
      clientAppServiceInitResolve = r;
    }));

    TestBed.configureTestingModule({
      providers: [
        BootstrapService,
        { provide: ClientAppService, useValue: clientAppServiceMock },
        { provide: MenuStateService, useValue: menuStateServiceMock },
        { provide: DeepLinkingService, useValue: deepLinkingServiceMock },
      ],
    });

    service = TestBed.get(BootstrapService);
  });

  it('should call ClientAppService:init() first', fakeAsync(() => {
    service.bootstrap();

    tick();

    expect(clientAppServiceMock.init).toHaveBeenCalled();
    expect(menuStateServiceMock.init).not.toHaveBeenCalled();
    expect(deepLinkingServiceMock.initialNavigation).not.toHaveBeenCalled();
  }));

  it('should print an error in the console', fakeAsync(() => {
    spyOn(console, 'error');
    clientAppServiceMock.init.and.callFake(() => Promise.reject('Something went wrong'));
    let caughtError: string;

    service.bootstrap().catch(error => caughtError = error);

    tick();

    expect(console.error).toHaveBeenCalledWith('[NAVAPP] Bootstrap error: Something went wrong');
    expect(caughtError).toBe('Something went wrong');
  }));

  describe('when ClientAppService is initialized', () => {
    let bootstrapped = false;

    beforeEach(async(() => {
      service.bootstrap().then(() => bootstrapped = true);

      clientAppServiceInitResolve();
    }));

    it('should call MenuStateService:init()', () => {
      expect(menuStateServiceMock.init).toHaveBeenCalled();
      expect(deepLinkingServiceMock.initialNavigation).not.toHaveBeenCalled();
    });

    describe('when MenuStateService is initialized', () => {
      beforeEach(async(() => {
        menuStateServiceInitResolve();
      }));

      it('should call DeepLinkingService:initialNavigation()', () => {
        expect(deepLinkingServiceMock.initialNavigation).toHaveBeenCalled();
      });

      describe('after initial navigation', () => {
        beforeEach(async(() => {
          deepLinkingServiceInitialNavigationResolve();
        }));

        it('should resolve the promise', () => {
          expect(bootstrapped).toBeTruthy();
        });
      });
    });
  });
});
