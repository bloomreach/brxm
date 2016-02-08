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

describe('ChannelService', function () {
  'use strict';

  var $q;
  var $rootScope;
  var $httpBackend;
  var ChannelService;
  var SessionServiceMock;
  var ConfigServiceMock;
  var channelMock;

  beforeEach(function () {
    module('hippo-cm');

    channelMock = {
      contextPath: '/testContextPath',
      hostname: 'test.host.name',
    };

    SessionServiceMock = {
      authenticate: function (channel) {
        return $q.resolve(channel);
      },
    };

    ConfigServiceMock = {
      apiUrlPrefix: '/testApiUrlPrefix',
      rootResource: '/testRootResource',
    };

    module(function ($provide) {
      $provide.value('SessionService', SessionServiceMock);
      $provide.value('ConfigService', ConfigServiceMock);
    });

    inject(function (_$q_, _$rootScope_, _$httpBackend_, _ChannelService_) {
      $q = _$q_;
      $rootScope = _$rootScope_;
      $httpBackend = _$httpBackend_;
      ChannelService = _ChannelService_;
    });
  });

  afterEach(function () {
    $httpBackend.verifyNoOutstandingRequest();
    $httpBackend.verifyNoOutstandingExpectation();
  });

  it('should not save a reference to the channel when load fails', function () {
    spyOn(SessionServiceMock, 'authenticate').and.callFake(function () {
      return $q.reject();
    });
    ChannelService.load(channelMock);
    $rootScope.$apply();
    expect(ChannelService.channel).not.toEqual(channelMock);
  });

  it('should save a reference to the channel when load succeeds', function () {
    ChannelService.load(channelMock);
    expect(ChannelService.channel).not.toEqual(channelMock);
    $rootScope.$apply();
    expect(ChannelService.channel).toEqual(channelMock);
  });

  it('should resolve a promise with the channel when load succeeds', function () {
    var promiseSpy = jasmine.createSpy('promiseSpy');
    ChannelService.load(channelMock).then(promiseSpy);
    $rootScope.$apply();
    expect(promiseSpy).toHaveBeenCalledWith(channelMock);
  });

  it('should return a url that starts with the contextPath', function () {
    ChannelService.load({ contextPath: '/test' });
    $rootScope.$apply();
    expect(ChannelService.getUrlForPath('/optional/path')).toEqual('/test/optional/path');
  });

  it('should return a url that ends with a slash if it equals the contextPath', function () {
    ChannelService.load({ contextPath: '/test' });
    $rootScope.$apply();
    expect(ChannelService.getUrlForPath('')).toEqual('/test/');
  });

  it('should return a url without the contextPath if it is root', function () {
    ChannelService.load({ contextPath: '/' });
    $rootScope.$apply();
    expect(ChannelService.getUrlForPath('')).toEqual('');
  });

  it('should return a url with the cmsPreviewPrefix appended after the contextPath with a slash', function () {
    ChannelService.load({ contextPath: '/test', cmsPreviewPrefix: 'cmsPreviewPrefix' });
    $rootScope.$apply();
    expect(ChannelService.getUrlForPath('')).toEqual('/test/cmsPreviewPrefix');
  });

  it('should return a url with the mountPath appended after the cmsPreviewPrefix', function () {
    ChannelService.load({ contextPath: '/test', cmsPreviewPrefix: 'cmsPreviewPrefix', mountPath: '/mountPath' });
    $rootScope.$apply();
    expect(ChannelService.getUrlForPath('')).toEqual('/test/cmsPreviewPrefix/mountPath');
  });

  it('should return the mountId of the current channel', function () {
    ChannelService.load({ id: 'test-id' });
    $rootScope.$apply();
    expect(ChannelService.getId()).toEqual('test-id');
  });

  it('should request the details of a new channel', function () {
    var id = 'test-id';
    var contextPath = '/test';
    var url = contextPath + ConfigServiceMock.apiUrlPrefix + ConfigServiceMock.rootResource + '/channels/' + id;

    // set the ChannelService's state (to validate the requested URL)
    ChannelService.load({ contextPath: contextPath });
    $rootScope.$apply();

    $httpBackend.expectGET(url).respond(200, {
      id: 'backend-id',
    });
    ChannelService.switchToChannel(id);
    $httpBackend.flush();

    expect(ChannelService.getId()).toEqual('backend-id');
  });

  // TODO: add a test where the server returns an error upon the ChannelService's request for channel details.

});
