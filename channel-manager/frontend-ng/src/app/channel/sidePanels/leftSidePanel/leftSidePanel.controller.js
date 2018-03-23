/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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
    localStorageService,
    SidePanelService,
    CatalogService,
    SiteMapService,
    HippoIframeService,
    ChannelService,
  ) {
    'ngInject';

    this.$scope = $scope;
    this.$element = $element;
    this.localStorageService = localStorageService;
    this.CatalogService = CatalogService;
    this.SidePanelService = SidePanelService;
    this.SiteMapService = SiteMapService;
    this.HippoIframeService = HippoIframeService;
    this.SiteMapService = SiteMapService;
    this.ChannelService = ChannelService;

    this.lastSavedWidth = null;
  }

  $onInit() {
    this.lastSavedWidth = this.localStorageService.get('leftSidePanelWidth') || '320px';
    this.sideNavElement = this.$element.find('.left-side-panel');
    this.sideNavElement.css('width', this.lastSavedWidth);
  }

  $postLink() {
    this.SidePanelService.initialize('left', this.$element.find('.left-side-panel'));
  }

  onResize(newWidth) {
    this.lastSavedWidth = `${newWidth}px`;
    this.localStorageService.set('leftSidePanelWidth', this.lastSavedWidth);
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

  isEditable() {
    return this.ChannelService.isEditable();
  }

}

export default LeftSidePanelCtrl;
