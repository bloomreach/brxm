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

export class ChangesMenuCtrl {
  constructor(
      $log,
      $translate,
      ChannelService,
      ConfigService,
      CmsService,
      DialogService,
      FeedbackService,
      HippoIframeService,
      SiteMapService
    ) {
    'ngInject';

    this.$log = $log;
    this.$translate = $translate;
    this.ChannelService = ChannelService;
    this.CmsService = CmsService;
    this.ConfigService = ConfigService;
    this.DialogService = DialogService;
    this.FeedbackService = FeedbackService;
    this.HippoIframeService = HippoIframeService;
    this.SiteMapService = SiteMapService;

    this.canManageChanges = ConfigService.canManageChanges;
  }

  _getChangedBySet() {
    return this.ChannelService.getChannel().changedBySet;
  }

  hasChangesToManage() {
    return this.canManageChanges && this._getChangedBySet().length > 0;
  }

  hasOwnChanges() {
    return this._getChangedBySet().indexOf(this.ConfigService.cmsUser) !== -1;
  }

  hasOnlyOwnChanges() {
    return this.hasOwnChanges() && this._getChangedBySet().length === 1;
  }

  isShowChangesMenu() {
    return this.hasChangesToManage() || this.hasOwnChanges();
  }

  isManageChangesEnabled() {
    return this.hasChangesToManage() && !this.hasOnlyOwnChanges();
  }

  publish() {
    this.ChannelService.publishChanges()
      .then(() => {
        this.CmsService.publish('channel-changed-in-angular');
        this.HippoIframeService.reload();
      })
      .catch((response) => {
        response = response || { };

        this.$log.info(response.message);
        this.FeedbackService.showError('ERROR_CHANGE_PUBLICATION_FAILED', response.data);
      });
  }

  discard() {
    this._confirmDiscard().then(() => {
      this.ChannelService.discardChanges()
        .then(() => {
          this.CmsService.publish('channel-changed-in-angular');
          this.HippoIframeService.reload();
          this.SiteMapService.load(this.ChannelService.getSiteMapId());
        })
        .catch((response) => {
          response = response || { };

          this.$log.info(response.message);
          this.FeedbackService.showError('ERROR_CHANGE_DISCARD_FAILED', response.data);
        });
    });
  }

  _confirmDiscard() {
    const confirm = this.DialogService.confirm()
      .title(this.$translate.instant('CONFIRM_DISCARD_CHANGES_TITLE'))
      .textContent(this.$translate.instant('CONFIRM_DISCARD_OWN_CHANGES_MESSAGE'))
      .ok(this.$translate.instant('BUTTON_YES'))
      .cancel(this.$translate.instant('BUTTON_NO'));

    return this.DialogService.show(confirm);
  }
}
