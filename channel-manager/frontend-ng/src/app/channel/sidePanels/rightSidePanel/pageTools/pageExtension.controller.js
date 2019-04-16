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

class pageExtensionCtrl {
  constructor($uiRouterGlobals, ChannelService, PageMetaDataService) {
    'ngInject';

    this.$uiRouterGlobals = $uiRouterGlobals;
    this.ChannelService = ChannelService;
    this.PageMetaDataService = PageMetaDataService;
  }

  $onInit() {
    this.extensionId = this.$uiRouterGlobals.params.extensionId;
    this._setPageContext(this.$uiRouterGlobals.params.pageUrl);
  }

  uiOnParamsChanged(params) {
    if (params.pageUrl) {
      this._setPageContext(params.pageUrl);
    }
  }

  _setPageContext(url) {
    const id = this.PageMetaDataService.getPageId();
    const channelId = this.PageMetaDataService.getChannelId();
    const siteMapItemId = this.PageMetaDataService.getSiteMapItemId();
    const path = this.PageMetaDataService.getPathInfo();
    const channel = this.ChannelService.getChannel();

    this.pageContext = {
      id,
      url,
      path,
      channel: {
        contextPath: channel.contextPath,
        id: channelId,
        mountPath: channel.mountPath,
      },
      siteMapItem: {
        id: siteMapItemId,
      },
    };
  }
}

export default pageExtensionCtrl;
