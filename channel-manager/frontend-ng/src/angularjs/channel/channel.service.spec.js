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
      workspaceExists: true,
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
      locale: 'en',
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
    spyOn(HstService, 'doGet').and.returnValue($q.when({ data: {} }));
    spyOn(HstService, 'doGetWithParams').and.returnValue($q.when({ data: {} }));
    spyOn(HstService, 'doPut');
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
    spyOn(ChannelService, '_loadGlobalFeatures');
    spyOn($state, 'go');

    ChannelService.initialize();
    expect(CmsService.subscribe).toHaveBeenCalledWith('load-channel', jasmine.any(Function));

    window.CMS_TO_APP.publish('load-channel', testChannel);

    expect(HstService.getChannel).toHaveBeenCalledWith(testChannel.id);
    expect(ChannelService._loadGlobalFeatures).toHaveBeenCalled();
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
    HstService.doGetWithParams.and.returnValue($q.when({ data: { prototypes: ['test'] } }));
    ChannelService._load(channelMock);
    expect(ChannelService.getChannel()).not.toEqual(channelMock);
    $rootScope.$digest();
    expect(ChannelService.getChannel()).toEqual(channelMock);
    expect(SiteMapService.load).toHaveBeenCalledWith('siteMapId');
    expect(HstService.doGetWithParams).toHaveBeenCalledWith(channelMock.mountId, undefined, 'newpagemodel');
    $rootScope.$digest();
    expect(ChannelService.hasPrototypes()).toBe(true);
    expect(ChannelService.hasWorkspace()).toBe(true);
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

  it('should return a url without the mountPath appended after the cmsPreviewPrefix', () => {
    ChannelService._load({
      contextPath: '/contextPath',
      cmsPreviewPrefix: 'cmsPreviewPrefix',
      mountPath: '/mountPath',
    });
    $rootScope.$digest();
    expect(ChannelService.makePath()).toEqual('/contextPath/cmsPreviewPrefix');
  });

  it('should append argument path to the url', () => {
    ChannelService._load({
      contextPath: '/contextPath',
      cmsPreviewPrefix: 'cmsPreviewPrefix',
    });
    $rootScope.$digest();
    expect(ChannelService.makePath('/mount/path')).toEqual('/contextPath/cmsPreviewPrefix/mount/path');
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

  it('should return the name of the current channel', () => {
    ChannelService._load({ name: 'test-name' });
    $rootScope.$digest();
    expect(ChannelService.getName()).toEqual('test-name');
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

  it('updates the current channel when told so by Ext', () => {
    ChannelService._load(channelMock);
    $rootScope.$digest();

    channelMock.changedBySet = ['anotherUser'];
    HstService.getChannel.and.returnValue($q.when(channelMock));
    window.CMS_TO_APP.publish('channel-changed-in-extjs');
    expect(HstService.getChannel).toHaveBeenCalledWith('channelId');
    $rootScope.$digest();

    expect(ChannelService.channel.changedBySet).toEqual(['anotherUser']);
  });

  it('prints console log when it is failed to update current channel as requested by Ext', () => {
    ChannelService._load(channelMock);
    $rootScope.$digest();

    channelMock.changedBySet = ['anotherUser'];
    HstService.getChannel.and.returnValue($q.reject());
    spyOn($log, 'error');

    window.CMS_TO_APP.publish('channel-changed-in-extjs');
    $rootScope.$digest();

    expect($log.error).toHaveBeenCalled();
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
      .toBe('/mou/nt/test/renderpa.th');
    expect(ChannelService.extractRenderPathInfo('/testContextPath/cmsPreviewPrefix'))
      .toBe('');
  });

  it('should extract the renderPathInfo given a channel with empty preview prefix and mount path', () => {
    ChannelService._load(channelMock);
    $rootScope.$digest();

    expect(ChannelService.extractRenderPathInfo('/testContextPath/test/render.path'))
      .toBe('/test/render.path');
    expect(ChannelService.extractRenderPathInfo('/testContextPath/')).toBe('');
  });

  it('uses the channel\'s mount path to generate the homepage renderPathInfo', () => {
    channelMock.mountPath = '/mou/nt';
    ChannelService._load(channelMock);
    $rootScope.$digest();
    expect(ChannelService.getHomePageRenderPathInfo()).toBe('/mou/nt');

    delete channelMock.mountPath;
    ChannelService._load(channelMock);
    $rootScope.$digest();
    expect(ChannelService.getHomePageRenderPathInfo()).toBe('');
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
    HstService.doGetWithParams.and.returnValue($q.when({ data: 'test' }));

    ChannelService.getNewPageModel().then((result) => {
      expect(result).toBe('test');
      done();
    });
    expect(HstService.doGetWithParams).toHaveBeenCalledWith(channelMock.mountId, undefined, 'newpagemodel');
    $rootScope.$digest();
  });

  it('should relay failure to retrieve the new page model from the HST service', (done) => {
    ChannelService._load(channelMock);
    $rootScope.$digest();
    HstService.doGetWithParams.and.returnValue($q.reject());

    ChannelService.getNewPageModel()
      .then(() => {
        fail();
      })
      .catch(() => {
        done();
      });
    $rootScope.$digest();
  });

  it('should ask the HST service to retrieve the channel settings description', (done) => {
    ChannelService._load(channelMock);
    $rootScope.$digest();
    const channelInfoDescription = { };
    HstService.doGetWithParams.and.returnValue($q.when(channelInfoDescription));

    ChannelService.getChannelInfoDescription()
      .then((response) => {
        expect(response).toBe(channelInfoDescription);
        done();
      })
      .catch(() => {
        fail();
      });
    expect(HstService.doGetWithParams)
      .toHaveBeenCalledWith('testRootUuid', { locale: 'en' }, 'channels', 'channelId', 'info');
    $rootScope.$digest();
  });

  it('should relay any error when retrieving the channel settings description', (done) => {
    ChannelService._load(channelMock);
    $rootScope.$digest();
    const error = { };
    HstService.doGetWithParams.and.returnValue($q.reject(error));

    ChannelService.getChannelInfoDescription()
      .then(() => {
        fail();
      })
      .catch((response) => {
        expect(response).toBe(error);
        done();
      });
    $rootScope.$digest();
  });

  it('should ask the HST service to save the channel settings', (done) => {
    ChannelService._load(channelMock);
    $rootScope.$digest();
    const properties = { };
    HstService.doPut.and.returnValue($q.when());

    ChannelService.saveProperties(properties)
      .then(() => {
        done();
      })
      .catch(() => {
        fail();
      });
    expect(HstService.doPut).toHaveBeenCalledWith(properties, 'testRootUuid', 'channels', 'channelId', 'properties');
    $rootScope.$digest();
  });

  it('should relay any error when saving the channel settings', (done) => {
    ChannelService._load(channelMock);
    $rootScope.$digest();
    const error = { };
    HstService.doPut.and.returnValue($q.reject(error));

    ChannelService.saveProperties({ })
      .then(() => {
        fail();
      })
      .catch((response) => {
        expect(response).toBe(error);
        done();
      });
    $rootScope.$digest();
  });

  it('retrieves the global features during initialization', () => {
    const features = {
      crossChannelPageCopySupported: true,
    };
    HstService.doGet.and.returnValue($q.when({ data: features }));
    ChannelService._loadGlobalFeatures();
    expect(HstService.doGet).toHaveBeenCalledWith('testRootUuid', 'features');
    $rootScope.$digest();
    expect(ChannelService.isCrossChannelPageCopySupported()).toBe(true);

    features.crossChannelPageCopySupported = false;
    ChannelService._loadGlobalFeatures();
    $rootScope.$digest();
    expect(ChannelService.isCrossChannelPageCopySupported()).toBe(false);

    HstService.doGet.and.returnValue($q.reject());
    ChannelService._loadGlobalFeatures();
    $rootScope.$digest();
    expect(ChannelService.isCrossChannelPageCopySupported()).toBeFalsy();
  });

  it('loads and caches the page modifiable channels', () => {
    const channels = ['a', 'b'];
    HstService.doGetWithParams.and.returnValue($q.when({ data: channels }));
    ChannelService.loadPageModifiableChannels();
    expect(HstService.doGetWithParams).toHaveBeenCalled();
    $rootScope.$digest();
    expect(ChannelService.getPageModifiableChannels()).toBe(channels);

    HstService.doGetWithParams.and.returnValue($q.when({ }));
    ChannelService.loadPageModifiableChannels();
    $rootScope.$digest();
    expect(ChannelService.getPageModifiableChannels()).toEqual([]);
  });

  it('retrieves the new page model for a different channel', () => {
    ChannelService._load(channelMock);
    $rootScope.$digest();
    ChannelService.getNewPageModel('other-mount');
    expect(HstService.doGetWithParams).toHaveBeenCalledWith('mountId', { mountId: 'other-mount' }, 'newpagemodel');
  });
});
