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

describe('HippoIframeService', () => {
  let $http;
  let $log;
  let $q;
  let $rootScope;
  let $window;
  let hippoIframe;
  let pageMeta;
  let iframe;
  let ChannelService;
  let CommunicationService;
  let ConfigService;
  let DomService;
  let HippoIframeService;
  let PageStructureService;
  let PageToolsService;
  let ScrollService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    CommunicationService = jasmine.createSpyObj('CommunicationService', ['reload']);
    DomService = jasmine.createSpyObj('DomService', ['getAssetUrl']);

    angular.mock.module(($provide) => {
      $provide.value('CommunicationService', CommunicationService);
      $provide.value('DomService', DomService);
    });

    inject((
      _$http_,
      _$log_,
      _$q_,
      _$rootScope_,
      _$window_,
      _ChannelService_,
      _ConfigService_,
      _HippoIframeService_,
      _PageStructureService_,
      _PageToolsService_,
      _ScrollService_,
    ) => {
      $http = _$http_;
      $log = _$log_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      $window = _$window_;
      ChannelService = _ChannelService_;
      ConfigService = _ConfigService_;
      HippoIframeService = _HippoIframeService_;
      PageStructureService = _PageStructureService_;
      PageToolsService = _PageToolsService_;
      ScrollService = _ScrollService_;
    });

    spyOn(ChannelService, 'makePath').and.returnValue('/test/url');
    spyOn(PageToolsService, 'updatePageTools');
    spyOn(ScrollService, 'savePosition');
    spyOn(ScrollService, 'restorePosition');

    jasmine.getFixtures().load('channel/hippoIframe/hippoIframe.service.fixture.html');

    hippoIframe = $j('hippo-iframe');
    iframe = $j('#testIframe');
    HippoIframeService.initialize(hippoIframe, iframe);

    const page = jasmine.createSpyObj('page', ['getMeta']);
    pageMeta = jasmine.createSpyObj('pageMeta', ['getContextPath', 'getPathInfo']);
    page.getMeta.and.returnValue(pageMeta);
    spyOn(PageStructureService, 'getPage').and.returnValue(page);
  });

  describe('initializePath', () => {
    beforeEach(() => {
      HippoIframeService.renderPathInfo = '/path';
      spyOn(ChannelService, 'getChannel');
      spyOn(ChannelService, 'makeRenderPath');
      spyOn(HippoIframeService, 'load');
      spyOn(HippoIframeService, 'reload');
    });

    it('changes the page in the current channel', () => {
      ChannelService.makeRenderPath.and.returnValue('/different/path');
      HippoIframeService.initializePath('/different/path');
      expect(HippoIframeService.load).toHaveBeenCalledWith('/different/path');
    });

    it('changes to the home page in the current channel', () => {
      ChannelService.makeRenderPath.and.returnValue('');
      HippoIframeService.initializePath('');
      expect(HippoIframeService.load).toHaveBeenCalledWith('');
    });

    it('reloads the same render path in the same context', () => {
      ChannelService.makeRenderPath.and.returnValue('/path');
      ChannelService.getChannel.and.returnValue({ contextPath: '/site' });
      pageMeta.getContextPath.and.returnValue('/site');

      HippoIframeService.initializePath('/path');

      expect(HippoIframeService.reload).toHaveBeenCalled();
    });

    it('reloads the current page in the current channel', () => {
      ChannelService.makeRenderPath.and.returnValue('');
      HippoIframeService.initializePath(null);
      expect(HippoIframeService.reload).toHaveBeenCalled();
    });

    it('changes to the same channel-relative path in a different channel', () => {
      HippoIframeService.renderPathInfo = '/mount-of-channel1/path';
      ChannelService.makeRenderPath.and.returnValue('/mount-of-channel2/path');

      HippoIframeService.initializePath('/path');

      expect(HippoIframeService.load).toHaveBeenCalledWith('/mount-of-channel2/path');
    });

    it('changes to the same channel-relative path in a different context', () => {
      HippoIframeService.renderPathInfo = '/path';
      ChannelService.makeRenderPath.and.returnValue('/path');
      ChannelService.getChannel.and.returnValue({ contextPath: '/site' });
      pageMeta.getContextPath.and.returnValue('/differentContextPath');

      HippoIframeService.initializePath('/path');

      expect(HippoIframeService.load).toHaveBeenCalledWith('/path');
    });
  });

  it('knows when a page has been loaded', () => {
    expect(HippoIframeService.isPageLoaded()).toBe(false);
    HippoIframeService.load('dummy');
    expect(HippoIframeService.isPageLoaded()).toBe(false);
    $rootScope.$emit('page:change', { initial: true });

    expect(HippoIframeService.isPageLoaded()).toBe(true);
  });

  it('does not reload the iframe when no page has been loaded yet', (done) => {
    HippoIframeService.initialize(undefined); // undo initialization

    HippoIframeService.reload().then(() => {
      expect(HippoIframeService.deferredReload).toBeFalsy();
      done();
    });

    $rootScope.$digest();
  });

  it('reloads the iframe and waits for the page load to complete', () => {
    HippoIframeService.pageLoaded = true;
    spyOn($log, 'warn');

    HippoIframeService.reload();
    $rootScope.$digest();

    expect(ScrollService.savePosition).toHaveBeenCalled();
    expect(ScrollService.restorePosition).not.toHaveBeenCalled();
    expect(CommunicationService.reload).toHaveBeenCalled();
    expect(PageToolsService.updatePageTools).not.toHaveBeenCalled();

    $rootScope.$emit('page:change', { initial: true });

    expect(ScrollService.restorePosition).toHaveBeenCalled();
    expect(PageToolsService.updatePageTools).toHaveBeenCalled();

    expect($log.warn).not.toHaveBeenCalled();
  });

  it('should perform force iframe reload', () => {
    HippoIframeService.pageLoaded = true;
    spyOn(HippoIframeService, 'getSrc').and.returnValue('something');

    HippoIframeService.reload(true);
    $rootScope.$digest();

    expect(CommunicationService.reload).not.toHaveBeenCalled();
    expect(HippoIframeService.iframeJQueryElement.attr('src')).toBe('something');

    $rootScope.$emit('page:change', { initial: true });
  });

  it('logs a warning upon a reload request when a reload is already ongoing', () => {
    spyOn($log, 'warn');

    HippoIframeService.pageLoaded = true;
    HippoIframeService.reload();

    expect($log.warn).not.toHaveBeenCalled();

    HippoIframeService.reload();

    expect($log.warn).toHaveBeenCalled();
  });

  it('reports page loads as user activity', () => {
    spyOn($window.APP_TO_CMS, 'publish');
    $rootScope.$emit('page:change', { initial: true });

    expect($window.APP_TO_CMS.publish).toHaveBeenCalledWith('user-activity');
  });

  it('ignores page loads which are no reloads', () => {
    $rootScope.$emit('page:change');
    $rootScope.$emit('page:change', { initial: false });

    expect(ScrollService.restorePosition).not.toHaveBeenCalled();
  });

  it('loads the requested renderPathInfo', () => {
    ChannelService.makePath.and.returnValue('fullPath');
    HippoIframeService.load('dummy');
    expect(ChannelService.makePath).toHaveBeenCalledWith('dummy');
    expect(HippoIframeService.getSrc()).toBe('fullPath');
  });

  it('extracts the current renderPathInfo when the page has been loaded', () => {
    spyOn(ChannelService, 'getHomePageRenderPathInfo').and.returnValue('/channel');
    pageMeta.getPathInfo.and.returnValue('/dummy');

    $rootScope.$emit('page:change', { initial: true });
    $rootScope.$digest();

    expect(HippoIframeService.getCurrentRenderPathInfo()).toBe('/channel/dummy');
  });

  it('should trim the trailing slash from the current rendering path', () => {
    spyOn(ChannelService, 'getHomePageRenderPathInfo').and.returnValue('/channel');
    pageMeta.getPathInfo.and.returnValue('/');

    $rootScope.$emit('page:change', { initial: true });
    $rootScope.$digest();

    expect(HippoIframeService.getCurrentRenderPathInfo()).toBe('/channel');
  });

  it('uses jQuery to trigger a reload if the src attribute matches the to-be-loaded path', () => {
    pageMeta.getPathInfo.and.returnValue('/target');
    HippoIframeService.load('/target');
    $rootScope.$emit('page:change', { initial: true });

    spyOn(iframe, 'attr');
    HippoIframeService.load('/not/target');
    expect(iframe.attr).toHaveBeenCalledWith('src', '/test/url');
  });

  it('set the CSS variable --locked-width when invoking lockWidth()', () => {
    spyOn(hippoIframe, 'outerWidth').and.returnValue(250);

    HippoIframeService.lockWidth();

    expect(hippoIframe[0].style.getPropertyValue('--locked-width')).toBe('250px');
  });

  describe('dev mode', () => {
    beforeEach(() => {
      spyOn(ConfigService, 'isDevMode').and.returnValue(true);
    });

    it('stores the current renderPath in sessionStorage', () => {
      pageMeta.getPathInfo.and.returnValue('dummy');
      $rootScope.$emit('page:change', { initial: true });
      $rootScope.$digest();

      expect(sessionStorage.channelPath).toBe('dummy');
    });

    afterEach(() => {
      delete sessionStorage.channelPath;
    });
  });

  describe('getAssetUrl', () => {
    it('should append antiCache query string parameter', () => {
      ConfigService.antiCache = 'something';
      HippoIframeService.getAssetUrl('/path');

      expect(DomService.getAssetUrl).toHaveBeenCalledWith('/path?antiCache=something');
    });

    it('should call the DOM service to resolve url', () => {
      DomService.getAssetUrl.and.returnValue('url');

      expect(HippoIframeService.getAssetUrl('/path')).toBe('url');
    });
  });

  describe('getAsset', () => {
    it('should send get request', (done) => {
      spyOn($http, 'get').and.returnValue($q.resolve({ data: 'something' }));
      spyOn(HippoIframeService, 'getAssetUrl').and.returnValue('url');

      HippoIframeService.getAsset('/path').then((data) => {
        expect(data).toBe('something');
        expect(HippoIframeService.getAssetUrl).toHaveBeenCalledWith('/path');
        expect($http.get).toHaveBeenCalledWith('url');

        done();
      });

      $rootScope.$digest();
    });
  });

  describe('isEditSharedContainers', () => {
    it('returns "edit-shared-containers" state as emitted by the iframe', () => {
      $rootScope.$emit('iframe:page:edit-shared-containers', true);
      expect(HippoIframeService.isEditSharedContainers()).toBe(true);

      $rootScope.$emit('iframe:page:edit-shared-containers', false);
      expect(HippoIframeService.isEditSharedContainers()).toBe(false);
    });

    it('resets the state to "false" on page change', () => {
      $rootScope.$emit('iframe:page:edit-shared-containers', true);
      expect(HippoIframeService.isEditSharedContainers()).toBe(true);

      pageMeta.getPathInfo.and.returnValue('dummy');
      $rootScope.$emit('page:change', { initial: true });
      $rootScope.$digest();

      expect(HippoIframeService.isEditSharedContainers()).toBe(false);
    });
  });
});
