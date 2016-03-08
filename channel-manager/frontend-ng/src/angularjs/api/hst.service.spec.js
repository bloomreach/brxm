/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

describe('HstService', function () {
  'use strict';

  var $httpBackend;
  var hstService;
  var ConfigServiceMock;

  var contextPath = '/testContextPath';
  var apiUrlPrefix = '/testApiUrlPrefix';
  var rootUuid = 'cafebabe';
  var hostname = 'test.host.name';
  var handshakeUrl = contextPath + apiUrlPrefix + '/' + rootUuid + './composermode/' + hostname;

  beforeEach(function () {
    module('hippo-cm-api');

    ConfigServiceMock = {
      apiUrlPrefix: apiUrlPrefix,
      cmsUser: 'testUser',
      contextPath: contextPath,
      rootUuid: rootUuid,
    };

    module(function ($provide) {
      $provide.value('ConfigService', ConfigServiceMock);
    });

    inject(function (_$httpBackend_, _HstService_) {
      $httpBackend = _$httpBackend_;
      hstService = _HstService_;
    });
  });

  afterEach(function () {
    $httpBackend.verifyNoOutstandingRequest();
    $httpBackend.verifyNoOutstandingExpectation();
  });

  it('should exist', function () {
    expect(hstService).toBeDefined();
  });

  it('should prefix all API urls with the context path and api url prefix', function () {
    expect(hstService._createApiUrl('1234', ['somepath'])).toEqual('/testContextPath/testApiUrlPrefix/1234./somepath');
  });

  it('should trim concatenated path elements', function () {
    expect(hstService._createApiUrl('1234', ['  foo ', ' bar'])).toEqual('/testContextPath/testApiUrlPrefix/1234./foo/bar');
  });

  it('should remove clashing slashes from concatenated path elements', function () {
    expect(hstService._createApiUrl('1234', ['/foo/', '/bar'])).toEqual('/testContextPath/testApiUrlPrefix/1234./foo/bar');
  });

  it('should be able to create an APIURL with only a UUID', function () {
    expect(hstService._createApiUrl('1234', [])).toEqual('/testContextPath/testApiUrlPrefix/1234./');
  });

  it('should construct a valid handshake url when initializing a channel session', function () {
    $httpBackend.expectGET(handshakeUrl).respond(200);
    hstService.initializeSession(hostname);
    $httpBackend.flush();
  });

  it('should use the new context path after it has been changed', function () {
    $httpBackend.expectGET('/testContextPath/testApiUrlPrefix/1234./test').respond(200);
    hstService.doGet('1234', 'test');
    $httpBackend.flush();

    ConfigServiceMock.contextPath = '/newContextPath';

    $httpBackend.expectGET('/newContextPath/testApiUrlPrefix/1234./test').respond(200);
    hstService.doGet('1234', 'test');
    $httpBackend.flush();
  });

  describe('when initialization is successful', function () {
    it('should resolve a promise', function () {
      var promiseSpy = jasmine.createSpy('promiseSpy');
      $httpBackend.expectGET(handshakeUrl).respond(200);
      hstService.initializeSession(hostname).then(promiseSpy);
      $httpBackend.flush();
      expect(promiseSpy).toHaveBeenCalled();
    });

    it('should resolve with true if response data parameter canWrite is true', function () {
      var promiseSpy = jasmine.createSpy('promiseSpy');
      $httpBackend.expectGET(handshakeUrl).respond(200, { data: { canWrite: true } });
      hstService.initializeSession(hostname).then(promiseSpy);
      $httpBackend.flush();
      expect(promiseSpy).toHaveBeenCalledWith(true);
    });

    it('should resolve with false if response data parameter canWrite is false', function () {
      var promiseSpy = jasmine.createSpy('promiseSpy');
      $httpBackend.expectGET(handshakeUrl).respond(200, { data: { canWrite: false } });
      hstService.initializeSession(hostname).then(promiseSpy);
      $httpBackend.flush();
      expect(promiseSpy).toHaveBeenCalledWith(false);
    });

    it('should resolve with false if response data parameter is missing', function () {
      var promiseSpy = jasmine.createSpy('promiseSpy');
      $httpBackend.expectGET(handshakeUrl).respond(200);
      hstService.initializeSession(hostname).then(promiseSpy);
      $httpBackend.flush();
      expect(promiseSpy).toHaveBeenCalledWith(false);
    });
  });

  it('should reject a promise when initialization fails', function () {
    var catchSpy = jasmine.createSpy('catchSpy');
    $httpBackend.expectGET(handshakeUrl).respond(500);
    hstService
      .initializeSession(hostname)
      .catch(catchSpy);

    $httpBackend.flush();
    expect(catchSpy).toHaveBeenCalled();
  });

  it('should get a channel by id', function () {
    var channelA = {
      id: 'channelA',
      contextPath: '/a',
    };
    var url = contextPath + apiUrlPrefix + '/' + rootUuid + './channels/' + channelA.id;
    var catchSpy = jasmine.createSpy('catchSpy');

    $httpBackend.expectGET(url).respond(200, channelA);
    hstService.getChannel('channelA')
      .then(catchSpy);

    $httpBackend.flush();
    expect(catchSpy).toHaveBeenCalledWith(channelA);
  });

  it('should reject a promise when a channel load fails', function () {
    var catchSpy = jasmine.createSpy('catchSpy');
    var url = contextPath + apiUrlPrefix + '/' + rootUuid + './channels/test';
    $httpBackend.expectGET(url).respond(500);
    hstService.
      getChannel('test')
      .catch(catchSpy);

    $httpBackend.flush();
    expect(catchSpy).toHaveBeenCalled();
  });
});
