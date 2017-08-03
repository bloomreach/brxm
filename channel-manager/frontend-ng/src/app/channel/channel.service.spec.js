/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
  let $log;
  let $q;
  let $rootScope;
  let $state;
  let CatalogService;
  let ChannelService;
  let CmsService;
  let ConfigServiceMock;
  let FeedbackService;
  let HstService;
  // let PathService;
  let SessionService;
  let SiteMapService;
  let channelMock;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    channelMock = {
      contextPath: '/testContextPath',
      hostname: 'test.host.name',
      mountId: 'mountId',
      id: 'channelId',
      previewHstConfigExists: true,
      siteMapId: 'siteMapId',
      workspaceExists: true,
    };

    ConfigServiceMock = {
      apiUrlPrefix: '/testApiUrlPrefix',
      rootUuid: 'testRootUuid',
      cmsUser: 'testUser',
      contextPaths: ['/testContextPath1', '/'],
      locale: 'en',
      projectsEnabled: false,
    };

    ConfigServiceMock.setContextPathForChannel = jasmine.createSpy('setContextPathForChannel');

    angular.mock.module(($provide) => {
      $provide.value('ConfigService', ConfigServiceMock);
    });

    inject((
      _$log_,
      _$q_,
      _$rootScope_,
      _$state_,
      _CatalogService_,
      _ChannelService_,
      _CmsService_,
      _FeedbackService_,
      _HstService_,
      _SessionService_,
      _SiteMapService_,
    ) => {
      $log = _$log_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      $state = _$state_;
      CatalogService = _CatalogService_;
      ChannelService = _ChannelService_;
      CmsService = _CmsService_;
      FeedbackService = _FeedbackService_;
      HstService = _HstService_;
      SessionService = _SessionService_;
      SiteMapService = _SiteMapService_;
    });

    spyOn(CatalogService, 'load');
    spyOn(CatalogService, 'getComponents');
    spyOn(CmsService, 'publish');
    spyOn(FeedbackService, 'showError');
    spyOn(HstService, 'doPost');
    spyOn(HstService, 'doGet').and.returnValue($q.when({ data: {} }));
    spyOn(HstService, 'doGetWithParams').and.returnValue($q.when({ data: {} }));
    spyOn(HstService, 'doPut');
    spyOn(HstService, 'doDelete');
    spyOn(HstService, 'getChannel').and.returnValue($q.when(channelMock));
    spyOn(SessionService, 'initialize').and.returnValue($q.when());
    spyOn(SessionService, 'hasWriteAccess').and.returnValue(true);
    spyOn(SiteMapService, 'load');
  });

  function loadChannel(id = 'testChannelId') {
    window.CMS_TO_APP.publish('load-channel', { id });
    $rootScope.$digest();
  }

  it('should initialize a channel', () => {
    const testChannel = {
      id: 'testChannelId',
      hostname: 'www.example.com',
      mountId: 'testMountId',
      mountPath: '/testMount',
      siteMapId: 'testSiteMapId',
      contextPath: 'testContextPath',
      previewHstConfigExists: true,
    };

    spyOn(CmsService, 'subscribe');
    HstService.getChannel.and.returnValue($q.resolve(testChannel));
    spyOn($state, 'go');

    ChannelService.initialize();
    expect(CmsService.subscribe).toHaveBeenCalledWith('load-channel', jasmine.any(Function));

    window.CMS_TO_APP.publish('load-channel', testChannel, '/testPath');

    $rootScope.$digest();

    expect(ConfigServiceMock.setContextPathForChannel).toHaveBeenCalledWith('testContextPath');
    expect(HstService.getChannel).toHaveBeenCalledWith(testChannel.id);
    expect(SessionService.initialize).toHaveBeenCalledWith(testChannel.hostname, testChannel.mountId);
    $rootScope.$digest();

    expect($state.go).toHaveBeenCalledWith(
      'hippo-cm.channel',
      {
        channelId: testChannel.id,
        initialRenderPath: '/testMount/testPath',
      },
      {
        reload: true,
      },
    );
  });

  it('should initialize a channel that is not editable yet', () => {
    const testChannel = {
      id: 'testChannelId',
      hostname: 'www.example.com',
      mountId: 'testMountId',
      mountPath: '/testMount',
      siteMapId: 'testSiteMapId',
      contextPath: 'testContextPath',
      previewHstConfigExists: false,
    };
    const editableTestChannel = {
      id: 'testChannelId-preview',
      hostname: 'www.example.com',
      mountId: 'testMountIdChanged',
      mountPath: '/testMount',
      siteMapId: 'testSiteMapIdChanged',
      contextPath: 'testContextPath',
      previewHstConfigExists: true,
    };

    spyOn(CmsService, 'subscribe');
    HstService.getChannel.and.returnValues($q.resolve(testChannel), $q.resolve(editableTestChannel));
    HstService.doPost.and.returnValue($q.resolve());
    spyOn($state, 'go');

    ChannelService.initialize();
    expect(CmsService.subscribe).toHaveBeenCalledWith('load-channel', jasmine.any(Function));

    window.CMS_TO_APP.publish('load-channel', testChannel, '/testPath');

    $rootScope.$apply();
    expect(ConfigServiceMock.setContextPathForChannel).toHaveBeenCalledWith('testContextPath');
    expect(HstService.getChannel).toHaveBeenCalledWith(testChannel.id);
    expect(SessionService.initialize).toHaveBeenCalledWith(testChannel.hostname, testChannel.mountId);

    expect($state.go).toHaveBeenCalledWith(
      'hippo-cm.channel',
      {
        channelId: editableTestChannel.id,
        initialRenderPath: '/testMount/testPath',
      },
      {
        reload: true,
      },
    );
  });

  it('should fallback to the non-editable channel if creating preview configuration fails', () => {
    const testChannel = {
      id: 'testChannelId',
      hostname: 'www.example.com',
      mountId: 'testMountId',
      mountPath: '/testMount',
      siteMapId: 'testSiteMapId',
      contextPath: 'testContextPath',
      previewHstConfigExists: false,
    };

    spyOn(CmsService, 'subscribe');
    HstService.getChannel.and.returnValues($q.resolve(testChannel));
    HstService.doPost.and.returnValue($q.reject({ message: 'Failed to create preview configuration' }));
    spyOn($log, 'error');
    spyOn($state, 'go');

    ChannelService.initialize();
    expect(CmsService.subscribe).toHaveBeenCalledWith('load-channel', jasmine.any(Function));

    window.CMS_TO_APP.publish('load-channel', testChannel, '/testPath');

    $rootScope.$digest();

    expect(ConfigServiceMock.setContextPathForChannel).toHaveBeenCalledWith('testContextPath');
    expect(HstService.getChannel).toHaveBeenCalledWith(testChannel.id);
    expect(SessionService.initialize).toHaveBeenCalledWith(testChannel.hostname, testChannel.mountId);
    $rootScope.$digest();

    expect($log.error).toHaveBeenCalledWith('Failed to load channel \'testChannelId\'.', 'Failed to create preview configuration');
    expect($state.go).toHaveBeenCalledWith(
      'hippo-cm.channel',
      {
        channelId: testChannel.id,
        initialRenderPath: '/testMount/testPath',
      },
      {
        reload: true,
      },
    );
    expect(ChannelService.isEditable()).toBe(false);
    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_ENTER_EDIT');
  });

  it('knows when the current channel is editable', () => {
    expect(ChannelService.isEditable()).toBe(false);

    loadChannel();
    expect(ChannelService.isEditable()).toBe(true);

    SessionService.hasWriteAccess.and.returnValue(false);
    expect(ChannelService.isEditable()).toBe(false);

    SessionService.hasWriteAccess.and.returnValue(true);
    ChannelService.channel.previewHstConfigExists = false;
    expect(ChannelService.isEditable()).toBe(false);
  });

  it('should not enter composer mode when retrieval of the channel fails', () => {
    const id = 'testChannelId';

    HstService.getChannel.and.returnValue($q.reject());
    loadChannel(id);
    expect(HstService.getChannel).toHaveBeenCalledWith(id);
    expect(SessionService.initialize).not.toHaveBeenCalled();
  });

  it('should not save a reference to the channel when load fails', () => {
    SessionService.initialize.and.returnValue($q.reject());
    loadChannel();
    expect(ChannelService.getChannel()).toEqual({});
  });

  it('should not fetch pagemodel when session does not have write permission', () => {
    SessionService.hasWriteAccess.and.returnValue(false);
    loadChannel();
    expect(HstService.doGetWithParams).not.toHaveBeenCalledWith(channelMock.mountId, undefined, 'newpagemodel');
  });

  it('should save a reference to the channel when load succeeds', () => {
    HstService.doGetWithParams.and.returnValue($q.when({ data: { prototypes: ['test'] } }));
    expect(ChannelService.getChannel()).not.toEqual(channelMock);

    loadChannel();

    expect(ChannelService.getChannel()).toEqual(channelMock);
    expect(SiteMapService.load).toHaveBeenCalledWith('siteMapId');
    expect(HstService.doGetWithParams).toHaveBeenCalledWith(channelMock.mountId, undefined, 'newpagemodel');
    expect(ChannelService.hasPrototypes()).toBe(true);
    expect(ChannelService.hasWorkspace()).toBe(true);
  });

  it('should ignore the contextPath if it is /', () => {
    channelMock.contextPath = '/';
    loadChannel();
    expect(ChannelService.makePath()).toEqual('/');

    channelMock.cmsPreviewPrefix = 'cmsPreviewPrefix';
    loadChannel();
    expect(ChannelService.makePath()).toEqual('/cmsPreviewPrefix');
  });

  it('should create paths that start with a slash if the channel\'s webapp runs as ROOT.war and hence the contextPath is an empty string', () => {
    channelMock.contextPath = '';
    loadChannel();
    expect(ChannelService.makePath()).toEqual('/');

    channelMock.cmsPreviewPrefix = 'cmsPreviewPrefix';
    loadChannel();
    expect(ChannelService.makePath()).toEqual('/cmsPreviewPrefix');
  });

  it('should return a preview path that starts with the contextPath', () => {
    channelMock.contextPath = '/contextPath';
    channelMock.mountPath = '/';
    loadChannel();
    expect(ChannelService.makePath('/test')).toEqual('/contextPath/test');

    channelMock.cmsPreviewPrefix = 'cmsPreviewPrefix';
    loadChannel();
    expect(ChannelService.makePath('/test')).toEqual('/contextPath/cmsPreviewPrefix/test');
  });

  it('should return a url that ends with a slash if it equals the contextPath', () => {
    channelMock.contextPath = '/contextPath';
    channelMock.mountPath = '/';
    loadChannel();
    expect(ChannelService.makePath()).toEqual('/contextPath/');
  });

  it('should append argument path to the url', () => {
    channelMock.contextPath = '/contextPath';
    channelMock.cmsPreviewPrefix = 'cmsPreviewPrefix';
    loadChannel();
    expect(ChannelService.makePath('/mountPath/testPath')).toEqual('/contextPath/cmsPreviewPrefix/mountPath/testPath');
  });

  it('should compile a list of preview paths', () => {
    loadChannel();
    expect(ChannelService.getPreviewPaths()).toEqual(['/testContextPath1', '/']);

    channelMock.cmsPreviewPrefix = 'cmsPreviewPrefix';
    loadChannel();
    expect(ChannelService.getPreviewPaths()).toEqual(['/testContextPath1/cmsPreviewPrefix', '/cmsPreviewPrefix']);
  });

  it('should return the Id of the current channel', () => {
    channelMock.id = 'testId';
    loadChannel();
    expect(ChannelService.getId()).toEqual('testId');
  });

  it('should return the name of the current channel', () => {
    channelMock.name = 'testName';
    loadChannel();
    expect(ChannelService.getName()).toEqual('testName');
  });

  it('should clear the current channel', () => {
    spyOn(ChannelService, 'setToolbarDisplayed');

    loadChannel();
    expect(ChannelService.hasChannel()).toBe(true);
    expect(ChannelService.isEditable()).toBe(true);
    ChannelService.setToolbarDisplayed(false);

    ChannelService.clearChannel();

    expect(ChannelService.hasChannel()).toBe(false);
    expect(ChannelService.isEditable()).toBe(false);
    expect(ChannelService.getChannel()).toEqual({});

    expect(ChannelService.setToolbarDisplayed).toHaveBeenCalled();
  });

  it('should switch to a new channel', () => {
    const channelB = {
      id: 'channelB',
      hostname: 'www.channelb.com',
      mountId: 'mountB',
      contextPath: '/b',
      previewHstConfigExists: true,
    };

    loadChannel();

    expect(ChannelService.getId()).toEqual(channelMock.id);
    expect(ChannelService.getChannel()).toEqual(channelMock);

    HstService.getChannel.and.callFake(() => $q.resolve(channelB));
    ChannelService.loadChannel(channelB.id);
    $rootScope.$digest();

    expect(ChannelService.getId()).toEqual(channelB.id);
    expect(ChannelService.getChannel()).toEqual(channelB);
    expect(SessionService.initialize).toHaveBeenCalledWith(channelB.hostname, channelB.mountId);
  });

  // TODO: add a test where the server returns an error upon the ChannelService's request for channel details.

  it('should trigger loading of the channel\'s catalog', () => {
    channelMock.mountId = '1234';
    loadChannel();
    expect(CatalogService.load).toHaveBeenCalledWith('1234');
  });

  it('should publish own changes', () => {
    channelMock.changedBySet = ['testUser'];
    loadChannel();

    channelMock.changedBySet = ['anotherUser'];
    HstService.doPost.and.returnValue($q.resolve());
    HstService.getChannel.and.returnValue($q.when(channelMock));
    ChannelService.publishOwnChanges();
    expect(HstService.doPost).toHaveBeenCalledWith(null, 'mountId', 'publish');

    $rootScope.$digest();
    expect(channelMock.changedBySet).toEqual(['anotherUser']);
  });

  it('should discard own changes', () => {
    channelMock.changedBySet = ['testUser'];
    loadChannel();

    channelMock.changedBySet = ['anotherUser'];
    HstService.doPost.and.returnValue($q.resolve());
    HstService.getChannel.and.returnValue($q.when(channelMock));
    ChannelService.discardOwnChanges();
    expect(HstService.doPost).toHaveBeenCalledWith(null, 'mountId', 'discard');

    $rootScope.$digest();
    expect(channelMock.changedBySet).toEqual(['anotherUser']);
  });

  it('should use the specified users when publishing or discarding changes', () => {
    HstService.doPost.and.returnValue($q.when());
    spyOn(ChannelService, 'reload');
    loadChannel();

    ChannelService.publishChangesOf(['tester']);
    expect(HstService.doPost).toHaveBeenCalledWith({ data: ['tester'] }, 'mountId', 'userswithchanges/publish');

    $rootScope.$digest();
    expect(ChannelService.reload).toHaveBeenCalled();

    ChannelService.discardChangesOf(['tester']);
    expect(HstService.doPost).toHaveBeenCalledWith({ data: ['tester'] }, 'mountId', 'userswithchanges/discard');

    $rootScope.$digest();
    expect(ChannelService.reload).toHaveBeenCalled();
  });

  it('records own changes', () => {
    channelMock.changedBySet = ['tobi', 'obiwan'];
    loadChannel();

    ChannelService.recordOwnChange();

    expect(channelMock.changedBySet).toEqual(['tobi', 'obiwan', 'testUser']);
    expect(CmsService.publish).toHaveBeenCalledWith('channel-changed-in-angular');
  });

  it('recognizes changes already pending', () => {
    channelMock.changedBySet = ['tobi', 'testUser', 'obiwan'];
    loadChannel();

    ChannelService.recordOwnChange();

    expect(channelMock.changedBySet).toEqual(['tobi', 'testUser', 'obiwan']);
    expect(CmsService.publish).toHaveBeenCalledWith('channel-changed-in-angular');
  });

  it('updates the current channel when told so by Ext', () => {
    loadChannel();

    channelMock.changedBySet = ['anotherUser'];
    HstService.getChannel.and.returnValue($q.when(channelMock));
    window.CMS_TO_APP.publish('channel-changed-in-extjs');
    expect(HstService.getChannel).toHaveBeenCalledWith('channelId');
    $rootScope.$digest();

    expect(ChannelService.channel.changedBySet).toEqual(['anotherUser']);
  });

  it('prints console log when it is failed to update current channel as requested by Ext', () => {
    loadChannel();

    channelMock.changedBySet = ['anotherUser'];
    HstService.getChannel.and.returnValue($q.reject());
    spyOn($log, 'error');

    window.CMS_TO_APP.publish('channel-changed-in-extjs');
    $rootScope.$digest();

    expect($log.error).toHaveBeenCalled();
  });

  it('should extract the renderPathInfo given a channel with non-empty preview prefix and mount path', () => {
    channelMock.cmsPreviewPrefix = '_cmsinternal';
    loadChannel();

    expect(ChannelService.extractRenderPathInfo('/testContextPath/_cmsinternal/test/pa.th')).toBe('/test/pa.th');
    expect(ChannelService.extractRenderPathInfo('/testContextPath/_cmsinternal/')).toBe('');
    expect(ChannelService.extractRenderPathInfo('/testContextPath/_cmsinternal')).toBe('');
  });

  it('should extract the renderPathInfo given a channel with empty preview prefix and mount path', () => {
    loadChannel();

    expect(ChannelService.extractRenderPathInfo('/testContextPath/test/render.path'))
      .toBe('/test/render.path');
    expect(ChannelService.extractRenderPathInfo('/testContextPath/')).toBe('');
  });

  it('uses the channel\'s mount path to generate the homepage renderPathInfo', () => {
    channelMock.mountPath = '/mou/nt';
    loadChannel();
    expect(ChannelService.getHomePageRenderPathInfo()).toBe('/mou/nt');

    delete channelMock.mountPath;
    loadChannel();
    expect(ChannelService.getHomePageRenderPathInfo()).toBe('');
  });

  it('should log a warning trying to extract a renderPathInfo if there is no matching channel prefix', () => {
    loadChannel();
    spyOn($log, 'warn');
    const nonMatchingPrefix = '/testContexxxtPath/test/render.path';

    expect(ChannelService.extractRenderPathInfo(nonMatchingPrefix))
      .toBe(nonMatchingPrefix);
    expect($log.warn).toHaveBeenCalled();
  });

  it('should return the channel\'s siteMap ID', () => {
    loadChannel();
    expect(ChannelService.getSiteMapId()).toBe('siteMapId');
  });

  it('should retrieve the new page model from the HST service', (done) => {
    loadChannel();
    HstService.doGetWithParams.and.returnValue($q.when({ data: 'test' }));

    ChannelService.getNewPageModel().then((result) => {
      expect(result).toBe('test');
      done();
    });
    expect(HstService.doGetWithParams).toHaveBeenCalledWith(channelMock.mountId, undefined, 'newpagemodel');
    $rootScope.$digest();
  });

  it('should relay failure to retrieve the new page model from the HST service', (done) => {
    loadChannel();
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
    loadChannel();
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
    loadChannel();
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
    loadChannel();
    HstService.doPut.and.returnValue($q.when());

    ChannelService.saveChannel()
      .then(() => {
        done();
      })
      .catch(() => {
        fail();
      });
    expect(HstService.doPut).toHaveBeenCalledWith(channelMock, 'testRootUuid', 'channels', 'channelId');
    $rootScope.$digest();
  });

  it('should relay any error when saving the channel settings', (done) => {
    loadChannel();
    const error = { };
    HstService.doPut.and.returnValue($q.reject(error));

    ChannelService.saveChannel()
      .then(() => {
        fail();
      })
      .catch((response) => {
        expect(response).toBe(error);
        done();
      });
    $rootScope.$digest();
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
    loadChannel();
    ChannelService.getNewPageModel('other-mount');
    expect(HstService.doGetWithParams).toHaveBeenCalledWith('mountId', { mountId: 'other-mount' }, 'newpagemodel');
  });

  it('sets and retrieves channel properties', () => {
    const properties = {
      key1: 'value1',
      key2: 'value2',
    };
    channelMock.properties = properties;
    loadChannel();
    expect(ChannelService.getProperties()).toBe(properties);

    const modifiedProperties = {
      key1: true,
      key3: 'value3',
    };

    ChannelService.setProperties(modifiedProperties);
    expect(ChannelService.getProperties()).toBe(modifiedProperties);

    HstService.doPut.and.returnValue($q.when());
    ChannelService.saveChannel();
    expect(HstService.doPut.calls.mostRecent().args[0].properties).toBe(modifiedProperties);
    $rootScope.$digest();
  });

  it('returns the content root path of the current channel', () => {
    channelMock.contentRoot = '/content/documents/testChannel';
    loadChannel();
    expect(ChannelService.getContentRootPath()).toEqual('/content/documents/testChannel');
  });

  it('should forward a channel delete request to the HstService', () => {
    loadChannel();

    const promise = $q.defer().promise;
    HstService.doDelete.and.returnValue(promise);

    expect(ChannelService.deleteChannel()).toBe(promise);
    expect(HstService.doDelete).toHaveBeenCalledWith(ConfigServiceMock.rootUuid, 'channels', channelMock.id);
  });

  it('should set channel toolbar display status', () => {
    loadChannel();
    expect(ChannelService.isToolbarDisplayed).toBe(true);
    ChannelService.setToolbarDisplayed(false);
    expect(ChannelService.isToolbarDisplayed).toBe(false);
  });
});
