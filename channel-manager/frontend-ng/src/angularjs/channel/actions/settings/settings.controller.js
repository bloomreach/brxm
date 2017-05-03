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

class ChannelSettingsCtrl {
  constructor($translate, FeedbackService, ChannelService, HippoIframeService, ConfigService) {
    'ngInject';

    this.$translate = $translate;
    this.ConfigService = ConfigService;
    this.ChannelService = ChannelService;
    this.FeedbackService = FeedbackService;
    this.HippoIframeService = HippoIframeService;

    ChannelService.reload()
      .then(() => this._initialize());

    this.channelInfoDescription = {};
  }

  _initialize() {
    this.subpageTitle = this.$translate.instant('SUBPAGE_CHANNEL_SETTINGS_TITLE', {
      channelName: this.ChannelService.getName(),
    });

    this.ChannelService.getChannelInfoDescription()
      .then((channelInfoDescription) => {
        this.channelInfoDescription = channelInfoDescription;
        if (this.isLockedByOther()) {
          this.FeedbackService.showErrorOnSubpage('ERROR_CHANNEL_SETTINGS_READONLY', { lockedBy: channelInfoDescription.lockedBy });
        }
        if (!this.isEditable()) {
          this.FeedbackService.showErrorOnSubpage('ERROR_CHANNEL_SETTINGS_NOT_EDITABLE');
        }
      })
      .catch(() => {
        this.onError({ key: 'ERROR_CHANNEL_INFO_RETRIEVAL_FAILED' });
      });

    // We're making a copy in order not to mess with ChannelService state
    // in case we're going to cancel the action after changing some of the fields.
    this.values = angular.copy(this.ChannelService.getProperties());
  }

  isLockedByOther() {
    return this.channelInfoDescription.lockedBy && this.channelInfoDescription.lockedBy !== this.ConfigService.cmsUser;
  }
  isEditable() {
    return this.channelInfoDescription.editable;
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
      this.ChannelService.saveChannel()
        .then(() => {
          this.HippoIframeService.reload();
          this.ChannelService.recordOwnChange();
          this.onSuccess({ key: 'CHANNEL_PROPERTIES_SAVE_SUCCESS' });
        })
        .catch((response) => {
          this.FeedbackService.showErrorResponseOnSubpage(response, 'ERROR_CHANNEL_PROPERTIES_SAVE_FAILED');
        });
    }
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
