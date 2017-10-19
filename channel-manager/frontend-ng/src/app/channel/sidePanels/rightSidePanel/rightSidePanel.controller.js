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

    SidePanelService.initialize('right', $element.find('.right-side-panel'),
      // onOpen
      (type, options) => {
        if (type === 'edit') {
          this._openEditContent(options);
        } else if (type === 'create') {
          this._openCreateContent(options);
        } else {
          throw new Error(`Failed to open rightside panel component with type ${type}`);
        }
        this._onOpen();
      },
      // onClose
      () => this._onClose());
  }

  $onInit() {
    this._resetBeforeStateChange();
    this.lastSavedWidth = this.localStorageService.get('rightSidePanelWidth') || '440px';
  }

  _openEditContent(documentId) {
    if (this.documentId === documentId) {
      return;
    }

    this.beforeStateChange().then(() => {
      this._resetState();
      if (documentId) {
        this.documentId = documentId;
        this.editing = true;
      } else {
        this.createContent = true;
      }
    });
  }

  _openCreateContent(options) {
    this._resetState();
    this.options = options;
    this.create = true;
    this.title = this.$translate.instant('NEW_CONTENT');
  }

  onBeforeStateChange(callback) {
    this.beforeStateChange = callback;
  }
  _resetState() {
    delete this.documentId;
    delete this.editing;
    delete this.create;
    delete this.options;
    this._resetBeforeStateChange();
  }

  _resetBeforeStateChange() {
    this.beforeStateChange = () => this.$q.resolve();
  }

  isLockedOpen() {
    return this.SidePanelService.isOpen('right');
  }

  _onOpen() {
    this.$element.addClass('sidepanel-open');
    this.$element.css('width', this.lastSavedWidth);
    this.$element.css('max-width', this.lastSavedWidth);
  }

  _onClose() {
    this._resetBeforeStateChange();
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
