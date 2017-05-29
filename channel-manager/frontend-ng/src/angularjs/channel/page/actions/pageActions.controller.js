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

class PageActionsCtrl {
  constructor($translate, FeedbackService, ChannelService, SiteMapService, SiteMapItemService, DialogService,
    HippoIframeService, PageMetaDataService, SessionService) {
    'ngInject';

    this.$translate = $translate;
    this.FeedbackService = FeedbackService;
    this.ChannelService = ChannelService;
    this.SessionService = SessionService;
    this.SiteMapService = SiteMapService;
    this.SiteMapItemService = SiteMapItemService;
    this.DialogService = DialogService;
    this.HippoIframeService = HippoIframeService;
    this.PageMetaDataService = PageMetaDataService;
  }

  isPageEditable() {
    return this.SiteMapItemService.isEditable();
  }

  isCopyEnabled() {
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

  isNewEnabled() {
    return this.ChannelService.hasWorkspace() && this.ChannelService.hasPrototypes();
  }

  onOpenMenu() {
    this.SiteMapItemService.loadAndCache(this.ChannelService.getSiteMapId(), this.PageMetaDataService.getSiteMapItemId());
    this.ChannelService.loadPageModifiableChannels();
  }

  openSubPage(subpage) {
    this.onActionSelected({ subpage });
  }

  deletePage() {
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

export default PageActionsCtrl;
