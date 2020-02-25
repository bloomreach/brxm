/*
 * Copyright 2017-2020 Hippo B.V. (http://www.onehippo.com)
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

import MenuService from '../menu.service';

class PageMenuService extends MenuService {
  constructor(
    $translate,
    ChannelService,
    DialogService,
    FeedbackService,
    HippoIframeService,
    PageStructureService,
    PageToolsService,
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
    this.PageStructureService = PageStructureService;
    this.PageToolsService = PageToolsService;
    this.SessionService = SessionService;
    this.SiteMapItemService = SiteMapItemService;
    this.SiteMapService = SiteMapService;

    const menu = this.defineMenu('page', {
      translationKey: 'TOOLBAR_BUTTON_PAGE',
      isVisible: () => this._canAccessMenuItem(),
      isEnabled: () => !this.ChannelService.isConfigurationLocked(),
      onClick: () => this.onOpenMenu(),
    });

    menu
      .addAction('tools', {
        isVisible: () => this._hasPageExtensions(),
        onClick: () => this._pageTools(),
        translationKey: 'TOOLBAR_MENU_PAGE_TOOLS',
      })
      .addAction('properties', {
        isEnabled: () => this._canEditPage(),
        isVisible: () => this._hasWriteAccess(),
        onClick: () => this._pageProperties(),
        translationKey: 'TOOLBAR_MENU_PAGE_PROPERTIES',
      })
      .addDivider({
        isVisible: () => this._hasWriteAccess(),
      })
      .addAction('copy', {
        isEnabled: () => this._canCopyPage(),
        isVisible: () => this._hasWriteAccess(),
        onClick: () => this._copyPage(),
        translationKey: 'TOOLBAR_MENU_PAGE_COPY',
      })
      .addAction('move', {
        isEnabled: () => this._canEditPage(),
        isVisible: () => this._hasWriteAccess(),
        onClick: () => this._movePage(),
        translationKey: 'TOOLBAR_MENU_PAGE_MOVE',
      })
      .addAction('delete', {
        isEnabled: () => this._canEditPage(),
        isVisible: () => this._hasWriteAccess(),
        onClick: () => this._deletePage(),
        translationKey: 'TOOLBAR_MENU_PAGE_DELETE',
      })
      .addDivider({
        isVisible: () => this._hasWriteAccess(),
      })
      .addAction('new', {
        isEnabled: () => this._canAddNewPage(),
        isVisible: () => this._hasWriteAccess(),
        onClick: () => this._newPage(),
        translationKey: 'TOOLBAR_MENU_PAGE_NEW',
      });
  }

  onOpenMenu() {
    const page = this.PageStructureService.getPage();
    const siteMapId = this.ChannelService.getSiteMapId();
    const siteMapItemId = page && page.getMeta().getSiteMapItemId();
    this.SiteMapItemService.loadAndCache(siteMapId, siteMapItemId);
    this.ChannelService.loadPageModifiableChannels();
  }

  _pageTools() {
    this.PageToolsService.showPageTools();
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

  _hasPageExtensions() {
    return this.PageToolsService.hasExtensions();
  }

  _hasWriteAccess() {
    return this.SessionService.hasWriteAccess();
  }

  _canAccessMenuItem() {
    return this._canEditChannel() || this._hasPageExtensions();
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
    const numberOfChildren = this.SiteMapItemService.getNumberOfChildren();
    this._confirmDelete(siteMapItem.name, numberOfChildren)
      .then(() => {
        this.SiteMapItemService.deleteItem()
          .then(() => {
            const homePage = this.ChannelService.getHomePageRenderPathInfo();
            this.HippoIframeService.load(homePage);

            const siteMapId = this.ChannelService.getSiteMapId();
            this.SiteMapService.load(siteMapId); // reload sitemap (left side panel)

            this.SiteMapItemService.clear(); // wipe meta-data of current page
            this.ChannelService.recordOwnChange(); // mark the channel changed
          })
          .catch(() => {
            this.FeedbackService.showError('ERROR_DELETE_PAGE');
          });
      });
    // do nothing on cancel
  }

  _confirmDelete(page, numberOfChildren) {
    const confirm = this.DialogService.confirm()
      .textContent(this._getPageDeleteMessage(page, numberOfChildren))
      .ok(this.$translate.instant('DELETE'))
      .cancel(this.$translate.instant('CANCEL'));

    return this.DialogService.show(confirm);
  }

  _getPageDeleteMessage(page, numberOfChildren) {
    if (numberOfChildren > 0) {
      return this.$translate.instant(
        'CONFIRM_DELETE_MULTIPLE_PAGE_MESSAGE',
        {
          numberOfPages: numberOfChildren + 1,
          page,
        },
      );
    }
    return this.$translate.instant('CONFIRM_DELETE_SINGLE_PAGE_MESSAGE', { page });
  }
}

export default PageMenuService;
