/*
 * Copyright 2015-2019 Hippo B.V. (http://www.onehippo.com)
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
  let $httpBackend;
  let $rootScope;
  let $q;
  let SessionService;
  let HstService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$httpBackend_, _$q_, _$rootScope_, _HstService_, _SessionService_) => {
      $httpBackend = _$httpBackend_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      HstService = _HstService_;
      SessionService = _SessionService_;
    });

    spyOn(HstService, 'initializeSession').and.returnValue($q.when());
  });

  describe('initializeContext', () => {
    it('performs a GET call to a webapp to trigger the construction of the CMS session context', () => {
      $httpBackend.when('GET', '/site2/_cmssessioncontext').respond(204);
      $httpBackend.expectGET('/site2/_cmssessioncontext');

      SessionService.initializeContext('/site2');
      $httpBackend.flush();

      $httpBackend.verifyNoOutstandingRequest();
      $httpBackend.verifyNoOutstandingExpectation();
    });
  });

  describe('initializeState', () => {
    it('should resolve a promise with the channel argument when initialization is successful', () => {
      const promiseSpy = jasmine.createSpy('promiseSpy');
      const channel = {};
      SessionService.initializeState(channel).then(promiseSpy);
      $rootScope.$apply();
      expect(promiseSpy).toHaveBeenCalled();
      expect(HstService.initializeSession).toHaveBeenCalledWith(channel);
    });

    it('should reject a promise when initialization fails', () => {
      HstService.initializeSession.and.returnValue($q.reject());
      const catchSpy = jasmine.createSpy('catchSpy');
      SessionService.initializeState().catch(catchSpy);
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
      SessionService.initializeState();
      $rootScope.$apply();

      expect(SessionService.hasWriteAccess()).toEqual(true);
      expect(SessionService.canManageChanges()).toEqual(true);
      expect(SessionService.canDeleteChannel()).toEqual(true);
      expect(SessionService.isCrossChannelPageCopySupported()).toEqual(true);
    });

    it('should not allow anything when no privileges are defined', () => {
      SessionService.initializeState();
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

      SessionService.initializeState();

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

      SessionService.initializeState();
      $rootScope.$digest();

      expect(cb1).not.toHaveBeenCalled();
      expect(cb2).toHaveBeenCalled();
    });
  });
});
