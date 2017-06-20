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
    CmsService,
    ConfigService,
    SidePanelService,
    FeedbackService,
    HippoIframeService,
    PageMetaDataService,
    ) {
    'ngInject';

    this.$log = $log;
    this.$rootScope = $rootScope;
    this.$timeout = $timeout;
    this.$translate = $translate;
    this.ChannelService = ChannelService;
    this.CmsService = CmsService;
    this.ConfigService = ConfigService;
    this.SidePanelService = SidePanelService;
    this.FeedbackService = FeedbackService;
    this.HippoIframeService = HippoIframeService;
    this.PageMetaDataService = PageMetaDataService;

    this.projectsEnabled = ConfigService.projectsEnabled;

    this.isContentOverlayDisplayed = true;
    this.isComponentsOverlayDisplayed = false;

    this.HippoIframeService.load($stateParams.initialRenderPath);

    CmsService.subscribe('clear-channel', () => this._clear());
  }

  _clear() {
    this.$rootScope.$apply(() => {
      this.hideSubpage();
      this.ChannelService.clearChannel();
    });
  }

  isControlsDisabled() {
    return !this.isChannelLoaded() || !this.isPageLoaded();
  }

  isConfigurationLocked() {
    return this.ChannelService.isConfigurationLocked();
  }

  isChannelLoaded() {
    return this.ChannelService.hasChannel();
  }

  isPageLoaded() {
    return this.HippoIframeService.isPageLoaded();
  }

  projectsEnabled() {
    return this.ConfigService.projectsEnabled;
  }

  isEditable() {
    return this.ChannelService.isEditable();
  }

  editMenu(menuUuid) {
    this.menuUuid = menuUuid;
    this.showSubpage('menu-editor');
  }

  editContent(contentUuid) {
    this.SidePanelService.open('right', contentUuid);
    this.CmsService.reportUsageStatistic('CMSChannelsEditContent');
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

  isToolbarDisplayed() {
    return this.ChannelService.isToolbarDisplayed;
  }
}

export default ChannelCtrl;
