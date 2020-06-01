/*
 * Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
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
    $translate,
    ChannelMenuService,
    ChannelService,
    CmsService,
    ConfigService,
    FeedbackService,
    HippoIframeService,
    OverlayService,
    PageMenuService,
    PageMetaDataService,
    ProjectService,
    SidePanelService,
  ) {
    'ngInject';

    this.$log = $log;
    this.$rootScope = $rootScope;
    this.$translate = $translate;
    this.ChannelMenuService = ChannelMenuService;
    this.ChannelService = ChannelService;
    this.CmsService = CmsService;
    this.ConfigService = ConfigService;
    this.FeedbackService = FeedbackService;
    this.HippoIframeService = HippoIframeService;
    this.OverlayService = OverlayService;
    this.PageMenuService = PageMenuService;
    this.PageMetaDataService = PageMetaDataService;
    this.ProjectService = ProjectService;
    this.SidePanelService = SidePanelService;

    this.menus = [
      ChannelMenuService.getMenu(subPage => this.showSubpage(subPage)),
      PageMenuService.getMenu(subPage => this.showSubpage(subPage)),
    ];
  }

  $onInit() {
    this.projectsEnabled = this.ConfigService.projectsEnabled;

    this.CmsService.subscribe('reload-page', this._reloadPage, this);
  }

  $onDestroy() {
    this.CmsService.unsubscribe('reload-page', this._reloadPage, this);
  }

  _reloadPage(errorResponse) {
    let errorKey;
    switch (errorResponse.error) {
      case 'ITEM_ALREADY_LOCKED':
        errorKey = 'ERROR_UPDATE_COMPONENT_ITEM_ALREADY_LOCKED';
        break;
      case 'ITEM_NOT_FOUND':
        errorKey = 'ERROR_COMPONENT_DELETED';
        break;
      default:
        errorKey = 'ERROR_UPDATE_COMPONENT';
    }

    this.FeedbackService.showError(errorKey, errorResponse.parameterMap);
    this.HippoIframeService.reload();
  }

  get isContentOverlayDisplayed() {
    return this.OverlayService.isContentOverlayDisplayed;
  }

  set isContentOverlayDisplayed(value) {
    this.OverlayService.showContentOverlay(value);
  }

  get isComponentsOverlayDisplayed() {
    return this.OverlayService.isComponentsOverlayDisplayed;
  }

  set isComponentsOverlayDisplayed(value) {
    this.OverlayService.showComponentsOverlay(value);
  }

  isControlsDisabled() {
    return !this.isChannelLoaded() || !this.isPageLoaded();
  }

  get channel() {
    return this.ChannelService.getChannel();
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

  isEditable() {
    return this.ChannelService.isEditable();
  }

  editMenu(menuUuid) {
    this.menuUuid = menuUuid;
    this.showSubpage('site-menu-editor');
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

  isSidePanelFullScreen(side) {
    return this.SidePanelService.isFullScreen(side);
  }
}

export default ChannelCtrl;
