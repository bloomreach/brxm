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

/**
 * Determines which page info extension is shown. Each page info extension is registered as a separate UI router state
 * below 'hippo-cm.channel.page-info'. These states are sticky so switching between them keeps the DOM state alive.
 * As a result, each page extension is initialized when viewed (i.e. when the tab is selected), but then kept alive
 * until the page-info parent state is left again.
 */
class PageInfoService {
  constructor(
    $state,
    $stateRegistry,
    $translate,
    ChannelService,
    ExtensionService,
    PageMetaDataService,
    PathService,
    RightSidePanelService,
  ) {
    'ngInject';

    this.$state = $state;
    this.$stateRegistry = $stateRegistry;
    this.$translate = $translate;
    this.ChannelService = ChannelService;
    this.ExtensionService = ExtensionService;
    this.PageMetaDataService = PageMetaDataService;
    this.PathService = PathService;
    this.RightSidePanelService = RightSidePanelService;

    this._registerPageExtensionStates();
  }

  _registerPageExtensionStates() {
    this.ExtensionService.getExtensions('page').forEach((pageExtension) => {
      this.$stateRegistry.register({
        name: `hippo-cm.channel.page-info.${pageExtension.id}`,
        params: {
          extensionId: pageExtension.id,
        },
        sticky: true,
        views: {
          [pageExtension.id]: {
            component: 'iframeExtension',
          },
        },
        resolve: {
          // Trick to set the attributes 'flex' and 'layout="column"' on the iframe-extension tag so we get a scrollbar
          // inside the tab (see https://github.com/angular-ui/ui-router/issues/3385#issuecomment-333919458).
          flex: () => '',
          layout: () => 'column',
        },
      });
    });
  }

  showPageInfo() {
    this._setTitle();
    this._loadFirstPageExtension();
  }

  updatePageInfo() {
    if (this._isPageInfoShown()) {
      this._setTitle();
      this._updateLoadedPageExtensions();
    }
  }

  get selectedExtensionId() {
    return this.$state.current.params.extensionId;
  }

  set selectedExtensionId(extensionId) {
    this._loadPageExtension(extensionId);
  }

  _setTitle() {
    const pageLabel = this.$translate.instant('PAGE');
    this.RightSidePanelService.setContext(pageLabel);

    const pageName = this._getPageName();
    const pageUrl = this._getPageUrl();
    this.RightSidePanelService.setTitle(pageName, pageUrl);
  }

  _getPageName() {
    const pagePath = this.PageMetaDataService.getPathInfo();
    const baseName = this.PathService.baseName(pagePath);
    return this.PathService.concatPaths('/', baseName);
  }

  _getPageUrl() {
    const channelBaseUrl = this.ChannelService.getChannel().url;
    const pagePath = this.PageMetaDataService.getPathInfo();
    return this.PathService.concatPaths(channelBaseUrl, pagePath);
  }

  _loadFirstPageExtension() {
    const pageExtensions = this.ExtensionService.getExtensions('page');
    const pageUrl = this._getPageUrl();
    this.$state.go(`hippo-cm.channel.page-info.${pageExtensions[0].id}`, { pageUrl });
  }

  _isPageInfoShown() {
    return this.$state.includes('hippo-cm.channel.page-info');
  }

  _updateLoadedPageExtensions() {
    const pageUrl = this._getPageUrl();
    this.$state.go(this.$state.current.name, { pageUrl });
  }

  _loadPageExtension(extensionId) {
    // N.B. the current pageUrl is inherited from the page-info parent state
    this.$state.go(`hippo-cm.channel.page-info.${extensionId}`);
  }

  closePageInfo() {
    this.$state.go('hippo-cm.channel');
  }
}

export default PageInfoService;
