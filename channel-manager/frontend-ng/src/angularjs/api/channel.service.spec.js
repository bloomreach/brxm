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

describe('ChannelService', () => {
  'use strict';

  let $q;
  let $rootScope;
  let ChannelService;
  let CatalogServiceMock;
  let SessionServiceMock;
  let HstServiceMock;
  let channelMock;

  beforeEach(() => {
    module('hippo-cm');

    channelMock = {
      contextPath: '/testContextPath',
      hostname: 'test.host.name',
      mountId: 'mountId',
    };

    SessionServiceMock = {
      initialize: (channel) => $q.resolve(channel),
    };

    CatalogServiceMock = jasmine.createSpyObj('CatalogService', [
      'load',
      'getComponents',
    ]);

    const ConfigServiceMock = {
      apiUrlPrefix: '/testApiUrlPrefix',
      rootUuid: 'testRootUuid',
      cmsUser: 'testUser',
      contextPaths: ['/testContextPath1', '/'],
    };

    HstServiceMock = jasmine.createSpyObj('HstService', ['getChannel', 'doPost']);

    module(($provide) => {
      $provide.value('SessionService', SessionServiceMock);
      $provide.value('ConfigService', ConfigServiceMock);
      $provide.value('HstService', HstServiceMock);
      $provide.value('CatalogService', CatalogServiceMock);
    });

    inject((_$q_, _$rootScope_, _ChannelService_) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      ChannelService = _ChannelService_;
    });

    spyOn(window.APP_TO_CMS, 'publish');
  });

  it('should not save a reference to the channel when load fails', () => {
    spyOn(SessionServiceMock, 'initialize').and.callFake(() => $q.reject());
    ChannelService.load(channelMock);
    $rootScope.$digest();
    expect(ChannelService.getChannel()).not.toEqual(channelMock);
  });

  it('should save a reference to the channel when load succeeds', () => {
    ChannelService.load(channelMock);
    expect(ChannelService.getChannel()).not.toEqual(channelMock);
    $rootScope.$digest();
    expect(ChannelService.getChannel()).toEqual(channelMock);
  });

  it('should resolve a promise with the channel when load succeeds', () => {
    const promiseSpy = jasmine.createSpy('promiseSpy');
    ChannelService.load(channelMock).then(promiseSpy);
    $rootScope.$digest();
    expect(promiseSpy).toHaveBeenCalledWith(channelMock);
  });

  it('should ignore the contextPath if it is /', () => {
    const contextPath = '/';
    const cmsPreviewPrefix = 'cmsPreviewPrefix';

    ChannelService.load({ contextPath });
    $rootScope.$digest();
    expect(ChannelService.getPreviewPath(contextPath)).toEqual('');
    expect(ChannelService.getUrl()).toEqual('');

    ChannelService.load({ contextPath, cmsPreviewPrefix });
    $rootScope.$digest();
    expect(ChannelService.getPreviewPath(contextPath)).toEqual('/cmsPreviewPrefix');
    expect(ChannelService.getUrl()).toEqual('/cmsPreviewPrefix');
  });

  it('should return a preview path that starts with the contextPath', () => {
    const contextPath = '/contextPath';
    const cmsPreviewPrefix = 'cmsPreviewPrefix';

    ChannelService.load({ contextPath });
    $rootScope.$digest();
    expect(ChannelService.getPreviewPath(contextPath)).toEqual('/contextPath');

    ChannelService.load({ contextPath, cmsPreviewPrefix });
    $rootScope.$digest();
    expect(ChannelService.getPreviewPath(contextPath)).toEqual('/contextPath/cmsPreviewPrefix');
  });

  it('should return a url that ends with a slash if it equals the contextPath', () => {
    ChannelService.load({ contextPath: '/contextPath' });
    $rootScope.$digest();
    expect(ChannelService.getUrl()).toEqual('/contextPath/');
  });

  it('should return a url with the mountPath appended after the cmsPreviewPrefix', () => {
    ChannelService.load({
      contextPath: '/contextPath',
      cmsPreviewPrefix: 'cmsPreviewPrefix',
      mountPath: '/mountPath',
    });
    $rootScope.$digest();
    expect(ChannelService.getUrl()).toEqual('/contextPath/cmsPreviewPrefix/mountPath');
  });

  it('should append argument path to the url', () => {
    ChannelService.load({
      contextPath: '/contextPath',
      cmsPreviewPrefix: 'cmsPreviewPrefix',
      mountPath: '/mountPath',
    });
    $rootScope.$digest();
    expect(ChannelService.getUrl('/optional/path')).toEqual('/contextPath/cmsPreviewPrefix/mountPath/optional/path');
  });

  it('should compile a list of preview paths', () => {
    ChannelService.load({ cmsPreviewPrefix: 'cmsPreviewPrefix' });
    $rootScope.$digest();
    expect(ChannelService.getPreviewPaths()).toEqual(['/testContextPath1/cmsPreviewPrefix', '/cmsPreviewPrefix']);

    ChannelService.load({ cmsPreviewPrefix: '' });
    $rootScope.$digest();
    expect(ChannelService.getPreviewPaths()).toEqual(['/testContextPath1', '']);
  });

  it('should return the mountId of the current channel', () => {
    ChannelService.load({ id: 'test-id' });
    $rootScope.$digest();
    expect(ChannelService.getId()).toEqual('test-id');
  });

  it('should switch to a new channel', () => {
    const channelA = {
      id: 'channelA',
      contextPath: '/a',
    };
    const channelB = {
      id: 'channelB',
      contextPath: '/b',
    };

    ChannelService.load(channelA);
    $rootScope.$digest();

    HstServiceMock.getChannel.and.callFake(() => $q.resolve(channelB));
    ChannelService.switchToChannel(channelB.id);
    $rootScope.$digest();

    expect(ChannelService.getId()).toEqual(channelB.id);
    expect(ChannelService.getChannel()).toEqual(channelB);
  });

  // TODO: add a test where the server returns an error upon the ChannelService's request for channel details.

  it('should trigger loading of the channel\'s catalog', () => {
    ChannelService.load({ mountId: '1234' });
    $rootScope.$digest();
    expect(CatalogServiceMock.load).toHaveBeenCalledWith('1234');
  });

  it('should relay the request for catalog components to the CatalogService', () => {
    const mockCatalog = [
      { label: 'componentA' },
      { label: 'componentB' },
    ];
    CatalogServiceMock.getComponents.and.returnValue(mockCatalog);
    expect(ChannelService.getCatalog()).toEqual(mockCatalog);
  });

  it('should publish own changes', () => {
    channelMock.changedBySet = ['testUser'];
    ChannelService.load(channelMock);
    $rootScope.$digest();

    const deferred = $q.defer();
    HstServiceMock.doPost.and.returnValue(deferred.promise);
    ChannelService.publishOwnChanges().then((response) => {
      expect(response).toBe('pass-through');
    });

    deferred.resolve('pass-through');
    $rootScope.$digest();

    expect(HstServiceMock.doPost).toHaveBeenCalledWith(null, 'mountId', 'publish');
    expect(window.APP_TO_CMS.publish).toHaveBeenCalledWith('channel-changed-in-angular');
    expect(channelMock.changedBySet).toEqual([]);
  });

  it('should discard own changes', () => {
    channelMock.changedBySet = ['testUser'];
    ChannelService.load(channelMock);
    $rootScope.$digest();

    const deferred = $q.defer();
    HstServiceMock.doPost.and.returnValue(deferred.promise);
    ChannelService.discardOwnChanges().then((response) => {
      expect(response).toBe('pass-through');
    });

    deferred.resolve('pass-through');
    $rootScope.$digest();

    expect(HstServiceMock.doPost).toHaveBeenCalledWith(null, 'mountId', 'discard');
    expect(channelMock.changedBySet).toEqual([]);
    expect(window.APP_TO_CMS.publish).toHaveBeenCalledWith('channel-changed-in-angular');
  });

  it('records own changes', () => {
    channelMock.changedBySet = ['tobi', 'obiwan'];
    ChannelService.load(channelMock);
    $rootScope.$digest();

    ChannelService.recordOwnChange();

    expect(channelMock.changedBySet).toEqual(['tobi', 'obiwan', 'testUser']);
    expect(window.APP_TO_CMS.publish).toHaveBeenCalledWith('channel-changed-in-angular');
  });

  it('recognizes changes already pending', () => {
    channelMock.changedBySet = ['tobi', 'testUser', 'obiwan'];
    ChannelService.load(channelMock);
    $rootScope.$digest();

    ChannelService.recordOwnChange();

    expect(channelMock.changedBySet).toEqual(['tobi', 'testUser', 'obiwan']);
    expect(window.APP_TO_CMS.publish).toHaveBeenCalledWith('channel-changed-in-angular');
  });

  it('incorporates changes made in ExtJs', () => {
    channelMock.changedBySet = ['testUser'];
    ChannelService.load(channelMock);
    $rootScope.$digest();

    window.CMS_TO_APP.publish('channel-changed-in-extjs', {
      changedBySet: ['anotherUser'],
    });

    expect(ChannelService.channel.changedBySet).toEqual(['anotherUser']);
  });
});
