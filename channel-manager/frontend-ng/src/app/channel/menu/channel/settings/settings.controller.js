/*
 * Copyright 2016-2023 Bloomreach (https://www.bloomreach.com)
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

import { v4 } from 'uuid';
import regenerateUrlDialogTemplate from './regenerateUrlDialog/regenerateUrlDialog.html';

class ChannelSettingsCtrl {
  constructor($element, $translate, FeedbackService, ChannelService, HippoIframeService, ConfigService, DialogService,
    ProjectService) {
    'ngInject';

    this.$element = $element;
    this.$translate = $translate;
    this.ConfigService = ConfigService;
    this.ChannelService = ChannelService;
    this.FeedbackService = FeedbackService;
    this.HippoIframeService = HippoIframeService;
    this.DialogService = DialogService;
    this.ProjectService = ProjectService;

    ChannelService.reload()
      .then(() => this._initialize());

    this.channelInfoDescription = {};
  }

  _initialize() {
    this.subpageTitle = this.$translate.instant('SUBPAGE_CHANNEL_SETTINGS_TITLE', {
      channelName: this.ChannelService.getName(),
    });

    this.projectsEnabled = this.ConfigService.projectsEnabled;

    this.ChannelService.getChannelInfoDescription()
      .then((channelInfoDescription) => {
        this.channelInfoDescription = channelInfoDescription;
        if (this.isLockedByOther()) {
          this.FeedbackService.showError(
            'ERROR_CHANNEL_SETTINGS_READONLY', { lockedBy: channelInfoDescription.lockedBy },
          );
        }
        if (!this.isEditable()) {
          this.FeedbackService.showError('ERROR_CHANNEL_SETTINGS_NOT_EDITABLE');
        }
      })
      .catch(() => {
        this.onError({ key: 'ERROR_CHANNEL_INFO_RETRIEVAL_FAILED' });
      });

    // We're making a copy in order not to mess with ChannelService state
    // in case we're going to cancel the action after changing some of the fields.
    this.values = angular.copy(this.ChannelService.getProperties());
    
    this._initializeExternalPreview();
  }

  _initializeExternalPreview() {
    const channel = this.ChannelService.getChannel();
    this.externalPreviewEnabled = channel.externalPreviewEnabled;
    this.externalPreviewToken = channel.externalPreviewToken == null ? '' : channel.externalPreviewToken;
    this.projectChannelPreviewURL = this.ChannelService.getProjectChannelPreviewURL(this.externalPreviewToken);
    this.deliveryAPIPreviewURL = this.ChannelService.getDeliveryApiPreviewURL(this.externalPreviewToken);
    this.projectName = this.ProjectService.selectedProject.name;
    this.$element.find('.url-input').on('mousedown', this._onMouseDown());

  }

  _onMouseDown() {
    return false; 
  }

  isLockedByOther() {
    return this.channelInfoDescription.lockedBy && this.channelInfoDescription.lockedBy !== this.ConfigService.cmsUser;
  }

  isEditable() {
    return this.channelInfoDescription.editable;
  }

  isReadOnly() {
    return this.isLockedByOther() || !this.isEditable() || this.ChannelService.isConfigurationLocked();
  }

  isSaveDisabled() {
    return this.isLockedByOther() || !this.isEditable();
  }

  touchRequiredFields() {
    if (this.form.$error.required) {
      this.form.$error.required.forEach(requiredField => requiredField.$setDirty());
    }
  }

  saveIfValid() {
    // Angular does not mark input containers with a select field as invalid unless they are dirty, so mark
    // all required field as dirty upon save to ensure that invalid required select fields are also highlighted.
    this.touchRequiredFields();

    if (this.form.$valid) {
      this.ChannelService.setProperties(this.values);
      this.ChannelService.setExternalPreviewProperties(this.externalPreviewEnabled,
        this.externalPreviewToken === '' ? null : this.externalPreviewToken);
      this.ChannelService.saveChannel()
        .then(() => {
          this.HippoIframeService.reload();
        })
        .then(() => this.ChannelService.checkChanges())
        .then(() => this.onSuccess({ key: 'CHANNEL_PROPERTIES_SAVE_SUCCESS' }))
        .catch((response) => {
          if (response != null && response.errorMessage) {
            this.FeedbackService.showError(response.errorMessage);
          } else {
            this.FeedbackService.showErrorResponse(response, 'ERROR_CHANNEL_PROPERTIES_SAVE_FAILED');
          }
        });
    }
  }

  onEnablePreview() {
    if (!this.externalPreviewToken) {
      this.onRegenerate('CHANNEL_SETTINGS_TOKEN_IS_CREATED_FIRST_TIME');
    }
  }

  onRegenerateURL() {
    return this.DialogService.show({
      template: regenerateUrlDialogTemplate,
      controller: () => ({
        onRegenerate: () => this.onRegenerate('REGENERATE_URL_CONFIRM_DIALOG_SUCCESS_MESSAGE'),
        onCancel: () => this.DialogService.hide()
      }),
      controllerAs: '$ctrl',
      bindToController: true
    });
  }

  onRegenerate(messageKey) {
    this.externalPreviewToken = v4();
    this.projectChannelPreviewURL = this.ChannelService.getProjectChannelPreviewURL(this.externalPreviewToken);
    this.deliveryAPIPreviewURL = this.ChannelService.getDeliveryApiPreviewURL(this.externalPreviewToken);
    this.FeedbackService.showNotification(this.$translate.instant(messageKey));
    this.DialogService.hide();
  }

  onCopyToClipboard(value) {
    navigator.clipboard.writeText(value)
      .then(() => this.FeedbackService.showDismissibleText(this.$translate.instant('COPY_TO_CLIPBOARD_SUCCESSFUL')))
      .catch(() => {
        this.FeedbackService.showDismissibleText(this.$translate.instant('COPY_TO_CLIPBOARD_FAILED'))
      });
  }

  getLabel(field) {
    return this.channelInfoDescription.i18nResources[field] || field;
  }

  getFieldGroups() {
    return this.channelInfoDescription.fieldGroups;
  }

  getUngroupedFields() {
    if (!this.channelInfoDescription.propertyDefinitions) {
      return [];
    }

    return Object.keys(this.channelInfoDescription.propertyDefinitions).sort((fieldA, fieldB) => {
      const labelA = this.getLabel(fieldA);
      const labelB = this.getLabel(fieldB);
      return labelA.localeCompare(labelB);
    });
  }
}

export default ChannelSettingsCtrl;
