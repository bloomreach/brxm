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

describe('ChannelService', function () {
  'use strict';

  var $q;
  var $rootScope;
  var ChannelService;
  var SessionServiceMock;
  var channelMock;

  beforeEach(function () {
    module('hippo-cm');

    channelMock = {
      contextPath: '/testContextPath',
      hostname: 'test.host.name'
    };

    SessionServiceMock = {
      authenticate: function(channel) {
        return $q.resolve(channel);
      }
    };

    module(function ($provide) {
      $provide.value('SessionService', SessionServiceMock);
    });

    inject(function (_$q_, _$rootScope_, _ChannelService_) {
      $q = _$q_;
      $rootScope = _$rootScope_;
      ChannelService = _ChannelService_;
    });
  });

  it('should not save a reference to the channel when load fails', function() {
    spyOn(SessionServiceMock, 'authenticate').and.callFake(function() {
      return $q.reject();
    });
    ChannelService.load(channelMock);
    $rootScope.$apply();
    expect(ChannelService.channel).not.toEqual(channelMock);
  });

  it('should save a reference to the channel when load succeeds', function() {
    ChannelService.load(channelMock);
    expect(ChannelService.channel).not.toEqual(channelMock);
    $rootScope.$apply();
    expect(ChannelService.channel).toEqual(channelMock);
  });

  it('should resolve a promise with the channel when load succeeds', function() {
    var promiseSpy = jasmine.createSpy('promiseSpy');
    ChannelService.load(channelMock).then(promiseSpy);
    $rootScope.$apply();
    expect(promiseSpy).toHaveBeenCalledWith(channelMock);
  });

  it('should load a channel with an empty path if none is provided', function() {
    ChannelService.load(channelMock);
    $rootScope.$apply();
    expect(ChannelService.path).toEqual('');
  });

  it('should load a channel with an optional path', function() {
    ChannelService.load(channelMock, 'optional/path');
    $rootScope.$apply();
    expect(ChannelService.path).toEqual('optional/path');
  });

  it('should return a url that starts with the contextPath', function() {
    ChannelService.load({ contextPath: '/test' }, '/optional/path');
    $rootScope.$apply();
    expect(ChannelService.getUrl()).toEqual('/test/optional/path');
  });

  it('should return a url that ends with a slash if it equals the contextPath', function() {
    ChannelService.load({ contextPath: '/test' });
    $rootScope.$apply();
    expect(ChannelService.getUrl()).toEqual('/test/');
  });

  it('should return a url without the contextPath if it is root', function() {
    ChannelService.load({ contextPath: '/' })
    $rootScope.$apply();
    expect(ChannelService.getUrl()).toEqual('');
  });

  it('should return a url with the cmsPreviewPrefix appended after the contextPath with a slash', function() {
    ChannelService.load({ contextPath: '/test', cmsPreviewPrefix: 'cmsPreviewPrefix' });
    $rootScope.$apply();
    expect(ChannelService.getUrl()).toEqual('/test/cmsPreviewPrefix');
  });

  it('should return a url with the mountPath appended after the cmsPreviewPrefix', function() {
    ChannelService.load({ contextPath: '/test', cmsPreviewPrefix: 'cmsPreviewPrefix', mountPath: '/mountPath' });
    $rootScope.$apply();
    expect(ChannelService.getUrl()).toEqual('/test/cmsPreviewPrefix/mountPath');
  });

});
