/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

class LeftSidePanelCtrl {
  constructor(
    $scope,
    $element,
    SidePanelService,
    CatalogService,
    SiteMapService,
    HippoIframeService,
  ) {
    'ngInject';

    this.$scope = $scope;
    this.CatalogService = CatalogService;
    this.SidePanelService = SidePanelService;
    this.SiteMapService = SiteMapService;
    this.HippoIframeService = HippoIframeService;
    this.SiteMapService = SiteMapService;

    SidePanelService.initialize('left', $element.find('.left-side-panel'));
  }

  isLockedOpen() {
    return this.SidePanelService.isOpen('left');
  }

  showComponentsTab() {
    const catalog = this.getCatalog();
    return this.componentsVisible && catalog.length > 0;
  }

  getCatalog() {
    return this.CatalogService.getComponents();
  }

  getSiteMap() {
    return this.SiteMapService.get();
  }

  getSiteMapItemHash(item) {
    return `${item.pathInfo}\0${item.pageTitle || item.name}`;
  }

  showPage(siteMapItem) {
    this.HippoIframeService.load(siteMapItem.renderPathInfo);
  }

  isActiveSiteMapItem(siteMapItem) {
    return siteMapItem.renderPathInfo === this.HippoIframeService.getCurrentRenderPathInfo();
  }

  isSidePanelLifted() {
    return this.SidePanelService.isSidePanelLifted;
  }
}

export default LeftSidePanelCtrl;
