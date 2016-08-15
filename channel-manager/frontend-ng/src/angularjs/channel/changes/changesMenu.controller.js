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
      SessionService,
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
    this.SessionService = SessionService;
    this.SiteMapService = SiteMapService;
  }

  _getChangedBySet() {
    return this.ChannelService.getChannel().changedBySet || [];
  }

  _hasChangesToManage() {
    return this.canManageChanges() && this._getChangedBySet().length > 0;
  }

  canManageChanges() {
    return this.SessionService.canManageChanges();
  }

  hasOwnChanges() {
    return this._getChangedBySet().indexOf(this.ConfigService.cmsUser) !== -1;
  }

  _hasOnlyOwnChanges() {
    return this.hasOwnChanges() && this._getChangedBySet().length === 1;
  }

  isShowChangesMenu() {
    return this._hasChangesToManage() || this.hasOwnChanges();
  }

  isManageChangesEnabled() {
    return this._hasChangesToManage() && !this._hasOnlyOwnChanges();
  }

  publish() {
    this.ChannelService.publishOwnChanges()
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
      this.ChannelService.discardOwnChanges()
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
      .textContent(this.$translate.instant('CONFIRM_DISCARD_OWN_CHANGES_MESSAGE'))
      .ok(this.$translate.instant('DISCARD'))
      .cancel(this.$translate.instant('CANCEL'));

    return this.DialogService.show(confirm);
  }
}
