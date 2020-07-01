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
import deleteProgressTemplate from './delete/delete-channel-progress.html';
import rejectPromptTemplate from './rejectPrompt/reject-prompt.html';

class ChannelMenuService extends MenuService {
  constructor(
    $log,
    $q,
    $state,
    $translate,
    ChannelService,
    CmsService,
    ConfigService,
    DialogService,
    EditComponentService,
    FeedbackService,
    HippoIframeService,
    PageService,
    SessionService,
    SiteMapService,
    ProjectService,
  ) {
    'ngInject';

    super();

    this.$log = $log;
    this.$q = $q;
    this.$state = $state;
    this.$translate = $translate;
    this.ChannelService = ChannelService;
    this.CmsService = CmsService;
    this.ConfigService = ConfigService;
    this.DialogService = DialogService;
    this.EditComponentService = EditComponentService;
    this.FeedbackService = FeedbackService;
    this.HippoIframeService = HippoIframeService;
    this.SessionService = SessionService;
    this.SiteMapService = SiteMapService;
    this.ProjectService = ProjectService;

    const menu = this.defineMenu('channel', {
      iconName: 'mdi-alert',
      isIconVisible: () => this._hasAnyChanges(),
      isVisible: () => PageService.hasActions('channel'),
      translationKey: 'TOOLBAR_BUTTON_CHANNEL',
    });

    function isEnabled(action) {
      return PageService.isActionEnabled('channel', action);
    }

    function isVisible(action) {
      return PageService.hasAction('channel', action);
    }

    menu
      .addAction('settings', {
        iconName: 'mdi-settings',
        isEnabled: () => isEnabled('settings'),
        isVisible: () => isVisible('settings'),
        onClick: () => this._showChannelSettings(),
        translationKey: 'TOOLBAR_MENU_CHANNEL_SETTINGS',
      })
      .addDivider({
        isVisible: () => isVisible('settings'),
      })
      .addAction('publish', {
        iconName: 'mdi-publish',
        isEnabled: () => this._hasOwnChanges(),
        isVisible: () => isVisible('publish') && !this._isBranch(),
        onClick: () => this._publish(),
        translationKey: 'TOOLBAR_MENU_CHANNEL_PUBLISH',
      })
      .addAction('confirm', {
        iconName: 'mdi-publish',
        isEnabled: () => this._hasOwnChanges(),
        isVisible: () => isVisible('confirm') && this._isBranch(),
        onClick: () => this._publish(),
        translationKey: 'TOOLBAR_MENU_CHANNEL_SUBMIT',
      })
      .addAction('discard-changes', {
        isEnabled: () => this._hasOwnChanges(),
        isVisible: () => isVisible('discard-changes'),
        onClick: () => this._discardChanges(),
        translationKey: 'TOOLBAR_MENU_CHANNEL_DISCARD_CHANGES',
      })
      .addAction('manage-changes', {
        isEnabled: () => isEnabled('manage-changes') && this._hasChanges() && !this._hasOnlyOwnChanges(),
        isVisible: () => isVisible('manage-changes'),
        onClick: () => this._showManageChanges(),
        translationKey: 'TOOLBAR_MENU_CHANNEL_MANAGE_CHANGES',
      })
      .addDivider({
        isVisible: () => this._isBranch() && (isVisible('accept') || isVisible('reject')),
      })
      .addAction('accept', {
        iconName: 'mdi-check',
        isEnabled: () => this.ProjectService.isAcceptEnabled(),
        isVisible: () => isVisible('accept') && this._isBranch(),
        onClick: () => this._accept(),
        translationKey: 'TOOLBAR_MENU_CHANNEL_ACCEPT',
      })
      .addAction('reject', {
        iconName: 'mdi-close',
        isEnabled: () => this.ProjectService.isRejectEnabled(),
        isVisible: () => isVisible('reject') && this._isBranch(),
        onClick: () => this._reject(),
        translationKey: 'TOOLBAR_MENU_CHANNEL_REJECT',
      })
      .addDivider({
        isVisible: () => isVisible('delete'),
      })
      .addAction('delete', {
        iconName: 'mdi-delete',
        isEnabled: () => isEnabled('delete'),
        isVisible: () => isVisible('delete'),
        onClick: () => this._deleteChannel(),
        translationKey: 'TOOLBAR_MENU_CHANNEL_DELETE',
      })
      .addAction('close', {
        onClick: () => this._closeChannel(),
        translationKey: 'TOOLBAR_MENU_CHANNEL_CLOSE',
      });

    this.CmsService.subscribe('close-channel', () => this._closeChannel());
  }

  _isBranch() {
    return this.ProjectService.isBranch();
  }

  // Settings
  _showChannelSettings() {
    this.showSubPage('channel-settings');
  }

  // Changes
  _showManageChanges() {
    this._closeEditors().then(() => this.showSubPage('manage-changes'));
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

  _hasChanges() {
    return this._getChangedBySet().length > 0;
  }

  _getChangedBySet() {
    return this.ChannelService.getChannel().changedBySet || [];
  }

  _publish() {
    this._closeEditors()
      .then(() => this._executePublish());
  }

  _closeEditors() {
    return this.EditComponentService.stopEditing();
  }

  _executePublish() {
    return this.ChannelService.publishOwnChanges()
      .then(() => this._onChannelChanged())
      .catch(response => this._handleError('ERROR_CHANGE_PUBLICATION_FAILED', response));
  }

  _onChannelChanged() {
    this.CmsService.publish('channel-changed-in-angular');
    this.HippoIframeService.reload();
  }

  _handleError(errorKey, error = {}) {
    if (error.message) {
      this.$log.error(error.message);
    }
    this.FeedbackService.showErrorResponse(error, errorKey);
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
    this._closeEditors()
      .then(() => this._confirmDiscardChanges())
      .then(() => this._executeDiscardChanges());
  }

  _confirmDiscardChanges() {
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

  _executeDiscardChanges() {
    return this.ChannelService.discardOwnChanges()
      .then(() => {
        this._onChannelChanged();
        this.SiteMapService.load(this.ChannelService.getSiteMapId());
      })
      .catch(response => this._handleError('ERROR_CHANGE_DISCARD_FAILED', response));
  }

  _deleteChannel() {
    this._closeEditors()
      .then(() => this._confirmDeleteChannel())
      .then(() => this._executeDeleteChannel());
  }

  _executeDeleteChannel() {
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
  }

  _confirmDeleteChannel() {
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
