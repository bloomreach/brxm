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

class pageExtensionCtrl {
  constructor($uiRouterGlobals, PageMetaDataService) {
    'ngInject';

    this.$uiRouterGlobals = $uiRouterGlobals;
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

  _setPageContext(pageUrl) {
    const channelId = this.PageMetaDataService.getChannelId();
    const pageId = this.PageMetaDataService.getPageId();
    const sitemapItemId = this.PageMetaDataService.getSiteMapItemId();

    this.pageContext = {
      channelId,
      pageId,
      pageUrl,
      sitemapItemId,
    };
  }
}

export default pageExtensionCtrl;
