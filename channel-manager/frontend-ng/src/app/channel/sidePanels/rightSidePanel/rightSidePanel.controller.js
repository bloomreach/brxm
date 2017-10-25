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

class RightSidePanelCtrl {
  constructor(
    $scope,
    $element,
    $timeout,
    $translate,
    $q,
    SidePanelService,
    ChannelService,
    CmsService,
    localStorageService,
  ) {
    'ngInject';

    this.$scope = $scope;
    this.$element = $element;
    this.$timeout = $timeout;
    this.$translate = $translate;
    this.$q = $q;

    this.SidePanelService = SidePanelService;
    this.ChannelService = ChannelService;
    this.CmsService = CmsService;
    this.localStorageService = localStorageService;

    this.lastSavedWidth = null;
    this.isFullWidth = false;
    this.modes = {
      edit: {
        closeChannelMessage: 'SAVE_CHANGES_ON_CLOSE_CHANNEL',
        openMessage: null,
        switchToMessage: 'SAVE_CHANGES_ON_CREATE_DOCUMENT',
      },
      create: {
        closeChannelMessage: null,
        openMessage: 'SAVE_CHANGES_GENERIC',
        switchToMessage: null,
        step: 1,
      },
    };

    SidePanelService.initialize('right', $element.find('.right-side-panel'),
      // onOpen
      (id, options) => this._onOpen(id, options),
      // onClose
      () => this.beforeStateChange(this.mode && this.mode.closeChannelMessage).then(() => this._onClose()));
  }

  $onInit() {
    this._resetBeforeStateChange();
    this.lastSavedWidth = this.localStorageService.get('rightSidePanelWidth') || '440px';
  }

  onBeforeStateChange(callback) {
    this.beforeStateChange = callback;
  }

  _resetState() {
    delete this.mode;
    delete this.options;
    this._resetBeforeStateChange();
  }

  _resetBeforeStateChange() {
    this.onBeforeStateChange(() => this.$q.resolve());
  }

  isLockedOpen() {
    return this.SidePanelService.isOpen('right');
  }

  _setMode(component, options) {
    this._resetState();
    this.mode = component;
    this.options = options;
  }

  _openInMode(mode, options) {
    if (this.mode === mode) {
      this._setMode(mode, options);
      return this.$q.resolve();
    }
    const message = this.mode ? this.mode.switchToMessage : mode.openMessage;
    return this.beforeStateChange(message)
      .then(() => this._setMode(mode, options));
  }

  _onOpen(id, options) {
    if (!this.modes[id]) {
      throw new Error(`Failed to open rightside panel in mode ${id}`);
    }

    this._openInMode(this.modes[id], options).then(() => {
      this.$element.addClass('sidepanel-open');
      this.$element.css('width', this.lastSavedWidth);
      this.$element.css('max-width', this.lastSavedWidth);
    });
  }

  switchCreateContentState() {
    if (this.mode !== this.modes.create || this.mode.step !== 1) {
      throw new Error('Could not switch to Create content step 2 from the current mode');
    }

    this.mode.step = 2;
  }

  _onClose() {
    this.$element.removeClass('sidepanel-open');
    this.$element.css('max-width', '0px');
    this.setFullWidth(false);

    if (!this.ChannelService.isToolbarDisplayed) {
      this.ChannelService.setToolbarDisplayed(true);
    }

    this._resetState();
    return this.$q.resolve();
  }

  closePanel() {
    this.SidePanelService.close('right').then(() => this._onClose());
  }

  onResize(newWidth) {
    this.lastSavedWidth = `${newWidth}px`;
    this.localStorageService.set('rightSidePanelWidth', this.lastSavedWidth);
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
