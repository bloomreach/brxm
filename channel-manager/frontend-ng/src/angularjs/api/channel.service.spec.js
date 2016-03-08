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
  var ChannelService;
  var CatalogServiceMock;
  var SessionServiceMock;
  var ConfigServiceMock;
  var HstServiceMock;
  var channelMock;

  beforeEach(function () {
    module('hippo-cm');

    channelMock = {
      contextPath: '/testContextPath',
      hostname: 'test.host.name',
    };

    SessionServiceMock = {
      initialize: function (channel) {
        return $q.resolve(channel);
      },
    };

    CatalogServiceMock = jasmine.createSpyObj('CatalogService', [
      'load',
      'getComponents',
    ]);

    ConfigServiceMock = jasmine.createSpyObj('ConfigService', ['setContextPath']);
    ConfigServiceMock.apiUrlPrefix = '/testApiUrlPrefix';
    ConfigServiceMock.rootUuid = 'testRootUuid';

    HstServiceMock = jasmine.createSpyObj('HstService', ['setContextPath', 'getChannel']);

    module(function ($provide) {
      $provide.value('SessionService', SessionServiceMock);
      $provide.value('ConfigService', ConfigServiceMock);
      $provide.value('HstService', HstServiceMock);
      $provide.value('CatalogService', CatalogServiceMock);
    });

    inject(function (_$q_, _$rootScope_, _ChannelService_) {
      $q = _$q_;
      $rootScope = _$rootScope_;
      ChannelService = _ChannelService_;
    });
  });

  it('should not save a reference to the channel when load fails', function () {
    spyOn(SessionServiceMock, 'initialize').and.callFake(function () {
      return $q.reject();
    });
    ChannelService.load(channelMock);
    $rootScope.$digest();
    expect(ChannelService.getChannel()).not.toEqual(channelMock);
  });

  it('should save a reference to the channel when load succeeds', function () {
    ChannelService.load(channelMock);
    expect(ChannelService.getChannel()).not.toEqual(channelMock);
    $rootScope.$digest();
    expect(ChannelService.getChannel()).toEqual(channelMock);
  });

  it('should resolve a promise with the channel when load succeeds', function () {
    var promiseSpy = jasmine.createSpy('promiseSpy');
    ChannelService.load(channelMock).then(promiseSpy);
    $rootScope.$digest();
    expect(promiseSpy).toHaveBeenCalledWith(channelMock);
  });

  it('should return a url that starts with the contextPath', function () {
    ChannelService.load({ contextPath: '/test' });
    $rootScope.$digest();
    expect(ChannelService.getUrl('/optional/path')).toEqual('/test/optional/path');
  });

  it('should return a url that ends with a slash if it equals the contextPath', function () {
    ChannelService.load({ contextPath: '/test' });
    $rootScope.$digest();
    expect(ChannelService.getUrl()).toEqual('/test/');
  });

  it('should return a url without the contextPath if it is root', function () {
    ChannelService.load({ contextPath: '/' });
    $rootScope.$digest();
    expect(ChannelService.getUrl()).toEqual('');
  });

  it('should return a url with the cmsPreviewPrefix appended after the contextPath with a slash', function () {
    ChannelService.load({ contextPath: '/test', cmsPreviewPrefix: 'cmsPreviewPrefix' });
    $rootScope.$digest();
    expect(ChannelService.getUrl()).toEqual('/test/cmsPreviewPrefix');
  });

  it('should return a url with the mountPath appended after the cmsPreviewPrefix', function () {
    ChannelService.load({ contextPath: '/test', cmsPreviewPrefix: 'cmsPreviewPrefix', mountPath: '/mountPath' });
    $rootScope.$digest();
    expect(ChannelService.getUrl()).toEqual('/test/cmsPreviewPrefix/mountPath');
  });

  it('should return the mountId of the current channel', function () {
    ChannelService.load({ id: 'test-id' });
    $rootScope.$digest();
    expect(ChannelService.getId()).toEqual('test-id');
  });

  it('should update the ConfigService\'s context path when the channel reference is updated', function () {
    var channelA = {
      id: 'channelA',
      contextPath: '/a',
    };
    var channelB = {
      id: 'channelB',
      contextPath: '/b',
    };

    ChannelService.load(channelA);
    $rootScope.$digest();
    expect(ConfigServiceMock.setContextPath).toHaveBeenCalledWith(channelA.contextPath);

    ChannelService.load(channelB);
    $rootScope.$digest();
    expect(ConfigServiceMock.setContextPath).toHaveBeenCalledWith(channelB.contextPath);
  });

  it('should switch to a new channel', function () {
    var channelA = {
      id: 'channelA',
      contextPath: '/a',
    };
    var channelB = {
      id: 'channelB',
      contextPath: '/b',
    };

    ChannelService.load(channelA);
    $rootScope.$digest();

    HstServiceMock.getChannel.and.callFake(function () {
      return $q.resolve(channelB);
    });
    ChannelService.switchToChannel(channelB.id);
    $rootScope.$digest();

    expect(ChannelService.getId()).toEqual(channelB.id);
    expect(ChannelService.getChannel()).toEqual(channelB);
  });
  // TODO: add a test where the server returns an error upon the ChannelService's request for channel details.

  it('should trigger loading of the channel\'s catalog', function () {
    ChannelService.load({ mountId: '1234' });
    $rootScope.$digest();
    expect(CatalogServiceMock.load).toHaveBeenCalledWith('1234');
  });

  it('should relay the request for catalog components to the CatalogService', function () {
    var mockCatalog = [
      { label: 'componentA' },
      { label: 'componentB' },
    ];
    CatalogServiceMock.getComponents.and.returnValue(mockCatalog);
    expect(ChannelService.getCatalog()).toEqual(mockCatalog);
  });

});
