/*
 * Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

describe('PageToolsService', () => {
  let $rootScope;
  let $state;
  let $stateRegistry;
  let $translate;
  let ChannelService;
  let ExtensionService;
  let PageToolsService;
  let PageMetaDataService;
  let RightSidePanelService;

  let extension1;
  let extension2;

  beforeEach(() => {
    extension1 = {
      id: 'extension1',
      displayName: 'Page Extension 1',
      context: 'page',
    };
    extension2 = {
      id: 'extension2',
      displayName: 'Page Extension 2',
      context: 'page',
    };

    ExtensionService = jasmine.createSpyObj('ExtensionService', ['hasExtensions', 'getExtensions']);
    ExtensionService.getExtensions.and.returnValue([extension1, extension2]);

    angular.mock.module('hippo-cm.channel.pageTools', ($provide) => {
      $provide.value('ExtensionService', ExtensionService);
    });

    inject((
      _$rootScope_,
      _$state_,
      _$stateRegistry_,
      _$translate_,
      _ChannelService_,
      _PageToolsService_,
      _PageMetaDataService_,
      _RightSidePanelService_,
    ) => {
      $rootScope = _$rootScope_;
      $state = _$state_;
      $stateRegistry = _$stateRegistry_;
      $translate = _$translate_;
      ChannelService = _ChannelService_;
      PageToolsService = _PageToolsService_;
      PageMetaDataService = _PageMetaDataService_;
      RightSidePanelService = _RightSidePanelService_;
    });

    spyOn(ChannelService, 'getChannel');
    spyOn(PageMetaDataService, 'getPathInfo');
  });

  function pageName(pagePath) {
    PageMetaDataService.getPathInfo.and.returnValue(pagePath);
    return PageToolsService._getPageName();
  }

  function pageUrl(channelUrl, pagePath) {
    ChannelService.getChannel.and.returnValue({ url: channelUrl });
    PageMetaDataService.getPathInfo.and.returnValue(pagePath);
    return PageToolsService._getPageUrl();
  }

  it('generates the correct page name', () => {
    expect(pageName('')).toEqual('/');
    expect(pageName('/')).toEqual('/');
    expect(pageName('/news')).toEqual('/news');
    expect(pageName('/news/')).toEqual('/news');
    expect(pageName('/news/2018')).toEqual('/2018');
    expect(pageName('/news/2018/')).toEqual('/2018');
    expect(pageName('/news/2018/my-page.html')).toEqual('/my-page.html');
    expect(pageName('/weird-path.html/page')).toEqual('/page');
  });

  it('generates the correct page URL', () => {
    expect(pageUrl('http://localhost:8080/site', '')).toEqual('http://localhost:8080/site');
    expect(pageUrl('http://localhost:8080/site', '/news')).toEqual('http://localhost:8080/site/news');
    expect(pageUrl('http://localhost:8080/site/nl', '')).toEqual('http://localhost:8080/site/nl');
    expect(pageUrl('http://localhost:8080/site/nl', '/nieuws')).toEqual('http://localhost:8080/site/nl/nieuws');
    expect(pageUrl('https://example.com', '')).toEqual('https://example.com');
    expect(pageUrl('https://example.com', '/news')).toEqual('https://example.com/news');
    expect(pageUrl('https://example.com/', '/news')).toEqual('https://example.com/news');
    expect(pageUrl('https://example.com/en', '/news')).toEqual('https://example.com/en/news');
    expect(pageUrl('https://example.com/en/', '/news/page.html')).toEqual('https://example.com/en/news/page.html');
  });

  describe('hasExtensions', () => {
    it('uses the ExtensionService to check for "CHANNEL_PAGE_TOOL" extensions', () => {
      PageToolsService.hasExtensions();
      expect(ExtensionService.hasExtensions).toHaveBeenCalledWith('CHANNEL_PAGE_TOOL');
    });

    it('returns true if there are extensions', () => {
      ExtensionService.hasExtensions.and.returnValue(true);
      expect(PageToolsService.hasExtensions()).toBe(true);
    });

    it('returns false if there are no extensions', () => {
      ExtensionService.hasExtensions.and.returnValue(false);
      expect(PageToolsService.hasExtensions()).toBe(false);
    });
  });

  describe('getExtensions', () => {
    it('returns "CHANNEL_PAGE_TOOL" extensions', () => {
      expect(PageToolsService.getExtensions()).toEqual([extension1, extension2]);
      expect(ExtensionService.getExtensions).toHaveBeenCalledWith('CHANNEL_PAGE_TOOL');
    });
  });

  describe('state per page extension', () => {
    it('is registered', () => {
      expect($stateRegistry.states['hippo-cm.channel.page-tools.extension1']).toBeDefined();
      expect($stateRegistry.states['hippo-cm.channel.page-tools.extension2']).toBeDefined();
    });

    it('has certain properties', () => {
      const state = $stateRegistry.states['hippo-cm.channel.page-tools.extension1'];
      expect(state.name).toEqual('hippo-cm.channel.page-tools.extension1');
      expect(state.parameter('extensionId').value()).toEqual('extension1');
      expect(state.sticky).toEqual(true);
      expect(state.views.extension1).toBeDefined();
      expect(state.resolve.flex()).toEqual('');
      expect(state.resolve.layout()).toEqual('column');
    });
  });

  describe('showPageTools', () => {
    beforeEach(() => {
      const pagePath = '/news/2018/my-page.html';
      pageName(pagePath);
      pageUrl('https://example.com', pagePath);
    });

    it('sets the title of the right side-panel', () => {
      spyOn($translate, 'instant').and.callThrough();
      spyOn(RightSidePanelService, 'setContext');
      spyOn(RightSidePanelService, 'setTitle');

      PageToolsService.showPageTools();

      expect($translate.instant).toHaveBeenCalledWith('PAGE');
      expect(RightSidePanelService.setContext).toHaveBeenCalledWith('PAGE');
      expect(RightSidePanelService.setTitle).toHaveBeenCalledWith(
        '/my-page.html', 'https://example.com/news/2018/my-page.html',
      );
    });

    it('goes to the state of the first page extension', () => {
      spyOn($state, 'go');
      PageToolsService.showPageTools();
      expect($state.go).toHaveBeenCalledWith(
        'hippo-cm.channel.page-tools.extension1',
        { pageUrl: 'https://example.com/news/2018/my-page.html' },
      );
    });
  });

  describe('updatePageTools', () => {
    beforeEach(() => {
      const pagePath = '/news/2018/my-page.html';
      pageName(pagePath);
      pageUrl('https://example.com', pagePath);
    });

    describe('when page tools are already shown', () => {
      beforeEach(() => {
        PageToolsService.showPageTools();
        $rootScope.$digest();

        const anotherPagePath = '/events/latest';
        pageName(anotherPagePath);
        pageUrl('https://example.com', anotherPagePath);
      });

      it('updates the title of the right side-panel', () => {
        spyOn($translate, 'instant').and.callThrough();
        spyOn(RightSidePanelService, 'setContext');
        spyOn(RightSidePanelService, 'setTitle');

        PageToolsService.updatePageTools();

        expect($translate.instant).toHaveBeenCalledWith('PAGE');
        expect(RightSidePanelService.setContext).toHaveBeenCalledWith('PAGE');
        expect(RightSidePanelService.setTitle).toHaveBeenCalledWith('/latest', 'https://example.com/events/latest');
      });

      it('updates the state of all loaded page extensions', () => {
        spyOn($state, 'go');
        PageToolsService.updatePageTools();
        expect($state.go).toHaveBeenCalledWith(
          'hippo-cm.channel.page-tools.extension1',
          { pageUrl: 'https://example.com/events/latest' },
        );
      });
    });

    describe('when no page tools are shown', () => {
      it('does nothing', () => {
        spyOn($translate, 'instant').and.callThrough();
        spyOn(RightSidePanelService, 'setTitle');
        spyOn($state, 'go');

        PageToolsService.updatePageTools();

        expect($translate.instant).not.toHaveBeenCalled();
        expect(RightSidePanelService.setTitle).not.toHaveBeenCalled();
        expect($state.go).not.toHaveBeenCalled();
      });
    });
  });

  describe('selectedExtensionId', () => {
    it('returns the extension ID of the current state', () => {
      $state.go('hippo-cm.channel.page-tools.extension2', { pageUrl: '' });
      $rootScope.$digest();
      expect(PageToolsService.selectedExtensionId).toEqual('extension2');
    });

    it('navigates to another page extension state when set', () => {
      $state.go('hippo-cm.channel.page-tools.extension1', { pageUrl: '/test' });
      $rootScope.$digest();

      PageToolsService.selectedExtensionId = 'extension2';
      $rootScope.$digest();

      expect($state.current.name).toEqual('hippo-cm.channel.page-tools.extension2');
    });
  });
});
