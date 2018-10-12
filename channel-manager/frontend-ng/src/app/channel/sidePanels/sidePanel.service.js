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

import './sidePanel.scss';

const FULL_SCREEN_ANIMATION_DURATION = 600;
class SidePanelService {
  constructor($document, $mdSidenav, $q, $timeout, $window, ChannelService, CmsService, HippoIframeService) {
    'ngInject';

    this.$document = $document;
    this.$mdSidenav = $mdSidenav;
    this.$q = $q;
    this.$timeout = $timeout;
    this.$window = $window;

    this.ChannelService = ChannelService;
    this.CmsService = CmsService;
    this.HippoIframeService = HippoIframeService;

    this.panels = {};
  }

  initialize(side, sidePanelElement, sideNavElement) {
    const sideNavComponentId = sideNavElement.attr('md-component-id');
    const sideNav = this.$mdSidenav(sideNavComponentId);
    const sideNavClass = sideNav.isOpen()
      ? 'side-panel-open'
      : 'side-panel-closed';

    sidePanelElement.addClass(sideNavClass);

    this.panels[side] = {
      sideNav,
      sidePanelElement,
      fullScreen: false,
    };
  }

  toggle(side) {
    if (this.isOpen(side)) {
      this.close(side);
    } else {
      this.open(side);
    }
  }

  open(side) {
    const panel = this.panels[side];
    if (panel && !panel.sideNav.isOpen()) {
      panel.sidePanelElement
        .removeClass('side-panel-closed')
        .addClass('side-panel-open');
      return panel.sideNav.open();
    }
    return this.$q.resolve();
  }

  isOpen(side) {
    const panel = this.panels[side];
    return panel && panel.sideNav.isOpen();
  }

  close(side) {
    const panel = this.panels[side];
    if (panel && panel.sideNav.isOpen()) {
      return panel.sideNav.close()
        .then(() => {
          panel.sidePanelElement
            .removeClass('side-panel-open')
            .addClass('side-panel-closed');
        });
    }
    return this.$q.resolve();
  }

  liftSidePanelAboveMask() {
    this.isSidePanelLifted = true;
  }

  lowerSidePanelBeneathMask() {
    this.isSidePanelLifted = false;
  }

  isFullScreen(side) {
    const panel = this.panels[side];
    return panel && panel.fullScreen;
  }

  setFullScreen(side, fullScreen) {
    const panel = this.panels[side];
    if (panel) {
      if (fullScreen) {
        this.CmsService.reportUsageStatistic('CMSChannelsFullScreen', { side });
        this.HippoIframeService.lockWidth();
      }
      this.ChannelService.setToolbarDisplayed(!fullScreen);
      panel.fullScreen = fullScreen;
    }

    // Trigger hiding/showing pagination handles of md-tabs.
    // It needs to wait until the full-screen animation is done before it can be triggered.
    this.$timeout(() => {
      this.$window.dispatchEvent(new Event('resize'));
    }, FULL_SCREEN_ANIMATION_DURATION);
  }
}

export default SidePanelService;
