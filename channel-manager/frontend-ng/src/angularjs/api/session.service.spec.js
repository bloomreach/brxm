/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
  var sessionService;
  var configServiceMock;
  var channelMock;

  // built-in path to RootResource 'REST' endpoint
  var composerModePath = '/cafebabe-cafe-babe-cafe-babecafebabe./composermode/';
  var handshakeUrl = '/testContextPath/testApiUrlPrefix' + composerModePath + 'test.host.name/';

  beforeEach(function () {
    module('hippo-cm-api');

    channelMock = {
      contextPath: '/testContextPath',
      hostname: 'test.host.name'
    };

    configServiceMock = {
      apiUrlPrefix: '/testApiUrlPrefix',
      cmsUser: 'testUser'
    };

    module(function ($provide) {
      $provide.value('ConfigService', configServiceMock);
    });
  });

  beforeEach(inject([
    '$httpBackend', 'SessionService', function (httpBackend, SessionService) {
      $httpBackend = httpBackend;
      sessionService = SessionService;
    }
  ]));

  afterEach(function () {
    $httpBackend.verifyNoOutstandingRequest();
    $httpBackend.verifyNoOutstandingExpectation();
  });

  it('should exist', function () {
    expect(sessionService).toBeDefined();
  });

  it('should have a handshakePath', function () {
    expect(sessionService.handshakePath).toEqual(configServiceMock.apiUrlPrefix + composerModePath);
  });

  it('should have a cms user', function () {
    expect(sessionService.cmsUser).toEqual(configServiceMock.cmsUser);
  });

  it('should not have a session ID before authenticating', function() {
    expect(sessionService.sessionID).toEqual(null);
  });

  it('should always be readonly before authenticating', function() {
    expect(sessionService.canWrite).toEqual(false);
  });

  it('should construct a valid handshake url when authenticating', function() {
    $httpBackend.expectGET(handshakeUrl).respond(200);
    sessionService.authenticate(channelMock);
    $httpBackend.flush();
  });

  it('should resolve a promise with the channel as value after authenticating', function(done) {
    $httpBackend.expectGET(handshakeUrl).respond(200);
    sessionService
      .authenticate(channelMock)
      .then(function(channel) {
        expect(channel).toEqual(channelMock);
        done();
      });

    $httpBackend.flush();
  });

  it('should reject a promise when authentication fails', function(done) {
    $httpBackend.expectGET(handshakeUrl).respond(500);
    sessionService
      .authenticate(channelMock)
      .catch(done);

    $httpBackend.flush();
  });

  it('should set canWrite and sessionId after authenticating', function(done) {
    var handshakeResponse = {
      data: {
        canWrite: true,
        sessionId: '1234'
      }
    };
    $httpBackend.expectGET(handshakeUrl).respond(200, handshakeResponse);
    sessionService
      .authenticate(channelMock)
      .then(function () {
          expect(sessionService.canWrite).toEqual(true);
          expect(sessionService.sessionId).toEqual('1234');
          done();
        });

    $httpBackend.flush();
  });

  it('should reset values for canWrite and sessionId when handling an invalid response', function(done) {

    sessionService.canWrite = true;
    sessionService.sessionId = '1234';

    $httpBackend.expectGET(handshakeUrl).respond(200);
    sessionService
      .authenticate(channelMock)
      .then(function() {
        expect(sessionService.canWrite).toEqual(false);
        expect(sessionService.sessionId).toEqual(null);
        done();
      });

    $httpBackend.flush();
  });

});
