/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

describe('PageInfoService', () => {
  let $rootScope;
  let $state;
  let $stateRegistry;
  let $translate;
  let ChannelService;
  let PageInfoService;
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

    const ExtensionService = jasmine.createSpyObj('ExtensionService', ['getExtensions']);
    ExtensionService.getExtensions.and.returnValue([extension1, extension2]);

    angular.mock.module('hippo-cm.channel.pageInfo', ($provide) => {
      $provide.value('ExtensionService', ExtensionService);
    });

    inject((
      _$rootScope_,
      _$state_,
      _$stateRegistry_,
      _$translate_,
      _ChannelService_,
      _PageInfoService_,
      _PageMetaDataService_,
      _RightSidePanelService_,
    ) => {
      $rootScope = _$rootScope_;
      $state = _$state_;
      $stateRegistry = _$stateRegistry_;
      $translate = _$translate_;
      ChannelService = _ChannelService_;
      PageInfoService = _PageInfoService_;
      PageMetaDataService = _PageMetaDataService_;
      RightSidePanelService = _RightSidePanelService_;
    });

    spyOn(ChannelService, 'getChannel');
    spyOn(PageMetaDataService, 'getPathInfo');
  });

  function pageUrl(channelUrl, pagePath) {
    ChannelService.getChannel.and.returnValue({ url: channelUrl });
    PageMetaDataService.getPathInfo.and.returnValue(pagePath);
    return PageInfoService._getPageUrl();
  }

  describe('state per page extension', () => {
    it('is registered', () => {
      expect($stateRegistry.states['hippo-cm.channel.page-info.extension1']).toBeDefined();
      expect($stateRegistry.states['hippo-cm.channel.page-info.extension2']).toBeDefined();
    });

    it('has certain properties', () => {
      const state = $stateRegistry.states['hippo-cm.channel.page-info.extension1'];
      expect(state.name).toEqual('hippo-cm.channel.page-info.extension1');
      expect(state.parameter('extensionId').value()).toEqual('extension1');
      expect(state.sticky).toEqual(true);
      expect(state.views.extension1).toBeDefined();
      expect(state.resolve.flex()).toEqual('');
      expect(state.resolve.layout()).toEqual('column');
    });
  });

  it('generates the correct pageUrl', () => {
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

  describe('showPageInfo', () => {
    beforeEach(() => {
      pageUrl('https://example.com', '/pageUrl');
    });

    it('sets the title of the right side-panel', () => {
      spyOn($translate, 'instant').and.callThrough();
      spyOn(RightSidePanelService, 'setTitle');

      PageInfoService.showPageInfo();

      expect($translate.instant).toHaveBeenCalledWith('PAGE_INFO_TITLE', { pageUrl: 'https://example.com/pageUrl' });
      expect(RightSidePanelService.setTitle).toHaveBeenCalledWith('PAGE_INFO_TITLE');
    });

    it('goes to the state of the first page extension', () => {
      spyOn($state, 'go');
      PageInfoService.showPageInfo();
      expect($state.go).toHaveBeenCalledWith('hippo-cm.channel.page-info.extension1', { pageUrl: 'https://example.com/pageUrl' });
    });
  });

  describe('updatePageInfo', () => {
    beforeEach(() => {
      pageUrl('https://example.com', '/pageUrl');
    });

    describe('when page info is already shown', () => {
      beforeEach(() => {
        PageInfoService.showPageInfo();
        $rootScope.$digest();

        pageUrl('https://example.com', '/anotherPageUrl');
      });

      it('updates the title of the right side-panel', () => {
        spyOn($translate, 'instant').and.callThrough();
        spyOn(RightSidePanelService, 'setTitle');

        PageInfoService.updatePageInfo();

        expect($translate.instant).toHaveBeenCalledWith('PAGE_INFO_TITLE', { pageUrl: 'https://example.com/anotherPageUrl' });
        expect(RightSidePanelService.setTitle).toHaveBeenCalledWith('PAGE_INFO_TITLE');
      });

      it('updates the state of all loaded page extensions', () => {
        spyOn($state, 'go');
        PageInfoService.updatePageInfo();
        expect($state.go).toHaveBeenCalledWith('hippo-cm.channel.page-info.extension1', { pageUrl: 'https://example.com/anotherPageUrl' });
      });
    });

    describe('when no page info is shown', () => {
      it('does nothing', () => {
        spyOn($translate, 'instant').and.callThrough();
        spyOn(RightSidePanelService, 'setTitle');
        spyOn($state, 'go');

        PageInfoService.updatePageInfo();

        expect($translate.instant).not.toHaveBeenCalled();
        expect(RightSidePanelService.setTitle).not.toHaveBeenCalled();
        expect($state.go).not.toHaveBeenCalled();
      });
    });
  });

  describe('selectedExtensionId', () => {
    it('returns the extension ID of the current state', () => {
      $state.go('hippo-cm.channel.page-info.extension2', { pageUrl: '' });
      $rootScope.$digest();
      expect(PageInfoService.selectedExtensionId).toEqual('extension2');
    });

    it('navigates to another page extension state when set', () => {
      $state.go('hippo-cm.channel.page-info.extension1', { pageUrl: '/test' });
      $rootScope.$digest();

      PageInfoService.selectedExtensionId = 'extension2';
      $rootScope.$digest();

      expect($state.current.name).toEqual('hippo-cm.channel.page-info.extension2');
    });
  });

  describe('closePageInfo', () => {
    it('goes to the hippo-cm.channel state', () => {
      spyOn($state, 'go');
      PageInfoService.closePageInfo();
      expect($state.go).toHaveBeenCalledWith('hippo-cm.channel');
    });
  });
});
