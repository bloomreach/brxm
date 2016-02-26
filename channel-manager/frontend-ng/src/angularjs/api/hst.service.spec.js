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
  var channelMock;

  var rootResource = '/cafebabe.';
  var apiUrlPrefix = '/testApiUrlPrefix';
  var composerMode = '/composermode';
  var contextPath = '/testContextPath';
  var hostname = 'test.host.name';
  var handshakeUrl = contextPath + apiUrlPrefix + rootResource + composerMode + '/' + hostname + '/';

  beforeEach(function () {
    module('hippo-cm-api');

    channelMock = {
      contextPath: contextPath,
      hostname: hostname,
    };

    ConfigServiceMock = {
      defaultContextPath: contextPath,
      apiUrlPrefix: apiUrlPrefix,
      cmsUser: 'testUser',
      rootResource: rootResource,
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

  it('should have a default contextPath', function () {
    expect(hstService.contextPath).toEqual(ConfigServiceMock.defaultContextPath);
  });

  it('should construct a valid handshake url when initializing a channel session', function () {
    $httpBackend.expectGET(handshakeUrl).respond(200);
    hstService.initializeSession(channelMock);
    $httpBackend.flush();
  });

  it('should use the new contextPath after is has been updated', function () {
    hstService.contextPath = '/newSite';
    expect(hstService.apiPath).toEqual('/newSite' + apiUrlPrefix + rootResource);
  });

  describe('when initialization is successful', function () {
    it('should resolve a promise', function () {
      var promiseSpy = jasmine.createSpy('promiseSpy');
      $httpBackend.expectGET(handshakeUrl).respond(200);
      hstService.initializeSession(channelMock).then(promiseSpy);
      $httpBackend.flush();
      expect(promiseSpy).toHaveBeenCalled();
    });

    it('should resolve with true if response data parameter canWrite is true', function () {
      var promiseSpy = jasmine.createSpy('promiseSpy');
      $httpBackend.expectGET(handshakeUrl).respond(200, { data: { canWrite: true } });
      hstService.initializeSession(channelMock).then(promiseSpy);
      $httpBackend.flush();
      expect(promiseSpy).toHaveBeenCalledWith(true);
    });

    it('should resolve with false if response data parameter canWrite is false', function () {
      var promiseSpy = jasmine.createSpy('promiseSpy');
      $httpBackend.expectGET(handshakeUrl).respond(200, { data: { canWrite: false } });
      hstService.initializeSession(channelMock).then(promiseSpy);
      $httpBackend.flush();
      expect(promiseSpy).toHaveBeenCalledWith(false);
    });

    it('should resolve with false if response data parameter is missing', function () {
      var promiseSpy = jasmine.createSpy('promiseSpy');
      $httpBackend.expectGET(handshakeUrl).respond(200);
      hstService.initializeSession(channelMock).then(promiseSpy);
      $httpBackend.flush();
      expect(promiseSpy).toHaveBeenCalledWith(false);
    });
  });

  it('should reject a promise when initialization fails', function () {
    var catchSpy = jasmine.createSpy('catchSpy');
    $httpBackend.expectGET(handshakeUrl).respond(500);
    hstService
      .initializeSession(channelMock)
      .catch(catchSpy);

    $httpBackend.flush();
    expect(catchSpy).toHaveBeenCalled();
  });
});
