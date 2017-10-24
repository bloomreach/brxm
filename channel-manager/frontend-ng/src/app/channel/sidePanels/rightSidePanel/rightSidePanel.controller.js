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
    this.components = {
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
    };

    SidePanelService.initialize('right', $element.find('.right-side-panel'),
      // onOpen
      (id, options) => this._onOpen(id, options),
      // onClose
      () => this.beforeStateChange(this.closeChannelMessage).then(() => this._onClose()));
  }

  $onInit() {
    this._resetBeforeStateChange();
    this.lastSavedWidth = this.localStorageService.get('rightSidePanelWidth') || '440px';
  }

  onBeforeStateChange(callback) {
    this.beforeStateChange = callback;
  }

  _resetState() {
    delete this.component;
    delete this.options;
    this._resetBeforeStateChange();
  }

  _resetBeforeStateChange() {
    this.onBeforeStateChange(() => this.$q.resolve());
  }

  isLockedOpen() {
    return this.SidePanelService.isOpen('right');
  }

  _setComponent(component, options) {
    this._resetState();
    this.component = component;
    this.options = options;
    this.closeChannelMessage = component.closeChannelMessage;
  }

  _openComponent(component, options) {
    if (this.component === component) {
      this._setComponent(component, options);
      return this.$q.resolve();
    }
    const message = this.component ? this.component.switchToMessage : component.openMessage;
    return this.beforeStateChange(message)
      .then(() => this._setComponent(component, options));
  }

  _onOpen(id, options) {
    if (!this.components[id]) {
      throw new Error(`Failed to open rightside panel component with id ${id}`);
    }

    this._openComponent(this.components[id], options).then(() => {
      this.$element.addClass('sidepanel-open');
      this.$element.css('width', this.lastSavedWidth);
      this.$element.css('max-width', this.lastSavedWidth);
    });
  }

  _onClose() {
    this._resetState();
    return this.$q.resolve();
  }

  onResize(newWidth) {
    this.lastSavedWidth = `${newWidth}px`;
    this.localStorageService.set('rightSidePanelWidth', this.lastSavedWidth);
  }

  closePanel() {
    this.SidePanelService.close('right')
      .then(() => this._resetState())
      .finally(() => {
        this.$element.removeClass('sidepanel-open');
        this.$element.css('max-width', '0px');
        this.setFullWidth(false);
      });

    if (!this.ChannelService.isToolbarDisplayed) {
      this.ChannelService.setToolbarDisplayed(true);
    }
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
