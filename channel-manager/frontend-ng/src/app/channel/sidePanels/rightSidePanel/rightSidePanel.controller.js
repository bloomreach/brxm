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

class RightSidePanelCtrl {
  constructor(
    $element,
    $mdConstant,
    $state,
    $transitions,
    SidePanelService,
    ChannelService,
    CmsService,
    localStorageService,
    RightSidePanelService,
  ) {
    'ngInject';

    this.$element = $element;
    this.$transitions = $transitions;

    this.SidePanelService = SidePanelService;
    this.ChannelService = ChannelService;
    this.CmsService = CmsService;
    this.localStorageService = localStorageService;
    this.RightSidePanelService = RightSidePanelService;

    this.isFullWidth = false;

    SidePanelService.initialize('right', $element.find('.right-side-panel'));

    // Prevent the default closing action bound to the escape key by Angular Material.
    // We should show the "unsaved changes" dialog first.
    $element.on('keydown', (e) => {
      if (e.which === $mdConstant.KEY_CODE.ESCAPE) {
        e.stopImmediatePropagation();
        $state.go('^');
      }
    });
  }

  $onInit() {
    this.lastSavedWidth = this.localStorageService.get('rightSidePanelWidth') || '440px';

    this.$transitions.onBefore({ to: 'hippo-cm.channel.*' }, () => this._openPanel());
    this.$transitions.onSuccess({ from: 'hippo-cm.channel.*', to: 'hippo-cm.channel' }, () => this._closePanel());
  }

  onResize(newWidth) {
    this.lastSavedWidth = `${newWidth}px`;
    this.localStorageService.set('rightSidePanelWidth', this.lastSavedWidth);
  }

  isLoading() {
    return this.RightSidePanelService.isLoading();
  }

  getTitle() {
    return this.RightSidePanelService.getTitle();
  }

  isLockedOpen() {
    return this.SidePanelService.isOpen('right');
  }

  _openPanel() {
    this.SidePanelService.open('right')
      .then(() => {
        this.$element.addClass('sidepanel-open');
        this.$element.css('width', this.lastSavedWidth);
        this.$element.css('max-width', this.lastSavedWidth);
      });
  }

  _closePanel() {
    this.$element.removeClass('sidepanel-open');
    this.$element.css('max-width', '0px');

    this.SidePanelService.close('right')
      .finally(() => {
        this.setFullWidth(false);
      });
  }

  setFullWidth(state) {
    if (state === true) {
      this.$element.addClass('fullwidth');
      this.ChannelService.setToolbarDisplayed(false);
      this.CmsService.reportUsageStatistic('CMSChannelsFullScreen');
    } else {
      this.$element.removeClass('fullwidth');
      this.ChannelService.setToolbarDisplayed(true);
    }
    this.isFullWidth = state;
  }
}

export default RightSidePanelCtrl;
