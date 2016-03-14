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
  var mountId = '1234';
  var handshakeUrl = contextPath + apiUrlPrefix + '/' + rootUuid + './composermode/' + hostname + '/' + mountId;

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

  it('exists', function () {
    expect(hstService).toBeDefined();
  });

  it('prefixes all API urls with the context path and api url prefix', function () {
    expect(hstService._createApiUrl('1234', ['somepath'])).toEqual('/testContextPath/testApiUrlPrefix/1234./somepath');
  });

  it('trims concatenated path elements', function () {
    expect(hstService._createApiUrl('1234', ['  foo ', ' bar'])).toEqual('/testContextPath/testApiUrlPrefix/1234./foo/bar');
  });

  it('removes clashing slashes from concatenated path elements', function () {
    expect(hstService._createApiUrl('1234', ['/foo/', '/bar'])).toEqual('/testContextPath/testApiUrlPrefix/1234./foo/bar');
  });

  it('can create an API URL with only a UUID', function () {
    expect(hstService._createApiUrl('1234', [])).toEqual('/testContextPath/testApiUrlPrefix/1234./');
  });

  it('can do a GET call', function () {
    $httpBackend.expectGET(contextPath + apiUrlPrefix + '/some-uuid./one/two/three', {
      'CMS-User': 'testUser',
      FORCE_CLIENT_HOST: 'true',
      Accept: 'application/json, text/plain, */*',
    }).respond(200);
    hstService.doGet('some-uuid', 'one', 'two', 'three').catch(fail);
    $httpBackend.flush();
  });

  it('returns a rejected promise when a GET call fails', function () {
    $httpBackend.expectGET(contextPath + apiUrlPrefix + '/some-uuid./one/two/three').respond(500);
    hstService.doGet('some-uuid', 'one', 'two', 'three').then(fail);
    $httpBackend.flush();
  });

  it('can do a POST call', function () {
    $httpBackend.expectPOST(contextPath + apiUrlPrefix + '/some-uuid./one/two/three', { foo: 1 }, {
      'CMS-User': 'testUser',
      FORCE_CLIENT_HOST: 'true',
      Accept: 'application/json, text/plain, */*',
      'Content-Type': 'application/json;charset=utf-8',
    }).respond(200);
    hstService.doPost({ foo: 1 }, 'some-uuid', 'one', 'two', 'three').catch(fail);
    $httpBackend.flush();
  });

  it('can do a POST call without data', function () {
    $httpBackend.expectPOST(contextPath + apiUrlPrefix + '/some-uuid./one/two/three', null, {
      'CMS-User': 'testUser',
      FORCE_CLIENT_HOST: 'true',
      Accept: 'application/json, text/plain, */*',
      'Content-Type': 'application/json;charset=utf-8',
    }).respond(200);
    hstService.doPost(null, 'some-uuid', 'one', 'two', 'three').catch(fail);
    $httpBackend.flush();
  });

  it('returns a rejected promise when a POST call fails', function () {
    $httpBackend.expectPOST(contextPath + apiUrlPrefix + '/some-uuid./one/two/three', { foo: 1 }).respond(500);
    hstService.doPost({ foo: 1 }, 'some-uuid', 'one', 'two', 'three').then(fail);
    $httpBackend.flush();
  });

  it('constructs a valid handshake url when initializing a channel session', function () {
    $httpBackend.expectGET(handshakeUrl).respond(200);
    hstService.initializeSession(hostname, mountId);
    $httpBackend.flush();
  });

  it('uses the new context path after it has been changed', function () {
    $httpBackend.expectGET('/testContextPath/testApiUrlPrefix/1234./test').respond(200);
    hstService.doGet('1234', 'test');
    $httpBackend.flush();

    ConfigServiceMock.contextPath = '/newContextPath';

    $httpBackend.expectGET('/newContextPath/testApiUrlPrefix/1234./test').respond(200);
    hstService.doGet('1234', 'test');
    $httpBackend.flush();
  });

  describe('when initialization is successful', function () {
    it('resolves a promise', function () {
      var promiseSpy = jasmine.createSpy('promiseSpy');
      $httpBackend.expectGET(handshakeUrl).respond(200);
      hstService.initializeSession(hostname, mountId).then(promiseSpy);
      $httpBackend.flush();
      expect(promiseSpy).toHaveBeenCalled();
    });

    it('resolves with true if response data parameter canWrite is true', function () {
      var promiseSpy = jasmine.createSpy('promiseSpy');
      $httpBackend.expectGET(handshakeUrl).respond(200, { data: { canWrite: true } });
      hstService.initializeSession(hostname, mountId).then(promiseSpy);
      $httpBackend.flush();
      expect(promiseSpy).toHaveBeenCalledWith(true);
    });

    it('resolves with false if response data parameter canWrite is false', function () {
      var promiseSpy = jasmine.createSpy('promiseSpy');
      $httpBackend.expectGET(handshakeUrl).respond(200, { data: { canWrite: false } });
      hstService.initializeSession(hostname, mountId).then(promiseSpy);
      $httpBackend.flush();
      expect(promiseSpy).toHaveBeenCalledWith(false);
    });

    it('resolves with false if response data parameter is missing', function () {
      var promiseSpy = jasmine.createSpy('promiseSpy');
      $httpBackend.expectGET(handshakeUrl).respond(200);
      hstService.initializeSession(hostname, mountId).then(promiseSpy);
      $httpBackend.flush();
      expect(promiseSpy).toHaveBeenCalledWith(false);
    });
  });

  it('rejects a promise when initialization fails', function () {
    var catchSpy = jasmine.createSpy('catchSpy');
    $httpBackend.expectGET(handshakeUrl).respond(500);
    hstService
      .initializeSession(hostname, mountId)
      .catch(catchSpy);

    $httpBackend.flush();
    expect(catchSpy).toHaveBeenCalled();
  });

  it('gets a channel by id', function () {
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

  it('rejects a promise when a channel load fails', function () {
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
