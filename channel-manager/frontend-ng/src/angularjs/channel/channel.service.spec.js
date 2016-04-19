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
  let $log;
  let $rootScope;
  let ChannelService;
  let SiteMapService;
  let CatalogServiceMock;
  let SessionServiceMock;
  let HstService;
  let channelMock;
  let CmsService;
  let $state;

  beforeEach(() => {
    module('hippo-cm');

    channelMock = {
      contextPath: '/testContextPath',
      hostname: 'test.host.name',
      mountId: 'mountId',
      id: 'channelId',
      siteMapId: 'siteMapId',
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

    module(($provide) => {
      $provide.value('SessionService', SessionServiceMock);
      $provide.value('ConfigService', ConfigServiceMock);
      $provide.value('CatalogService', CatalogServiceMock);
    });

    inject((_$q_, _$log_, _$state_, _$rootScope_, _ChannelService_, _CmsService_, _HstService_, _SiteMapService_) => {
      $q = _$q_;
      $log = _$log_;
      $state = _$state_;
      $rootScope = _$rootScope_;
      ChannelService = _ChannelService_;
      CmsService = _CmsService_;
      HstService = _HstService_;
      SiteMapService = _SiteMapService_;
    });

    spyOn(HstService, 'doPost');
    spyOn(HstService, 'doGet');
    spyOn(HstService, 'getChannel');
    spyOn(SiteMapService, 'load');
    spyOn(window.APP_TO_CMS, 'publish');
  });

  it('should initialize the channel', () => {
    const testChannel = {
      id: 'testChannelId',
      siteMapId: 'testSiteMapId',
    };

    spyOn(CmsService, 'subscribe');
    HstService.getChannel.and.returnValue($q.resolve(testChannel));
    spyOn(ChannelService, '_load').and.callThrough();
    spyOn($state, 'go');

    ChannelService.initialize();
    expect(CmsService.subscribe).toHaveBeenCalledWith('load-channel', jasmine.any(Function));

    window.CMS_TO_APP.publish('load-channel', testChannel);

    expect(HstService.getChannel).toHaveBeenCalledWith(testChannel.id);
    $rootScope.$digest();

    expect(ChannelService._load).toHaveBeenCalledWith(testChannel);
    $rootScope.$digest();

    expect($state.go).toHaveBeenCalledWith(
      'hippo-cm.channel',
      {
        channelId: testChannel.id,
      },
      {
        reload: true,
      }
    );
  });

  it('should not enter composer mode whan retrieval of the channel fails', () => {
    const testChannel = {
      id: 'testChannelId',
    };

    HstService.getChannel.and.returnValue($q.reject());
    spyOn(ChannelService, '_load').and.callThrough();

    ChannelService.initialize();
    expect(ChannelService._load).not.toHaveBeenCalled();

    window.CMS_TO_APP.publish('load-channel', testChannel);
    expect(HstService.getChannel).toHaveBeenCalledWith(testChannel.id);
    $rootScope.$digest(); // trigger the rejection from HstService.getChannel()

    expect(ChannelService._load).not.toHaveBeenCalled();
  });

  it('should not save a reference to the channel when load fails', () => {
    spyOn(SessionServiceMock, 'initialize').and.returnValue($q.reject());
    ChannelService._load(channelMock);
    $rootScope.$digest();
    expect(ChannelService.getChannel()).not.toEqual(channelMock);
  });

  it('should save a reference to the channel when load succeeds', () => {
    ChannelService._load(channelMock);
    expect(ChannelService.getChannel()).not.toEqual(channelMock);
    $rootScope.$digest();
    expect(ChannelService.getChannel()).toEqual(channelMock);
    expect(SiteMapService.load).toHaveBeenCalledWith('siteMapId');
  });

  it('should resolve a promise with the channel when load succeeds', () => {
    const promiseSpy = jasmine.createSpy('promiseSpy');
    ChannelService._load(channelMock).then(promiseSpy);
    $rootScope.$digest();
    expect(promiseSpy).toHaveBeenCalledWith(channelMock.id);
  });

  it('should ignore the contextPath if it is /', () => {
    const contextPath = '/';
    const cmsPreviewPrefix = 'cmsPreviewPrefix';

    ChannelService._load({ contextPath });
    $rootScope.$digest();
    expect(ChannelService.makeContextPrefix(contextPath)).toEqual('');
    expect(ChannelService.makePath()).toEqual('');

    ChannelService._load({ contextPath, cmsPreviewPrefix });
    $rootScope.$digest();
    expect(ChannelService.makeContextPrefix(contextPath)).toEqual('/cmsPreviewPrefix');
    expect(ChannelService.makePath()).toEqual('/cmsPreviewPrefix');
  });

  it('should return a preview path that starts with the contextPath', () => {
    const contextPath = '/contextPath';
    const cmsPreviewPrefix = 'cmsPreviewPrefix';

    ChannelService._load({ contextPath });
    $rootScope.$digest();
    expect(ChannelService.makeContextPrefix(contextPath)).toEqual('/contextPath');

    ChannelService._load({ contextPath, cmsPreviewPrefix });
    $rootScope.$digest();
    expect(ChannelService.makeContextPrefix(contextPath)).toEqual('/contextPath/cmsPreviewPrefix');
  });

  it('should return a url that ends with a slash if it equals the contextPath', () => {
    ChannelService._load({ contextPath: '/contextPath' });
    $rootScope.$digest();
    expect(ChannelService.makePath()).toEqual('/contextPath/');
  });

  it('should return a url with the mountPath appended after the cmsPreviewPrefix', () => {
    ChannelService._load({
      contextPath: '/contextPath',
      cmsPreviewPrefix: 'cmsPreviewPrefix',
      mountPath: '/mountPath',
    });
    $rootScope.$digest();
    expect(ChannelService.makePath()).toEqual('/contextPath/cmsPreviewPrefix/mountPath');
  });

  it('should append argument path to the url', () => {
    ChannelService._load({
      contextPath: '/contextPath',
      cmsPreviewPrefix: 'cmsPreviewPrefix',
      mountPath: '/mountPath',
    });
    $rootScope.$digest();
    expect(ChannelService.makePath('/optional/path')).toEqual('/contextPath/cmsPreviewPrefix/mountPath/optional/path');
  });

  it('should compile a list of preview paths', () => {
    ChannelService._load({ cmsPreviewPrefix: 'cmsPreviewPrefix' });
    $rootScope.$digest();
    expect(ChannelService.getPreviewPaths()).toEqual(['/testContextPath1/cmsPreviewPrefix', '/cmsPreviewPrefix']);

    ChannelService._load({ cmsPreviewPrefix: '' });
    $rootScope.$digest();
    expect(ChannelService.getPreviewPaths()).toEqual(['/testContextPath1', '']);
  });

  it('should return the mountId of the current channel', () => {
    ChannelService._load({ id: 'test-id' });
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

    ChannelService._load(channelA);
    $rootScope.$digest();

    HstService.getChannel.and.callFake(() => $q.resolve(channelB));
    ChannelService.switchToChannel(channelB.id);
    $rootScope.$digest();

    expect(ChannelService.getId()).toEqual(channelB.id);
    expect(ChannelService.getChannel()).toEqual(channelB);
  });

  // TODO: add a test where the server returns an error upon the ChannelService's request for channel details.

  it('should trigger loading of the channel\'s catalog', () => {
    ChannelService._load({ mountId: '1234' });
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
    ChannelService._load(channelMock);
    $rootScope.$digest();

    const deferred = $q.defer();
    HstService.doPost.and.returnValue(deferred.promise);
    ChannelService.publishOwnChanges().then((response) => {
      expect(response).toBe('pass-through');
    });

    deferred.resolve('pass-through');
    $rootScope.$digest();

    expect(HstService.doPost).toHaveBeenCalledWith(null, 'mountId', 'publish');
    expect(window.APP_TO_CMS.publish).toHaveBeenCalledWith('channel-changed-in-angular');
    expect(channelMock.changedBySet).toEqual([]);
  });

  it('should discard own changes', () => {
    channelMock.changedBySet = ['testUser'];
    ChannelService._load(channelMock);
    $rootScope.$digest();

    const deferred = $q.defer();
    HstService.doPost.and.returnValue(deferred.promise);
    ChannelService.discardOwnChanges().then((response) => {
      expect(response).toBe('pass-through');
    });

    deferred.resolve('pass-through');
    $rootScope.$digest();

    expect(HstService.doPost).toHaveBeenCalledWith(null, 'mountId', 'discard');
    expect(channelMock.changedBySet).toEqual([]);
    expect(window.APP_TO_CMS.publish).toHaveBeenCalledWith('channel-changed-in-angular');
  });

  it('records own changes', () => {
    channelMock.changedBySet = ['tobi', 'obiwan'];
    ChannelService._load(channelMock);
    $rootScope.$digest();

    ChannelService.recordOwnChange();

    expect(channelMock.changedBySet).toEqual(['tobi', 'obiwan', 'testUser']);
    expect(window.APP_TO_CMS.publish).toHaveBeenCalledWith('channel-changed-in-angular');
  });

  it('recognizes changes already pending', () => {
    channelMock.changedBySet = ['tobi', 'testUser', 'obiwan'];
    ChannelService._load(channelMock);
    $rootScope.$digest();

    ChannelService.recordOwnChange();

    expect(channelMock.changedBySet).toEqual(['tobi', 'testUser', 'obiwan']);
    expect(window.APP_TO_CMS.publish).toHaveBeenCalledWith('channel-changed-in-angular');
  });

  it('incorporates changes made in ExtJs', () => {
    channelMock.changedBySet = ['testUser'];
    ChannelService._load(channelMock);
    $rootScope.$digest();

    window.CMS_TO_APP.publish('channel-changed-in-extjs', {
      changedBySet: ['anotherUser'],
    });

    expect(ChannelService.channel.changedBySet).toEqual(['anotherUser']);
  });

  it('should update the channel\'s preview config flag after successfully creating the preview config', () => {
    channelMock.previewHstConfigExists = false;
    ChannelService._load(channelMock);
    $rootScope.$digest();

    expect(ChannelService.hasPreviewConfiguration()).toBe(false);

    HstService.doPost.calls.reset();
    HstService.doPost.and.returnValue($q.when());

    ChannelService.createPreviewConfiguration();
    expect(HstService.doPost).toHaveBeenCalledWith(null, 'mountId', 'edit');

    $rootScope.$digest();
    expect(ChannelService.hasPreviewConfiguration()).toBe(true);
  });

  it('should not update the channel\'s preview config flag if creating a preview config failed', () => {
    channelMock.previewHstConfigExists = false;
    ChannelService._load(channelMock);
    $rootScope.$digest();

    HstService.doPost.and.returnValue($q.reject());

    ChannelService.createPreviewConfiguration();
    $rootScope.$digest();

    expect(ChannelService.hasPreviewConfiguration()).toBe(false);
  });

  it('should extract the renderPathInfo given a channel with non-empty preview prefix and mount path', () => {
    channelMock.cmsPreviewPrefix = 'cmsPreviewPrefix';
    channelMock.mountPath = '/mou/nt';
    ChannelService._load(channelMock);
    $rootScope.$digest();

    expect(ChannelService.extractRenderPathInfo('/testContextPath/cmsPreviewPrefix/mou/nt/test/renderpa.th/'))
      .toBe('/test/renderpa.th');
    expect(ChannelService.extractRenderPathInfo('/testContextPath/cmsPreviewPrefix/mou/nt'))
      .toBe('');
  });

  it('should extract the renderPathInfo given a channel with empty preview prefix and mount path', () => {
    ChannelService._load(channelMock);
    $rootScope.$digest();

    expect(ChannelService.extractRenderPathInfo('/testContextPath/test/render.path'))
      .toBe('/test/render.path');
    expect(ChannelService.extractRenderPathInfo('/testContextPath/')).toBe('');
  });

  it('should log a warning trying to extract a renderPathInfo if there is no matching channel prefix', () => {
    ChannelService._load(channelMock);
    $rootScope.$digest();
    spyOn($log, 'warn');
    const nonMatchingPrefix = '/testContexxxtPath/test/render.path';

    expect(ChannelService.extractRenderPathInfo(nonMatchingPrefix))
      .toBe(nonMatchingPrefix);
    expect($log.warn).toHaveBeenCalled();
  });

  it('should return the channel\'s siteMap ID', () => {
    ChannelService._load(channelMock);
    $rootScope.$digest();
    expect(ChannelService.getSiteMapId()).toBe('siteMapId');
  });

  it('should retrieve the new page model from the HST service', (done) => {
    ChannelService._load(channelMock);
    $rootScope.$digest();
    HstService.doGet.and.returnValue($q.when({ data: 'test' }));

    ChannelService.getNewPageModel().then((result) => {
      expect(result).toBe('test');
      done();
    });
    expect(HstService.doGet).toHaveBeenCalledWith(channelMock.mountId, 'newpagemodel');
    $rootScope.$digest();
  });

  it('should relay failure to retrieve the new page model from the HST service', (done) => {
    ChannelService._load(channelMock);
    $rootScope.$digest();
    HstService.doGet.and.returnValue($q.reject());

    ChannelService.getNewPageModel()
      .then(() => {
        fail();
      })
      .catch(() => {
        done();
      });
    $rootScope.$digest();
  });
});
