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

const PAGE_TOOLS = 'CHANNEL_PAGE_TOOL';

/**
 * Determines which page tool extension is shown. Each page tool extension is registered as a separate UI router state
 * below 'hippo-cm.channel.page-tools'. These states are sticky so switching between them keeps the DOM state alive.
 * As a result, each page tool extension is initialized when viewed (i.e. when the tab is selected), but then kept
 * alive until the page-tool parent state is left again.
 */
class PageToolsService {
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
    this.getExtensions().forEach((pageToolExtension) => {
      this.$stateRegistry.register({
        name: `hippo-cm.channel.page-tools.${pageToolExtension.id}`,
        params: {
          extensionId: pageToolExtension.id,
        },
        sticky: true,
        views: {
          [pageToolExtension.id]: {
            component: 'pageExtension',
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

  getExtensions() {
    return this.ExtensionService.getExtensions(PAGE_TOOLS);
  }

  hasExtensions() {
    return this.ExtensionService.hasExtensions(PAGE_TOOLS);
  }

  showPageTools() {
    this._setTitle();
    this._loadFirstPageExtension();
  }

  updatePageTools() {
    if (this._isPageToolsShown()) {
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
    const pageExtensions = this.getExtensions();
    const pageUrl = this._getPageUrl();
    this.$state.go(`hippo-cm.channel.page-tools.${pageExtensions[0].id}`, { pageUrl });
  }

  _isPageToolsShown() {
    return this.$state.includes('hippo-cm.channel.page-tools');
  }

  _updateLoadedPageExtensions() {
    const pageUrl = this._getPageUrl();
    this.$state.go(this.$state.current.name, { pageUrl });
  }

  _loadPageExtension(extensionId) {
    // N.B. the current pageUrl is inherited from the page-tools parent state
    this.$state.go(`hippo-cm.channel.page-tools.${extensionId}`);
  }
}

export default PageToolsService;
