/*
 * Copyright 2016-2019 Hippo B.V. (http://www.onehippo.com)
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
  constructor(
    $document,
    $mdSidenav,
    $mdUtil,
    $q,
    $rootScope,
    $timeout,
    $window,
    ChannelService,
    CmsService,
    HippoIframeService,
  ) {
    'ngInject';

    this.$document = $document;
    this.$mdSidenav = $mdSidenav;
    this.$mdUtil = $mdUtil;
    this.$q = $q;
    this.$rootScope = $rootScope;
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

    this.$rootScope.$watch(() => sideNav.isOpen(), (isOpened) => {
      sidePanelElement
        .toggleClass('side-panel-open', isOpened)
        .toggleClass('side-panel-closed', !isOpened);
    });

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

  focus(side) {
    this.$mdUtil.nextTick(() => {
      const panel = this.panels[side];
      const focusElement = this.$mdUtil.findFocusTarget(panel.sidePanelElement)
        || this.$mdUtil.findFocusTarget(panel.sidePanelElement, '[md-sidenav-focus]')
        || panel.sidePanelElement.find('md-sidenav');

      if (focusElement) {
        focusElement.focus();
      }
    });
  }

  open(side) {
    const panel = this.panels[side];

    if (!panel) {
      return this.$q.resolve();
    }

    if (panel.sideNav.isOpen()) {
      this.focus(side);

      return this.$q.resolve();
    }

    return panel.sideNav.open().then(() => {
      panel.sidePanelElement.one('transitionend', () => this.focus(side));
    });
  }

  isOpen(side) {
    const panel = this.panels[side];
    return panel && panel.sideNav.isOpen();
  }

  close(side) {
    const panel = this.panels[side];
    if (panel && panel.sideNav.isOpen()) {
      return panel.sideNav.close();
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
