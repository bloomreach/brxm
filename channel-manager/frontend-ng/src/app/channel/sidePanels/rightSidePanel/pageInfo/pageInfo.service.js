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

class PageInfoService {
  constructor($state, $stateRegistry, $translate, ExtensionService, RightSidePanelService) {
    'ngInject';

    this.$state = $state;
    this.$stateRegistry = $stateRegistry;
    this.$translate = $translate;
    this.ExtensionService = ExtensionService;
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

  showPageInfo(pageUrl) {
    const title = this.$translate.instant('PAGE_INFO_TITLE', { pageUrl });
    this.RightSidePanelService.setTitle(title);

    const pageExtensions = this.ExtensionService.getExtensions('page');
    this.$state.go(`hippo-cm.channel.page-info.${pageExtensions[0].id}`, { pageUrl });
  }

  get selectedExtensionId() {
    return this.$state.current.params.extensionId;
  }

  set selectedExtensionId(extensionId) {
    this.$state.go(`hippo-cm.channel.page-info.${extensionId}`);
  }

  closePageInfo() {
    this.$state.go('hippo-cm.channel');
  }
}

export default PageInfoService;
