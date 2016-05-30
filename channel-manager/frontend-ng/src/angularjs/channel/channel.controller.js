/*
 * Copyright 2015-2016 Hippo B.V. (http://www.onehippo.com)
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

export class ChannelCtrl {

  constructor(
      $log,
      $translate,
      $stateParams,
      ChannelService,
      ComponentAdderService,
      FeedbackService,
      HippoIframeService,
      PageMetaDataService,
      ScalingService,
      SessionService
    ) {
    'ngInject';

    this.$log = $log;
    this.$translate = $translate;
    this.ChannelService = ChannelService;
    this.FeedbackService = FeedbackService;
    this.HippoIframeService = HippoIframeService;
    this.PageMetaDataService = PageMetaDataService;
    this.ScalingService = ScalingService;
    this.SessionService = SessionService;
    this.isEditMode = false;
    this.isCreatingPreview = false;

    // reset service state to avoid weird scaling when controller is reloaded due to state change
    ScalingService.setPushWidth(0);

    ComponentAdderService.setCatalogContainerClass('catalog-dd-container');
    ComponentAdderService.setCatalogContainerItemClass('catalog-dd-container-item');

    this.HippoIframeService.load($stateParams.initialRenderPath);
    this.isEditMode = false;
  }

  enterEditMode() {
    if (!this.isEditMode && !this.ChannelService.hasPreviewConfiguration()) {
      this._createPreviewConfiguration();
    } else {
      this.isEditMode = true;
    }
  }

  leaveEditMode() {
    this.isEditMode = false;
  }

  isEditModeActive() {
    return this.isEditMode;
  }

  toggleEditMode() {
    if (this.isEditMode) {
      this.leaveEditMode();
    } else {
      this.enterEditMode();
    }
  }

  isEditable() {
    return this.SessionService.hasWriteAccess();
  }

  editMenu(menuUuid) {
    this.menuUuid = menuUuid;
    this.showSubpage('menu-editor');
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

  getEditButtonBgColor() {
    return this.isEditModeActive() ? 'background-300' : 'background-50';
  }

  getViewButtonBgColor() {
    return this.isEditModeActive() ? 'background-50' : 'background-300';
  }
}
