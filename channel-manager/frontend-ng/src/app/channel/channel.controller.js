/*
 * Copyright 2015-2020 Hippo B.V. (http://www.onehippo.com)
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
    $state,
    $translate,
    ChannelMenuService,
    ChannelService,
    CmsService,
    ConfigService,
    FeedbackService,
    HippoIframeService,
    PageService,
    PageMenuService,
    PageStructureService,
    ProjectService,
    SidePanelService,
    XPageMenuService,
  ) {
    'ngInject';

    this.$log = $log;
    this.$rootScope = $rootScope;
    this.$state = $state;
    this.$translate = $translate;
    this.ChannelMenuService = ChannelMenuService;
    this.ChannelService = ChannelService;
    this.CmsService = CmsService;
    this.ConfigService = ConfigService;
    this.FeedbackService = FeedbackService;
    this.HippoIframeService = HippoIframeService;
    this.PageService = PageService;
    this.PageMenuService = PageMenuService;
    this.PageStructureService = PageStructureService;
    this.ProjectService = ProjectService;
    this.SidePanelService = SidePanelService;
    this.XPageMenuService = XPageMenuService;

    this.menus = [
      ChannelMenuService.getMenu(subPage => this.showSubpage(subPage)),
      PageMenuService.getMenu(subPage => this.showSubpage(subPage)),
      XPageMenuService.getMenu(subPage => this.showSubpage(subPage)),
    ];
  }

  $onInit() {
    this.projectsEnabled = this.ConfigService.projectsEnabled;
    this.isComponentsOverlayDisplayed = false;
    this.isContentOverlayDisplayed = false;

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

  get channel() {
    return this.ChannelService.getChannel();
  }

  isConfigurationLocked() {
    if (!this.isComponentOverlayInitiallyDisabled()) {
      return false;
    }

    return this.ChannelService.isConfigurationLocked();
  }

  isChannelLoaded() {
    return this.ChannelService.hasChannel();
  }

  isPageLoaded() {
    return this.HippoIframeService.isPageLoaded();
  }

  editMenu(menuUuid) {
    this.menuUuid = menuUuid;
    this.showSubpage('site-menu-editor');
  }

  getRenderVariant() {
    const page = this.PageStructureService.getPage();

    return page && page.getMeta().getRenderVariant();
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

  add() {
    if (!this.canAddXPage()) {
      return;
    }

    const {
      xPageLayouts,
      xPageTemplateQueries,
    } = this.PageService.getState('channel');

    const layouts = Object.entries(xPageLayouts).map(([id, displayName]) => ({ id, displayName }));
    const [[documentTemplateQuery, rootPath]] = Object.entries(xPageTemplateQueries);
    const config = {
      layouts,
      documentTemplateQuery,
      rootPath,
    };
    this.$state.go('hippo-cm.channel.create-content-step-1', { config });
  }

  canAddXPage() {
    if (!this.PageService.hasState('channel')) {
      return false;
    }

    const {
      xPageLayouts,
      xPageTemplateQueries,
    } = this.PageService.getState('channel');

    if (!xPageLayouts || Object.keys(xPageLayouts).length === 0) {
      return false;
    }

    if (!xPageTemplateQueries || Object.keys(xPageTemplateQueries).length === 0) {
      return false;
    }

    return true;
  }

  isComponentOverlayInitiallyDisabled() {
    if (!this.ProjectService.isBranch()) {
      return false;
    }

    if (this.ProjectService.isEditingAllowed('components')) {
      return false;
    }

    if (this._pageContainsEditableXPageContainer()) {
      return false;
    }

    return true;
  }

  isContentOverlayInitiallyDisabled() {
    if (!this.ProjectService.isBranch()) {
      return false;
    }

    if (this.ProjectService.isEditingAllowed('content')) {
      return false;
    }

    return true;
  }

  _pageContainsEditableXPageContainer() {
    const page = this.PageStructureService.getPage();
    if (!page) {
      return false;
    }

    const meta = page.getMeta();
    if (!meta) {
      return false;
    }

    if (!meta.isXPage()) {
      return false;
    }

    return page.getContainers().some(container => container.isXPageEditable());
  }
}

export default ChannelCtrl;
