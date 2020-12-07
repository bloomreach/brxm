/*
 * Copyright 2016-2020 Hippo B.V. (http://www.onehippo.com)
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
  let CatalogService;
  let ChannelService;
  let CmsService;
  let ConfigServiceMock;
  let FeedbackService;
  let HstService;
  let ProjectService;
  let SessionService;
  let SiteMapService;
  let channelMock;
  let projectMock;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    projectMock = {
      id: 'master',
    };

    channelMock = {
      contextPath: '/testContextPath',
      hostGroup: 'testHostGroup',
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
      locale: 'en',
      projectsEnabled: false,
    };

    angular.mock.module(($provide) => {
      $provide.value('ConfigService', ConfigServiceMock);
    });

    inject((
      _$log_,
      _$q_,
      _$rootScope_,
      _CatalogService_,
      _ChannelService_,
      _CmsService_,
      _FeedbackService_,
      _HstService_,
      _ProjectService_,
      _SessionService_,
      _SiteMapService_,
    ) => {
      $log = _$log_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      CatalogService = _CatalogService_;
      ChannelService = _ChannelService_;
      CmsService = _CmsService_;
      FeedbackService = _FeedbackService_;
      HstService = _HstService_;
      ProjectService = _ProjectService_;
      SessionService = _SessionService_;
      SiteMapService = _SiteMapService_;
    });

    spyOn(CatalogService, 'load');
    spyOn(CatalogService, 'getComponents');
    spyOn(CmsService, 'publish');
    spyOn(FeedbackService, 'showErrorResponse');
    spyOn(HstService, 'doPost');
    spyOn(HstService, 'doGet').and.returnValue($q.when({ data: {} }));
    spyOn(HstService, 'doGetWithParams').and.returnValue($q.when({ data: {} }));
    spyOn(HstService, 'doPut');
    spyOn(HstService, 'doDelete');
    spyOn(HstService, 'getChannel').and.returnValue($q.when(channelMock));
    spyOn(SessionService, 'initializeContext').and.returnValue($q.when());
    spyOn(SessionService, 'initializeState').and.returnValue($q.when());
    spyOn(SessionService, 'canWriteHstConfig').and.returnValue(true);
    spyOn(SiteMapService, 'load');
    ProjectService.selectedProject = projectMock;
  });

  function loadChannel(id = 'testChannelId', contextPath = 'testContextPath', hostGroup = 'testHostGroup') {
    ChannelService.initializeChannel(id, contextPath, hostGroup);
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
      hostGroup: 'dev-localhost',
      previewHstConfigExists: true,
    };

    HstService.getChannel.and.returnValue($q.resolve(testChannel));

    ChannelService.initializeChannel(testChannel.id, testChannel.contextPath, testChannel.hostGroup, '/testPath');
    $rootScope.$digest();

    expect(HstService.getChannel).toHaveBeenCalledWith(testChannel.id, testChannel.contextPath, testChannel.hostGroup);
    expect(SessionService.initializeState).toHaveBeenCalledWith(testChannel);
    $rootScope.$digest();
  });

  it('should initialize a channel that is not editable yet', () => {
    const testChannel = {
      id: 'testChannelId',
      hostname: 'www.example.com',
      mountId: 'testMountId',
      mountPath: '/testMount',
      siteMapId: 'testSiteMapId',
      contextPath: 'testContextPath',
      hostGroup: 'dev-localhost',
      previewHstConfigExists: false,
    };
    const editableTestChannel = {
      id: 'testChannelId-preview',
      hostname: 'www.example.com',
      mountId: 'testMountIdChanged',
      mountPath: '/testMount',
      siteMapId: 'testSiteMapIdChanged',
      contextPath: 'testContextPath',
      hostGroup: 'dev-localhost',
      previewHstConfigExists: true,
    };

    HstService.getChannel.and.returnValues($q.resolve(testChannel), $q.resolve(editableTestChannel));
    HstService.doPost.and.returnValue($q.resolve());

    ChannelService.initializeChannel(testChannel.id, testChannel.contextPath, testChannel.hostGroup, '/testPath');
    $rootScope.$digest();

    expect(HstService.getChannel).toHaveBeenCalledWith(testChannel.id, testChannel.contextPath, testChannel.hostGroup);
    expect(SessionService.initializeState).toHaveBeenCalledWith(testChannel);
  });

  it('should use the live channel when no -preview channel exists and the user is not allowed to create it', () => {
    const testChannel = {
      id: 'testChannelId',
      hostname: 'www.example.com',
      mountId: 'testMountId',
      mountPath: '/testMount',
      siteMapId: 'testSiteMapId',
      contextPath: 'testContextPath',
      hostGroup: 'dev-localhost',
      preview: false,
      previewHstConfigExists: false,
    };

    HstService.getChannel.and.returnValue($q.resolve(testChannel));
    SessionService.canWriteHstConfig.and.returnValue(false);

    const { id, contextPath, hostGroup } = testChannel;
    ChannelService.initializeChannel(id, contextPath, hostGroup, '/testPath');
    $rootScope.$digest();

    expect(HstService.getChannel).toHaveBeenCalledWith(id, contextPath, hostGroup);
    expect(HstService.getChannel).not.toHaveBeenCalledWith(`${id}-preview`, contextPath, hostGroup);
    expect(SessionService.initializeState).toHaveBeenCalledWith(testChannel);
    expect(HstService.doPost).not.toHaveBeenCalled();
    expect(ChannelService.getChannel()).toEqual(testChannel);
  });

  it('should fallback to the non-editable channel if creating preview configuration fails', () => {
    const testChannel = {
      id: 'testChannelId',
      hostname: 'www.example.com',
      mountId: 'testMountId',
      mountPath: '/testMount',
      siteMapId: 'testSiteMapId',
      contextPath: 'testContextPath',
      hostGroup: 'dev-localhost',
      previewHstConfigExists: false,
    };

    HstService.getChannel.and.returnValues($q.resolve(testChannel));
    HstService.doPost.and.returnValue($q.reject({ message: 'Failed to create preview configuration' }));
    spyOn($log, 'error');

    ChannelService.initializeChannel(testChannel.id, testChannel.contextPath, testChannel.hostGroup, '/testPath');
    $rootScope.$digest();

    expect(HstService.getChannel).toHaveBeenCalledWith(testChannel.id, testChannel.contextPath, testChannel.hostGroup);
    expect(SessionService.initializeState).toHaveBeenCalledWith(testChannel);
    $rootScope.$digest();

    expect($log.error).toHaveBeenCalledWith(
      'Failed to load channel \'testChannelId\'.',
      'Failed to create preview configuration',
    );
    expect(ChannelService.isEditable()).toBe(false);
    expect(FeedbackService.showErrorResponse).toHaveBeenCalledWith(undefined, 'ERROR_ENTER_EDIT');
  });

  it('knows when the current channel is editable', () => {
    expect(ChannelService.isEditable()).toBe(false);

    loadChannel();
    expect(ChannelService.isEditable()).toBe(true);

    SessionService.canWriteHstConfig.and.returnValue(false);
    expect(ChannelService.isEditable()).toBe(false);

    SessionService.canWriteHstConfig.and.returnValue(true);
    ChannelService.channel.previewHstConfigExists = false;
    expect(ChannelService.isEditable()).toBe(false);
  });

  it('should not enter composer mode when retrieval of the channel fails', () => {
    const id = 'testChannelId';
    const contextPath = 'testContextPath';
    const hostGroup = 'testHostGroup';

    HstService.getChannel.and.returnValue($q.reject());
    loadChannel(id, contextPath, hostGroup);
    expect(HstService.getChannel).toHaveBeenCalledWith(id, contextPath, hostGroup);
    expect(SessionService.initializeState).not.toHaveBeenCalled();
  });

  it('should not save a reference to the channel when load fails', () => {
    SessionService.initializeState.and.returnValue($q.reject());
    loadChannel();
    expect(ChannelService.getChannel()).toEqual({});
  });

  it('should restore the session of the old channel when initialization of the new channel fails', () => {
    loadChannel('testChannelId', 'testContextPath');

    SessionService.initializeState.calls.reset();
    HstService.getChannel.and.returnValue($q.reject());

    loadChannel('anotherChannel', 'anotherContextPath');

    expect(SessionService.initializeState).toHaveBeenCalledWith(channelMock);
  });

  it('should not fetch pagemodel when session does not have write permission', () => {
    SessionService.canWriteHstConfig.and.returnValue(false);
    loadChannel();
    expect(HstService.doGetWithParams).not.toHaveBeenCalledWith(channelMock.mountId, undefined, 'newpagemodel');
  });

  it('should save a reference to the channel when load succeeds', () => {
    HstService.doGetWithParams.and.returnValue($q.when({ data: { prototypes: ['test'] } }));
    expect(ChannelService.getChannel()).not.toEqual(channelMock);

    loadChannel();

    expect(ChannelService.getChannel()).toEqual(channelMock);
    expect(SiteMapService.load).toHaveBeenCalledWith('siteMapId');
  });

  describe('matchesChannel', () => {
    it('returns false when no channel is loaded yet', () => {
      expect(ChannelService.matchesChannel(channelMock.id)).toBe(false);
    });

    it('returns true for the current channel', () => {
      loadChannel(channelMock.id);
      expect(ChannelService.matchesChannel(channelMock.id)).toBe(true);
    });

    it('returns false for another channel', () => {
      loadChannel(channelMock.id);
      expect(ChannelService.matchesChannel('anotherChannelId')).toBe(false);
    });
  });

  it('should ignore the contextPath if it is /', () => {
    channelMock.contextPath = '/';
    loadChannel();
    expect(ChannelService.makePath()).toEqual('/');

    channelMock.cmsPreviewPrefix = 'cmsPreviewPrefix';
    loadChannel();
    expect(ChannelService.makePath()).toEqual('/cmsPreviewPrefix');
  });

  it('should create paths that start with a slash if the contextPath is an empty string', () => {
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

  it('should return the id of the current channel', () => {
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

    loadChannel(channelB.id);

    expect(ChannelService.getId()).toEqual(channelB.id);
    expect(ChannelService.getChannel()).toEqual(channelB);
    expect(SessionService.initializeState).toHaveBeenCalledWith(channelB);
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
    spyOn($rootScope, '$broadcast');

    ChannelService.publishOwnChanges();
    expect(HstService.doPost).toHaveBeenCalledWith(null, 'mountId', 'publish');

    $rootScope.$digest();
    expect(channelMock.changedBySet).toEqual(['anotherUser']);
    expect($rootScope.$broadcast).toHaveBeenCalledWith('channel:changes:publish');
  });

  it('should discard own changes', () => {
    channelMock.changedBySet = ['testUser'];
    loadChannel();

    channelMock.changedBySet = ['anotherUser'];
    HstService.doPost.and.returnValue($q.resolve());
    HstService.getChannel.and.returnValue($q.when(channelMock));
    spyOn($rootScope, '$broadcast');

    ChannelService.discardOwnChanges();
    expect(HstService.doPost).toHaveBeenCalledWith(null, 'mountId', 'discard');

    $rootScope.$digest();
    expect(channelMock.changedBySet).toEqual(['anotherUser']);
    expect($rootScope.$broadcast).toHaveBeenCalledWith('channel:changes:discard');
  });

  describe('should use the specified users when publishing or discarding changes', () => {
    beforeEach(() => {
      HstService.doPost.and.returnValue($q.when());
      spyOn(ChannelService, 'reload');

      loadChannel();
      spyOn($rootScope, '$broadcast');
    });

    it('should publish users changes', () => {
      ChannelService.publishChangesOf(['tester']);

      expect(HstService.doPost).toHaveBeenCalledWith({ data: ['tester'] }, 'mountId', 'userswithchanges/publish');

      $rootScope.$digest();
      expect(ChannelService.reload).toHaveBeenCalled();
      expect($rootScope.$broadcast).toHaveBeenCalledWith('channel:changes:publish');
    });

    it('should discard users changes', () => {
      ChannelService.discardChangesOf(['tester']);
      expect(HstService.doPost).toHaveBeenCalledWith({ data: ['tester'] }, 'mountId', 'userswithchanges/discard');

      $rootScope.$digest();
      expect(ChannelService.reload).toHaveBeenCalled();
      expect($rootScope.$broadcast).toHaveBeenCalledWith('channel:changes:discard');
    });
  });

  describe('checkChanges', () => {
    beforeEach(() => {
      loadChannel();
    });

    it('should emit "page:check-changes"', () => {
      const listener = jasmine.createSpy();
      $rootScope.$on('page:check-changes', listener);

      ChannelService.checkChanges();
      expect(listener).toHaveBeenCalled();
    });

    it('should not throw an error if the backend call fails', (done) => {
      HstService.doGet.and.returnValue($q.reject());

      ChannelService.checkChanges().then(done);
      $rootScope.$digest();
    });

    it('should get change-set from the backend', () => {
      ChannelService.checkChanges();

      expect(HstService.doGet).toHaveBeenCalledWith('mountId', 'mychanges');
    });

    it('should record changes if backend returns changes', () => {
      spyOn(ChannelService, 'recordOwnChange');
      HstService.doGet.and.returnValue($q.when({ data: [{ id: 'testUser' }] }));

      ChannelService.checkChanges();
      $rootScope.$digest();

      expect(ChannelService.recordOwnChange).toHaveBeenCalled();
    });

    it('should not record changes if backend does not return changes', () => {
      spyOn(ChannelService, 'recordOwnChange');
      HstService.doGet.and.returnValue($q.when({ data: [] }));

      ChannelService.checkChanges();
      $rootScope.$digest();

      expect(ChannelService.recordOwnChange).not.toHaveBeenCalled();
    });
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

  it('uses the channel\'s mount path to generate the homepage renderPathInfo', () => {
    channelMock.mountPath = '/mou/nt';
    loadChannel();
    expect(ChannelService.getHomePageRenderPathInfo()).toBe('/mou/nt');

    delete channelMock.mountPath;
    loadChannel();
    expect(ChannelService.getHomePageRenderPathInfo()).toBe('');
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
    const done = 'DONE';
    const promise = { then: () => done };
    HstService.doDelete.and.returnValue(promise);

    expect(ChannelService.deleteChannel()).toEqual(done);
    expect(HstService.doDelete).toHaveBeenCalledWith(ConfigServiceMock.rootUuid, 'channels', channelMock.id);
  });

  it('should set channel toolbar display status', () => {
    loadChannel();
    expect(ChannelService.isToolbarDisplayed).toBe(true);
    ChannelService.setToolbarDisplayed(false);
    expect(ChannelService.isToolbarDisplayed).toBe(false);
  });

  describe('getOrigin', () => {
    beforeEach(() => {
      spyOn(ChannelService, 'getChannel');
      spyOn(ChannelService, 'getProperties');
    });

    it('returns an origin from the preview url', () => {
      ChannelService.getChannel.and.returnValue({ spaUrl: 'http://example.com:3000/something' });

      expect(ChannelService.getOrigin()).toBe('http://example.com:3000');
    });

    it('returns an origin from the channel url', () => {
      ChannelService.getChannel.and.returnValue({ url: 'http://localhost:8080/_cmsinternal' });

      expect(ChannelService.getOrigin()).toBe('http://localhost:8080');
    });

    it('returns an empty origin when there is no configured url', () => {
      expect(ChannelService.getOrigin()).toBeUndefined();
    });

    it('returns the CMS origin', () => {
      ChannelService.getChannel.and.returnValue({ url: '/_cmsinternal' });

      expect(ChannelService.getOrigin()).toBe('http://localhost:8080');
    });

    it('returns an origin relative to the CMS', () => {
      ChannelService.getChannel.and.returnValue({ url: '//localhost:3000' });

      expect(ChannelService.getOrigin()).toBe('http://localhost:3000');
    });
  });
});
