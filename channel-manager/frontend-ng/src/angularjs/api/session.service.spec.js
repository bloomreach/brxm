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

describe('SessionService', function () {
  'use strict';

  var $rootScope;
  var deferred;
  var sessionService;
  var channelMock = 'channelMock';

  beforeEach(function () {
    module('hippo-cm-api');

    inject(function ($q, _$rootScope_, SessionService, HstService) {
      $rootScope = _$rootScope_;
      sessionService = SessionService;
      deferred = $q.defer();
      spyOn(HstService, 'initializeSession').and.returnValue(deferred.promise);
    });
  });

  it('should exist', function () {
    expect(sessionService).toBeDefined();
  });

  it('should always be readonly before initialization', function () {
    expect(sessionService.hasWriteAccess()).toEqual(false);
  });

  it('should resolve a promise with the channel argument when initialization is successful', function () {
    var promiseSpy = jasmine.createSpy('promiseSpy');
    sessionService
      .initialize(channelMock)
      .then(promiseSpy);

    deferred.resolve();
    $rootScope.$apply();
    expect(promiseSpy).toHaveBeenCalledWith(channelMock);
  });

  it('should reject a promise when initialization fails', function () {
    var catchSpy = jasmine.createSpy('catchSpy');
    sessionService
      .initialize(channelMock)
      .catch(catchSpy);

    deferred.reject();
    $rootScope.$apply();
    expect(catchSpy).toHaveBeenCalled();
  });

  it('should set canWrite after initializing', function () {
    sessionService.initialize(channelMock);
    deferred.resolve(true);
    $rootScope.$apply();
    expect(sessionService.hasWriteAccess()).toEqual(true);
  });
});
