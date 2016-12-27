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

import deleteProgressTemplate from './delete/delete-channel-progress.html';

class ChannelActionsCtrl {
  constructor($translate, ChannelService, CmsService, DialogService, FeedbackService, SessionService) {
    'ngInject';

    this.$translate = $translate;
    this.ChannelService = ChannelService;
    this.CmsService = CmsService;
    this.DialogService = DialogService;
    this.FeedbackService = FeedbackService;
    this.SessionService = SessionService;
  }

  isChannelSettingsAvailable() {
    return this.ChannelService.getChannel().hasCustomProperties;
  }

  isChannelDeletionAvailable() {
    return this.SessionService.canDeleteChannel();
  }

  hasMenuOptions() {
    return this.isChannelSettingsAvailable() || this.isChannelDeletionAvailable();
  }

  openSettings() {
    this.onActionSelected({ subpage: 'channel-settings' });
  }

  deleteChannel() {
    this._confirmDelete()
      .then(() => {
        this._showDeleteProgress();
        this.ChannelService.deleteChannel()
          .then(() => {
            this.CmsService.subscribeOnce('channel-removed-from-overview', () => this._hideDeleteProgress());
            this.CmsService.publish('channel-deleted');
          })
          .catch((response) => {
            this._hideDeleteProgress();
            if (response && response.error === 'CHILD_MOUNT_EXISTS') {
              this._showElaborateFeedback(response, 'ERROR_CHANNEL_DELETE_FAILED_DUE_TO_CHILD_MOUNTS');
            } else {
              this.FeedbackService.showErrorResponse(response, 'ERROR_CHANNEL_DELETE_FAILED');
            }
          });
      });
  }

  _confirmDelete() {
    const confirm = this.DialogService.confirm()
      .title(this.$translate.instant('CONFIRM_DELETE_CHANNEL_TITLE', { channel: this.ChannelService.getName() }))
      .textContent(this.$translate.instant('CONFIRM_DELETE_CHANNEL_MESSAGE'))
      .ok(this.$translate.instant('DELETE'))
      .cancel(this.$translate.instant('CANCEL'));

    return this.DialogService.show(confirm);
  }

  _showDeleteProgress() {
    this.DialogService.show({
      template: deleteProgressTemplate,
      locals: {
        translationData: {
          channel: this.ChannelService.getName(),
        },
      },
      controller: ($scope, translationData) => {
        'ngInject';

        $scope.translationData = translationData;
      },
    });
  }

  _hideDeleteProgress() {
    this.DialogService.hide();
  }

  _showElaborateFeedback(response, key) {
    const alert = this.DialogService.alert()
      .title(this.$translate.instant('ERROR_CHANNEL_DELETE_FAILED'))
      .textContent(this.$translate.instant(key, response.parameterMap))
      .ok(this.$translate.instant('OK'));

    this.DialogService.show(alert);
  }
}

export default ChannelActionsCtrl;
