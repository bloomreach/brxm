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
  let parent;

  beforeEach(() => {
    angular.mock.module('hippo-cm-iframe');

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

      parent = jasmine.createSpyObj('child', ['emit']);
      spyOn(Penpal, 'connectToParent').and.returnValue({ promise: $q.resolve(parent) });
    });
  });

  describe('connect', () => {
    let methods;

    beforeEach(() => {
      Penpal.connectToParent.and.callFake((options) => {
        ({ methods } = options);

        return { promise: Promise.resolve(parent) };
      });

      CommunicationService.connect();
      $rootScope.$digest();
    });

    it('should emit a channel manager prefixed event on the emit callback', () => {
      spyOn($rootScope, '$emit');
      methods.emit('event', 'something');

      expect($rootScope.$emit).toHaveBeenCalledWith('cm:event', 'something');
    });
  });

  describe('emit', () => {
    it('should not fail if it was not previously connected', () => {
      expect(() => CommunicationService.emit('event', 'something')).not.toThrow();
    });

    it('should pass a call to the child', () => {
      CommunicationService.connect();
      $rootScope.$digest();
      CommunicationService.emit('event', 'something');

      expect(parent.emit).toHaveBeenCalledWith('event', 'something');
    });
  });
});
