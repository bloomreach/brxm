/*
 * Copyright 2017-2019 Hippo B.V. (http://www.onehippo.com)
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
import deleteProgressTemplate from './delete/delete-channel-progress.html';
import rejectPromptTemplate from './rejectPrompt/reject-prompt.html';

class ChannelMenuService extends MenuService {
  constructor(
    $log,
    $state,
    $translate,
    ChannelService,
    CmsService,
    ConfigService,
    DialogService,
    FeedbackService,
    HippoIframeService,
    SessionService,
    SiteMapService,
    ProjectService,
  ) {
    'ngInject';

    super();

    this.$log = $log;
    this.$state = $state;
    this.$translate = $translate;
    this.ChannelService = ChannelService;
    this.CmsService = CmsService;
    this.ConfigService = ConfigService;
    this.DialogService = DialogService;
    this.FeedbackService = FeedbackService;
    this.HippoIframeService = HippoIframeService;
    this.SessionService = SessionService;
    this.SiteMapService = SiteMapService;
    this.ProjectService = ProjectService;

    this.defineMenu('channel', {
      translationKey: 'TOOLBAR_BUTTON_CHANNEL',
      isIconVisible: () => this._hasAnyChanges(),
      iconName: 'mdi-alert',
    });

    if (this._hasWriteAccess()) {
      this.addAction('settings', {
        translationKey: 'TOOLBAR_MENU_CHANNEL_SETTINGS',
        iconName: 'mdi-settings',
        isEnabled: () => this._isChannelSettingsAvailable(),
        onClick: () => this._showChannelSettings(),
      })
        .addDivider()
        .addAction('publish', {
          translationKey: 'TOOLBAR_MENU_CHANNEL_PUBLISH',
          iconName: 'mdi-publish',
          isVisible: () => !this._isBranch(),
          isEnabled: () => this._hasOwnChanges(),
          onClick: () => this._publish(),
        })
        .addAction('confirm', {
          translationKey: 'TOOLBAR_MENU_CHANNEL_SUBMIT',
          iconName: 'mdi-publish',
          isVisible: () => this._isBranch(),
          isEnabled: () => this._hasOwnChanges(),
          onClick: () => this._publish(),
        })
        .addAction('discard-changes', {
          translationKey: 'TOOLBAR_MENU_CHANNEL_DISCARD_CHANGES',
          isEnabled: () => this._hasOwnChanges(),
          onClick: () => this._discardChanges(),
        })
        .addAction('manage-changes', {
          translationKey: 'TOOLBAR_MENU_CHANNEL_MANAGE_CHANGES',
          isEnabled: () => this._hasChangesToManage() && !this._hasOnlyOwnChanges(),
          onClick: () => this._showManageChanges(),
        })
        .addDivider({
          isVisible: () => this._isBranch(),
        })
        .addAction('accept', {
          translationKey: 'TOOLBAR_MENU_CHANNEL_ACCEPT',
          iconName: 'mdi-check',
          isVisible: () => this._isBranch(),
          isEnabled: () => this.ProjectService.isAcceptEnabled(),
          onClick: () => this._accept(),
        })
        .addAction('reject', {
          translationKey: 'TOOLBAR_MENU_CHANNEL_REJECT',
          iconName: 'mdi-close',
          isVisible: () => this._isBranch(),
          isEnabled: () => this.ProjectService.isRejectEnabled(),
          onClick: () => this._reject(),
        })
        .addDivider()
        .addAction('delete', {
          translationKey: 'TOOLBAR_MENU_CHANNEL_DELETE',
          iconName: 'mdi-delete',
          isEnabled: () => this._isChannelDeletionAvailable(),
          onClick: () => this._deleteChannel(),
        });
    }

    this.addAction('close', {
      translationKey: 'TOOLBAR_MENU_CHANNEL_CLOSE',
      onClick: () => this._closeChannel(),
    });

    this.CmsService.subscribe('close-channel', () => this._closeChannel());
  }

  _isBranch() {
    return this.ProjectService.isBranch();
  }

  _hasWriteAccess() {
    return this.SessionService.hasWriteAccess();
  }

  // Settings
  _showChannelSettings() {
    this.showSubPage('channel-settings');
  }

  _isChannelSettingsAvailable() {
    return this.ChannelService.isEditable() &&
           this.ChannelService.getChannel().hasCustomProperties;
  }

  // Changes
  _showManageChanges() {
    this.showSubPage('manage-changes');
  }

  _hasAnyChanges() {
    return this._hasOwnChanges() || this._hasChangesToManage();
  }

  _hasOwnChanges() {
    return this._getChangedBySet().indexOf(this.ConfigService.cmsUser) !== -1;
  }

  _hasOnlyOwnChanges() {
    return this._hasOwnChanges() && this._getChangedBySet().length === 1;
  }

  _canManageChanges() {
    return this.SessionService.canManageChanges();
  }

  _hasChangesToManage() {
    return this._canManageChanges() && this._getChangedBySet().length > 0;
  }

  _getChangedBySet() {
    return this.ChannelService.getChannel().changedBySet || [];
  }

  _publish() {
    this.ChannelService.publishOwnChanges()
      .then(() => {
        this.CmsService.publish('channel-changed-in-angular');
        this.HippoIframeService.reload();
      })
      .catch((response) => {
        response = response || {};

        this.$log.info(response.message);
        this.FeedbackService.showErrorResponse(response, 'ERROR_CHANGE_PUBLICATION_FAILED');
      });
  }

  _reject() {
    const channel = this.ChannelService.getChannel();
    const channelId = channel.id.replace(/-preview$/, '');
    this.CmsService.reportUsageStatistic('RejectChannelChannelManager');

    this._showRejectPrompt(channel)
      .then((message) => {
        this.CmsService.reportUsageStatistic('RejectChannelCMDialogueOKButton');
        this.ProjectService.reject(channelId, message);
      });
  }

  _accept() {
    this.CmsService.reportUsageStatistic('AcceptChannelChannelManager');
    this.ProjectService.accept([this.ChannelService.getId().replace('-preview', '')]);
  }

  _discardChanges() {
    this._confirmDiscard().then(() => {
      this.ChannelService.discardOwnChanges()
        .then(() => {
          this.CmsService.publish('channel-changed-in-angular');
          this.HippoIframeService.reload();
          this.SiteMapService.load(this.ChannelService.getSiteMapId());
        })
        .catch((response) => {
          response = response || {};

          this.$log.info(response.message);
          this.FeedbackService.showErrorResponse(response, 'ERROR_CHANGE_DISCARD_FAILED');
        });
    });
  }

  _confirmDiscard() {
    const channel = this.ChannelService.getChannel();
    let content = this.$translate.instant('CONFIRM_DISCARD_OWN_CHANGES_MESSAGE', { channelName: channel.name });

    if (this._isBranch()) {
      const project = this.ProjectService.selectedProject;

      content = this.$translate.instant('CONFIRM_DISCARD_OWN_CHANGES_IN_PROJECT_MESSAGE', {
        channelName: channel.name,
        projectName: project.name,
      });
    }

    const confirm = this.DialogService.confirm()
      .textContent(content)
      .ok(this.$translate.instant('DISCARD'))
      .cancel(this.$translate.instant('CANCEL'));

    return this.DialogService.show(confirm);
  }

  // Delete
  _isChannelDeletionAvailable() {
    return this.SessionService.canDeleteChannel();
  }

  _deleteChannel() {
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
      .title(this.$translate.instant('CONFIRM_DELETE_CHANNEL_TITLE', {
        channel: this.ChannelService.getName(),
      }))
      .textContent(this.$translate.instant('CONFIRM_DELETE_CHANNEL_MESSAGE'))
      .ok(this.$translate.instant('DELETE'))
      .cancel(this.$translate.instant('CANCEL'));

    return this.DialogService.show(confirm);
  }

  _showRejectPrompt(channel) {
    return this.DialogService.show({
      template: rejectPromptTemplate,
      locals: {
        translationData: {
          channelName: channel.name,
        },
      },
      controller: 'RejectPromptCtrl',
      controllerAs: '$ctrl',
      bindToController: true,
    });
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

  // Close
  _closeChannel() {
    this.$state.go('hippo-cm')
      .then(() => {
        this.ChannelService.clearChannel();
        this.CmsService.publish('close-channel');
      });
  }
}

export default ChannelMenuService;
