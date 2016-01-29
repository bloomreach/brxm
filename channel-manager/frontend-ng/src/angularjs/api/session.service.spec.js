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

  var $httpBackend;
  var SessionService;
  var ConfigServiceMock;
  var channelMock;

  // built-in path to RootResource 'REST' endpoint
  var composerModePath = '/cafebabe-cafe-babe-cafe-babecafebabe./composermode/';
  var handshakeUrl = '/testContextPath/testApiUrlPrefix' + composerModePath + 'test.host.name/';

  beforeEach(function () {
    module('hippo-cm-api');

    channelMock = {
      contextPath: '/testContextPath',
      hostname: 'test.host.name',
    };

    ConfigServiceMock = {
      apiUrlPrefix: '/testApiUrlPrefix',
      cmsUser: 'testUser',
    };

    module(function ($provide) {
      $provide.value('ConfigService', ConfigServiceMock);
    });

    inject(function (_$httpBackend_, _SessionService_) {
      $httpBackend = _$httpBackend_;
      SessionService = _SessionService_;
    });
  });

  afterEach(function () {
    $httpBackend.verifyNoOutstandingRequest();
    $httpBackend.verifyNoOutstandingExpectation();
  });

  it('should exist', function () {
    expect(SessionService).toBeDefined();
  });

  it('should have a handshakePath', function () {
    expect(SessionService.handshakePath).toEqual(ConfigServiceMock.apiUrlPrefix + composerModePath);
  });

  it('should have a cms user', function () {
    expect(SessionService.cmsUser).toEqual(ConfigServiceMock.cmsUser);
  });

  it('should not have a session ID before authenticating', function () {
    expect(SessionService.sessionID).toEqual(null);
  });

  it('should always be readonly before authenticating', function () {
    expect(SessionService.canWrite).toEqual(false);
  });

  it('should construct a valid handshake url when authenticating', function () {
    $httpBackend.expectGET(handshakeUrl).respond(200);
    SessionService.authenticate(channelMock);
    $httpBackend.flush();
  });

  it('should resolve a promise with the channel as value after authenticating', function () {
    var promiseSpy = jasmine.createSpy('promiseSpy');
    $httpBackend.expectGET(handshakeUrl).respond(200);
    SessionService.authenticate(channelMock).then(promiseSpy);

    $httpBackend.flush();
    expect(promiseSpy).toHaveBeenCalledWith(channelMock);
  });

  it('should reject a promise when authentication fails', function () {
    var catchSpy = jasmine.createSpy('catchSpy');
    $httpBackend.expectGET(handshakeUrl).respond(500);
    SessionService
      .authenticate(channelMock)
      .catch(catchSpy);

    $httpBackend.flush();
    expect(catchSpy).toHaveBeenCalled();
  });

  it('should set canWrite and sessionId after authenticating', function () {
    $httpBackend.expectGET(handshakeUrl).respond(200, {
      data: {
        canWrite: true,
        sessionId: '1234',
      },
    });
    SessionService.authenticate(channelMock);
    $httpBackend.flush();

    expect(SessionService.canWrite).toEqual(true);
    expect(SessionService.sessionId).toEqual('1234');
  });

  it('should reset values for canWrite and sessionId when handling an invalid response', function () {
    SessionService.canWrite = true;
    SessionService.sessionId = '1234';

    $httpBackend.expectGET(handshakeUrl).respond(200);
    SessionService.authenticate(channelMock);
    $httpBackend.flush();

    expect(SessionService.canWrite).toEqual(false);
    expect(SessionService.sessionId).toEqual(null);
  });

});
