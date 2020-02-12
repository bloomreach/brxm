/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

describe('CommunicationService', () => {
  let $q;
  let $rootScope;
  let CommunicationService;
  let Penpal;
  let child;
  let destroy;
  let iframe;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((
      _$q_,
      _$rootScope_,
      _CommunicationService_,
      _Penpal_,
    ) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      CommunicationService = _CommunicationService_;
      Penpal = _Penpal_;

      child = jasmine.createSpyObj('child', ['emit']);
      destroy = jasmine.createSpy();
      iframe = {};
      spyOn(Penpal, 'connectToChild').and.returnValue({ destroy, promise: $q.resolve(child) });
    });
  });

  describe('connect', () => {
    let methods;

    beforeEach(() => {
      Penpal.connectToChild.and.callFake((options) => {
        ({ methods } = options);

        return { destroy, promise: Promise.resolve(child) };
      });

      CommunicationService.connect({ target: iframe, origin: 'http://localhost:3000' });
      $rootScope.$digest();
    });

    it('should pass an iframe', () => {
      expect(Penpal.connectToChild).toHaveBeenCalledWith(jasmine.objectContaining({ iframe }));
    });

    it('should pass an origin', () => {
      expect(Penpal.connectToChild).toHaveBeenCalledWith(jasmine.objectContaining({
        childOrigin: 'http://localhost:3000',
      }));
    });

    it('should emit an iframe prefixed event on the emit callback', () => {
      spyOn($rootScope, '$emit');
      methods.emit('event', 'something');

      expect($rootScope.$emit).toHaveBeenCalledWith('iframe:event', 'something');
    });
  });

  describe('disconnect', () => {
    it('should not fail if it was not previously connected', () => {
      expect(() => CommunicationService.disconnect()).not.toThrow();
    });

    it('should destroy a connection', () => {
      CommunicationService.connect({ target: iframe, origin: 'http://localhost:3000' });
      $rootScope.$digest();
      CommunicationService.disconnect();

      expect(destroy).toHaveBeenCalledWith();
    });
  });

  describe('emit', () => {
    it('should not fail if it was not previously connected', () => {
      expect(() => CommunicationService.emit('event', 'something')).not.toThrow();
    });

    it('should pass a call to the child', () => {
      CommunicationService.connect({ target: iframe, origin: 'http://localhost:3000' });
      $rootScope.$digest();
      CommunicationService.emit('event', 'something');

      expect(child.emit).toHaveBeenCalledWith('event', 'something');
    });
  });
});
