/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

export class ChannelSidenavCtrl {
  constructor($scope, $element, ChannelSidenavService, ChannelService, SiteMapService, HippoIframeService) {
    'ngInject';

    this.ChannelService = ChannelService;
    this.ChannelSidenavService = ChannelSidenavService;
    this.SiteMapService = SiteMapService;
    this.HippoIframeService = HippoIframeService;

    ChannelSidenavService.initialize($element.find('md-sidenav'));

    $scope.$watch('sidenav.editMode', () => {
      if (!this.editMode) {
        ChannelSidenavService.close();
      }
    });
  }

  showComponentsTab() {
    const catalog = this.getCatalog();
    return this.editMode && catalog.length > 0;
  }

  getCatalog() {
    return this.ChannelService.getCatalog();
  }

  getSiteMap() {
    return this.SiteMapService.get();
  }

  showPage(siteMapItem) {
    this.HippoIframeService.load(siteMapItem.renderPathInfo);
  }

  isActiveSiteMapItem(siteMapItem) {
    return siteMapItem.renderPathInfo === this.HippoIframeService.getCurrentRenderPathInfo();
  }
}
