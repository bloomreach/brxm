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

import template from './leftSidePanel.html';

export class ChannelLeftSidePanelCtrl {
  constructor($scope, $element, ChannelSidePanelService, ChannelService, SiteMapService, HippoIframeService) {
    'ngInject';

    this.$scope = $scope;
    this.ChannelService = ChannelService;
    this.ChannelSidePanelService = ChannelSidePanelService;
    this.SiteMapService = SiteMapService;
    this.HippoIframeService = HippoIframeService;

    ChannelSidePanelService.initialize('left', $element.find('.channel-left-side-panel'));
  }

  isLockedOpen() {
    return this.ChannelSidePanelService.isOpen('left');
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

const channelLeftSidePanelComponentModule = angular
  .module('hippo-cm.channel.leftSidePanelComponentModule', [])
  .component('channelLeftSidePanel', {
    bindings: {
      editMode: '=',
    },
    controller: ChannelLeftSidePanelCtrl,
    template,
  });

export default channelLeftSidePanelComponentModule;
