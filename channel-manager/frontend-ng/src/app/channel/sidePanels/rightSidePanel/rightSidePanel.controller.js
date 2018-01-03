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
    ChannelService,
    CmsService,
    HippoIframeService,
    localStorageService,
    SidePanelService,
  ) {
    'ngInject';

    this.$scope = $scope;
    this.$element = $element;
    this.$timeout = $timeout;
    this.$translate = $translate;
    this.$q = $q;

    this.ChannelService = ChannelService;
    this.CmsService = CmsService;
    this.HippoIframeService = HippoIframeService;
    this.localStorageService = localStorageService;
    this.SidePanelService = SidePanelService;

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
      },
      create2: {
        closeChannelMessage: null,
        openMessage: 'SAVE_CHANGES_GENERIC',
        switchToMessage: null,
      },
    };
  }

  $onInit() {
    this._resetBeforeStateChange();
    this.lastSavedWidth = this.localStorageService.get('rightSidePanelWidth') || '440px';
  }

  $postLink() {
    this.SidePanelService.initialize('right', this.$element.find('.right-side-panel'),
      (id, options) => this._onOpen(id, options),
      () => this.beforeStateChange(this.mode && this.mode.closeChannelMessage).then(this._beforeClosePanel()));
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

  editDocumentAndRefreshPage(documentId) {
    this.HippoIframeService.reload();
    this.openInMode(this.modes.edit, documentId);
  }

  openInMode(mode, options) {
    if (typeof mode === 'string') {
      mode = this.modes[mode];
    }
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

    this.openInMode(this.modes[id], options).then(() => {
      this.$element.addClass('sidepanel-open');
      this.$element.css('width', this.lastSavedWidth);
      this.$element.css('max-width', this.lastSavedWidth);
    });
  }

  switchCreateContentStep(options = {}) {
    if (this.mode !== this.modes.create) {
      throw new Error('Could not switch to Create content step 2 from the current mode');
    }

    this._setMode(this.modes.create2, options);
  }

  closePanel() {
    this._beforeClosePanel();
    return this.SidePanelService.close('right').then(() => {
      this._resetState();
    });
  }

  _beforeClosePanel() {
    this.$element.removeClass('sidepanel-open');
    this.$element.css('max-width', '0px');
    this.setFullWidth(false);
  }

  onResize(newWidth) {
    this.lastSavedWidth = `${newWidth}px`;
    this.localStorageService.set('rightSidePanelWidth', this.lastSavedWidth);
  }

  setFullWidth(state) {
    console.log('set full width', state);
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
