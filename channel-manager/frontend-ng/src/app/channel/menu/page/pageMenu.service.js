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
    PageService,
    PageStructureService,
    PageToolsService,
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
    this.SiteMapItemService = SiteMapItemService;
    this.SiteMapService = SiteMapService;

    const menu = this.defineMenu('page', {
      isEnabled: () => this.HippoIframeService.isPageLoaded(),
      isVisible: () => PageService.hasActions('page'),
      onClick: () => this.onOpenMenu(),
      translationKey: 'TOOLBAR_BUTTON_PAGE',
    });

    function isEnabled(action) {
      return PageService.isActionEnabled('page', action);
    }

    function isVisible(action) {
      return PageService.hasAction('page', action);
    }

    menu
      .addAction('tools', {
        isVisible: () => PageToolsService.hasExtensions(),
        onClick: () => PageToolsService.showPageTools(),
        translationKey: 'TOOLBAR_MENU_PAGE_TOOLS',
      })
      .addAction('properties', {
        isEnabled: () => isEnabled('properties'),
        isVisible: () => isVisible('properties'),
        onClick: () => this.showSubPage('page-properties'),
        translationKey: 'TOOLBAR_MENU_PAGE_PROPERTIES',
      })
      .addDivider({
        isVisible: () => isVisible('properties') || this._hasPageExtensions(),
      })
      .addAction('copy', {
        isEnabled: () => isEnabled('copy'),
        isVisible: () => isVisible('copy'),
        onClick: () => this.showSubPage('page-copy'),
        translationKey: 'TOOLBAR_MENU_PAGE_COPY',
      })
      .addAction('move', {
        isEnabled: () => isEnabled('move'),
        isVisible: () => isVisible('move'),
        onClick: () => this.showSubPage('page-move'),
        translationKey: 'TOOLBAR_MENU_PAGE_MOVE',
      })
      .addAction('delete', {
        isEnabled: () => isEnabled('delete'),
        isVisible: () => isVisible('delete'),
        onClick: () => this._deletePage(),
        translationKey: 'TOOLBAR_MENU_PAGE_DELETE',
      })
      .addDivider({
        isVisible: () => isVisible('copy') || isVisible('move') || isVisible('delete'),
      })
      .addAction('new', {
        isEnabled: () => isEnabled('new'),
        isVisible: () => isVisible('new'),
        onClick: () => this.showSubPage('page-new'),
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
          })
          .then(() => this.ChannelService.checkChanges())
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
