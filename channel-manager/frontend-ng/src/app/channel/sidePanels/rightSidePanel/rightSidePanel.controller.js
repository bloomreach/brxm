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
    $mdConstant,
    $timeout,
    $translate,
    $q,
    SidePanelService,
    ChannelService,
    CmsService,
    ContentService,
    DialogService,
    HippoIframeService,
    FeedbackService,
    localStorageService,
    FieldService,
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
    this.ContentService = ContentService;
    this.DialogService = DialogService;
    this.HippoIframeService = HippoIframeService;
    this.FeedbackService = FeedbackService;
    this.localStorageService = localStorageService;
    this.FieldService = FieldService;

    this.defaultTitle = $translate.instant('EDIT_CONTENT');
    this.closeLabel = $translate.instant('CLOSE');
    this.cancelLabel = $translate.instant('CANCEL');
    this.deleteDraftOnClose = true;

    this.lastSavedWidth = null;
    this.isFullWidth = false;

    SidePanelService.initialize('right', $element.find('.right-side-panel'),
      // onOpen
      (documentId) => {
        this.openEditor(documentId);
        this._onOpen();
      },
      // onClose
      () => $q.resolve());

    // Prevent the default closing action bound to the escape key by Angular Material.
    // We should show the "unsaved changes" dialog first.
    $element.on('keydown', (e) => {
      if (e.which === $mdConstant.KEY_CODE.ESCAPE) {
        e.stopImmediatePropagation();
        this._closePanel();
      }
    });
  }

  $onInit() {
    this.lastSavedWidth = this.localStorageService.get('rightSidePanelWidth') || '440px';
  }

  openEditor(documentId) {
    if (this.documentId === documentId) {
      return;
    }

    this._resetState();
    if (documentId) {
      this.documentId = documentId;
      this.editing = true;
    } else {
      this.createContent = true;
    }
  }

  _resetState() {
    delete this.documentId;
    delete this.editing;
    delete this.createContent;
  }

  isLockedOpen() {
    return this.SidePanelService.isOpen('right');
  }

  _onOpen() {
    this.$element.addClass('sidepanel-open');
    this.$element.css('width', this.lastSavedWidth);
    this.$element.css('max-width', this.lastSavedWidth);
  }

  onResize(newWidth) {
    this.lastSavedWidth = `${newWidth}px`;
    this.localStorageService.set('rightSidePanelWidth', this.lastSavedWidth);
  }

  _closePanel() {
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

    // this.FieldService.triggerInputFocus();
  }
}

export default RightSidePanelCtrl;
