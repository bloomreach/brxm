/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

import MenuService from '../../menu/menu.service';

class PageActionsService extends MenuService {
  constructor(
    $translate,
    ChannelService,
    DialogService,
    FeedbackService,
    HippoIframeService,
    PageMetaDataService,
    SessionService,
    SiteMapItemService,
    SiteMapService,
  ) {
    'ngInject';

    super();

    this.$translate = $translate;
    this.ChannelService = ChannelService;
    this.DialogService = DialogService;
    this.FeedbackService = FeedbackService;
    this.HippoIframeService = HippoIframeService;
    this.PageMetaDataService = PageMetaDataService;
    this.SessionService = SessionService;
    this.SiteMapItemService = SiteMapItemService;
    this.SiteMapService = SiteMapService;

    this.defineMenu('page', {
      translationKey: 'TOOLBAR_BUTTON_PAGE',
      isVisible: () => this._canEditChannel(),
      isEnabled: () => !this.ChannelService.isConfigurationLocked(),
      onClick: () => this.onOpenMenu(),
    })
    .addAction('properties', {
      translationKey: 'TOOLBAR_MENU_PAGE_PROPERTIES',
      isEnabled: () => this._canEditPage(),
      onClick: () => this._pageProperties(),
    })
    .addDivider()
    .addAction('copy', {
      translationKey: 'TOOLBAR_MENU_PAGE_COPY',
      isEnabled: () => this._canCopyPage(),
      onClick: () => this._copyPage(),
    })
    .addAction('move', {
      translationKey: 'TOOLBAR_MENU_PAGE_MOVE',
      isEnabled: () => this._canEditPage(),
      onClick: () => this._movePage(),
    })
    .addAction('delete', {
      translationKey: 'TOOLBAR_MENU_PAGE_DELETE',
      isEnabled: () => this._canEditPage(),
      onClick: () => this._deletePage(),
    })
    .addDivider()
    .addAction('new', {
      translationKey: 'TOOLBAR_MENU_PAGE_NEW',
      isEnabled: () => this._canAddNewPage(),
      onClick: () => this._newPage(),
    });
  }

  onOpenMenu() {
    this.SiteMapItemService.loadAndCache(this.ChannelService.getSiteMapId(), this.PageMetaDataService.getSiteMapItemId());
    this.ChannelService.loadPageModifiableChannels();
  }

  _pageProperties() {
    this.showSubPage('page-properties');
  }

  _copyPage() {
    this.showSubPage('page-copy');
  }

  _movePage() {
    this.showSubPage('page-move');
  }

  _newPage() {
    this.showSubPage('page-new');
  }

  _canEditChannel() {
    return this.ChannelService.isEditable();
  }

  _canEditPage() {
    return this.SiteMapItemService.isEditable();
  }

  _canCopyPage() {
    if (!this.SiteMapItemService.hasItem()) {
      return false;
    }
    if (this.SiteMapItemService.isLocked()) {
      return false;
    }
    if (this.ChannelService.hasWorkspace()) {
      return true; // copy inside this channel is supported
    }
    if (this.SessionService.isCrossChannelPageCopySupported()) {
      const channels = this.ChannelService.getPageModifiableChannels();
      if (channels && channels.length > 0) {
        return true;
      }
    }
    return false;
  }

  _canAddNewPage() {
    return this.ChannelService.hasWorkspace() && this.ChannelService.hasPrototypes();
  }

  _deletePage() {
    const siteMapItem = this.SiteMapItemService.get();
    this._confirmDelete(siteMapItem.name)
      .then(() => {
        this.SiteMapItemService.deleteItem()
          .then(() => {
            const homePage = this.ChannelService.getHomePageRenderPathInfo();
            this.HippoIframeService.load(homePage);

            const siteMapId = this.ChannelService.getSiteMapId();
            this.SiteMapService.load(siteMapId);   // reload sitemap (left side panel)

            this.SiteMapItemService.clear();       // wipe meta-data of current page
            this.ChannelService.recordOwnChange(); // mark the channel changed
          })
          .catch(() => {
            this.FeedbackService.showError('ERROR_DELETE_PAGE');
          });
      });
      // do nothing on cancel
  }

  _confirmDelete(page) {
    const confirm = this.DialogService.confirm()
      .textContent(this.$translate.instant('CONFIRM_DELETE_PAGE_MESSAGE', { page }))
      .ok(this.$translate.instant('DELETE'))
      .cancel(this.$translate.instant('CANCEL'));

    return this.DialogService.show(confirm);
  }
}

export default PageActionsService;
