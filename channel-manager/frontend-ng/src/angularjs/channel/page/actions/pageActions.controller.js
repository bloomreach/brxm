/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

export class PageActionsCtrl {
  constructor($translate, FeedbackService, ChannelService, SiteMapService, SiteMapItemService, DialogService,
              HippoIframeService, PageMetaDataService) {
    'ngInject';

    this.$translate = $translate;
    this.FeedbackService = FeedbackService;
    this.ChannelService = ChannelService;
    this.SiteMapService = SiteMapService;
    this.SiteMapItemService = SiteMapItemService;
    this.DialogService = DialogService;
    this.HippoIframeService = HippoIframeService;
    this.PageMetaDataService = PageMetaDataService;

    this.actions = [];

    ['edit', 'add', 'delete', 'move', 'copy']
      .forEach((id) => {
        this.actions.push({
          id,
          label: $translate.instant(`TOOLBAR_MENU_PAGES_${id.toUpperCase()}`),
          isEnabled: () => false,
        });
      });

    this._findAction('add').isEnabled = () => ChannelService.hasWorkspace() && ChannelService.hasPrototypes();
    this._findAction('delete').isEnabled = () => SiteMapItemService.isEditable();
    this._findAction('edit').isEnabled = () => SiteMapItemService.hasItem(); // TODO TBD
  }

  _findAction(id) {
    return this.actions.find((action) => action.id === id);
  }

  onOpenMenu() {
    this.SiteMapItemService.loadAndCache(this.ChannelService.getSiteMapId(), this.PageMetaDataService.getSiteMapItemId());
  }

  trigger(action) {
    if (action.id === 'delete') {
      this._deletePage();
    } else {
      this.onActionSelected({ subpage: `page-${action.id}` });
    }
  }

  _deletePage() {
    this._confirmDelete()
      .then(() => {
        this.SiteMapItemService.deleteItem()
          .then(() => {
            const siteMapId = this.ChannelService.getSiteMapId();

            this.HippoIframeService.load('');      // load homepage
            this.SiteMapService.load(siteMapId);   // reload sitemap (sidenav)
            this.SiteMapItemService.clear();       // wipe meta-data of current page
            this.ChannelService.recordOwnChange(); // mark the channel changed
          })
          .catch(() => {
            this.FeedbackService.showError('ERROR_DELETE_PAGE');
          });
      });
      // do nothing on cancel
  }

  _confirmDelete() {
    const confirm = this.DialogService.confirm()
      .title(this.$translate.instant('CONFIRM_DELETE_PAGE_TITLE'))
      .textContent(this.$translate.instant('CONFIRM_DELETE_PAGE_MESSAGE'))
      .ok(this.$translate.instant('BUTTON_YES'))
      .cancel(this.$translate.instant('BUTTON_NO'));

    return this.DialogService.show(confirm);
  }
}
