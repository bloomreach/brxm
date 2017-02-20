/*
 * Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
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

class ChannelCtrl {

  constructor(
    $log,
    $rootScope,
    $stateParams,
    $timeout,
    $translate,
    ChannelService,
    ChannelSidePanelService,
    CmsService,
    FeedbackService,
    HippoIframeService,
    PageMetaDataService,
    SessionService,
    ) {
    'ngInject';

    this.$log = $log;
    this.$rootScope = $rootScope;
    this.$timeout = $timeout;
    this.$translate = $translate;
    this.ChannelService = ChannelService;
    this.ChannelSidePanelService = ChannelSidePanelService;
    this.FeedbackService = FeedbackService;
    this.HippoIframeService = HippoIframeService;
    this.PageMetaDataService = PageMetaDataService;
    this.SessionService = SessionService;

    this.isEditMode = false;
    this.isCreatingPreview = false;

    this.HippoIframeService.load($stateParams.initialRenderPath);

    // editToggleState is only used as a 'fake' model for the toggle; isEditMode is updated in the onChange callback,
    // which may happen asynchronously if preview configuration needs to be created.
    this.editToggleState = false;
    this.isEditMode = false;

    CmsService.subscribe('clear-channel', () => this._clear());
  }

  _clear() {
    this.$rootScope.$apply(() => {
      this.hideSubpage();
      this.disableEditMode();
      this.ChannelService.clearChannel();
    });
  }

  isControlsDisabled() {
    return this.isCreatingPreview || !this.isChannelLoaded() || !this.isPageLoaded();
  }

  isChannelLoaded() {
    return this.ChannelService.hasChannel();
  }

  isPageLoaded() {
    return this.HippoIframeService.isPageLoaded();
  }

  enableEditMode() {
    if (!this.isEditMode && !this.ChannelService.hasPreviewConfiguration()) {
      this._createPreviewConfiguration();
    } else {
      this.isEditMode = true;
    }
  }

  disableEditMode() {
    this.isEditMode = false;
  }

  isEditable() {
    return this.SessionService.hasWriteAccess();
  }

  editMenu(menuUuid) {
    this.menuUuid = menuUuid;
    this.showSubpage('menu-editor');
  }

  editContent(contentUuid) {
    this.ChannelSidePanelService.open('right', contentUuid);
  }

  _createPreviewConfiguration() {
    this.isCreatingPreview = true;
    this.ChannelService.createPreviewConfiguration().then(() => {
      this.HippoIframeService.reload().then(() => {
        this.isEditMode = true;
      })
      .finally(() => {
        this.isCreatingPreview = false;
      });
    }).catch(() => {
      this.isCreatingPreview = false;
      this.FeedbackService.showError('ERROR_ENTER_EDIT');
    });
  }

  getRenderVariant() {
    return this.PageMetaDataService.getRenderVariant();
  }

  isSubpageOpen() {
    return !!this.currentSubpage;
  }

  showSubpage(subpage) {
    this.currentSubpage = subpage;
  }

  hideSubpage() {
    delete this.currentSubpage;
  }

  onSubpageSuccess(key, params) {
    this.hideSubpage();
    if (key) {
      // TODO show a toast message notify this change
      this.$log.info(this.$translate.instant(key, params));
    }
  }

  onSubpageError(key, params) {
    this.hideSubpage();
    if (key) {
      this.FeedbackService.showError(key, params);
    }
  }
}

export default ChannelCtrl;
