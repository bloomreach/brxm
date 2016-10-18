/*
 * Copyright 2015-2016 Hippo B.V. (http://www.onehippo.com)
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

import angular from 'angular';
import 'angular-mocks';

describe('SessionService', () => {
  let $rootScope;
  let $q;
  let SessionService;
  let HstService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$rootScope_, _$q_, _SessionService_, _HstService_) => {
      $rootScope = _$rootScope_;
      $q = _$q_;
      SessionService = _SessionService_;
      HstService = _HstService_;
    });

    spyOn(HstService, 'initializeSession').and.returnValue($q.when());
  });

  it('should exist', () => {
    expect(SessionService).toBeDefined();
  });

  it('should always be readonly before initialization', () => {
    expect(SessionService.hasWriteAccess()).toEqual(false);
  });

  it('should resolve a promise with the channel argument when initialization is successful', () => {
    const promiseSpy = jasmine.createSpy('promiseSpy');
    SessionService.initialize('hostname', 'mountId').then(promiseSpy);
    $rootScope.$apply();
    expect(promiseSpy).toHaveBeenCalled();
    expect(HstService.initializeSession).toHaveBeenCalledWith('hostname', 'mountId');
  });

  it('should reject a promise when initialization fails', () => {
    HstService.initializeSession.and.returnValue($q.reject());
    const catchSpy = jasmine.createSpy('catchSpy');
    SessionService.initialize().catch(catchSpy);
    $rootScope.$apply();
    expect(catchSpy).toHaveBeenCalled();
  });

  it('should not allow anything before initializing', () => {
    expect(SessionService.hasWriteAccess()).toEqual(false);
    expect(SessionService.canManageChanges()).toEqual(false);
    expect(SessionService.canDeleteChannel()).toEqual(false);
    expect(SessionService.isCrossChannelPageCopySupported()).toEqual(false);
  });

  it('should set privileges after initializing', () => {
    HstService.initializeSession.and.returnValue($q.when({
      canWrite: true,
      canManageChanges: true,
      canDeleteChannel: true,
      crossChannelPageCopySupported: true,
    }));
    SessionService.initialize();
    $rootScope.$apply();

    expect(SessionService.hasWriteAccess()).toEqual(true);
    expect(SessionService.canManageChanges()).toEqual(true);
    expect(SessionService.canDeleteChannel()).toEqual(true);
    expect(SessionService.isCrossChannelPageCopySupported()).toEqual(true);
  });

  it('should not allow anything when no privileges are defined', () => {
    SessionService.initialize();
    $rootScope.$apply();

    expect(SessionService.hasWriteAccess()).toEqual(false);
    expect(SessionService.canManageChanges()).toEqual(false);
    expect(SessionService.canDeleteChannel()).toEqual(false);
    expect(SessionService.isCrossChannelPageCopySupported()).toEqual(false);
  });

  it('should call all registered callbacks upon successful initialization', () => {
    const cb1 = jasmine.createSpy('cb1');
    const cb2 = jasmine.createSpy('cb2');

    SessionService.registerInitCallback('cb1', cb1);
    SessionService.registerInitCallback('cb2', cb2);

    expect(cb1).not.toHaveBeenCalled();
    expect(cb2).not.toHaveBeenCalled();

    SessionService.initialize();

    expect(cb1).not.toHaveBeenCalled();
    expect(cb2).not.toHaveBeenCalled();

    $rootScope.$digest();

    expect(cb1).toHaveBeenCalled();
    expect(cb2).toHaveBeenCalled();
  });

  it('should no longer call an unregistered callback', () => {
    const cb1 = jasmine.createSpy('cb1');
    const cb2 = jasmine.createSpy('cb2');

    SessionService.registerInitCallback('cb1', cb1);
    SessionService.registerInitCallback('cb2', cb2);

    SessionService.unregisterInitCallback('cb1');
    SessionService.unregisterInitCallback('cb3'); // should be ignored

    SessionService.initialize();
    $rootScope.$digest();

    expect(cb1).not.toHaveBeenCalled();
    expect(cb2).toHaveBeenCalled();
  });
});
