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

export class ChannelSettingsCtrl {
  constructor($element, $translate, FeedbackService, ChannelService, HippoIframeService, ConfigService) {
    'ngInject';

    this.ChannelService = ChannelService;
    this.FeedbackService = FeedbackService;
    this.HippoIframeService = HippoIframeService;

    this.feedbackParent = $element.find('.feedback-parent');

    this.subpageTitle = $translate.instant('SUBPAGE_CHANNEL_SETTINGS_TITLE', {
      channelName: ChannelService.getName(),
    });

    this.channelInfoDescription = {};
    ChannelService.getChannelInfoDescription()
      .then((channelInfoDescription) => {
        this.channelInfoDescription = channelInfoDescription;
        if (channelInfoDescription.lockedBy && channelInfoDescription.lockedBy !== ConfigService.cmsUser) {
          this.readOnlyAlert = $translate.instant('SUBPAGE_CHANNEL_SETTINGS_READONLY_ALERT', {
            lockedBy: channelInfoDescription.lockedBy,
          });
        }
      })
      .catch(() => {
        this.onError({ key: 'ERROR_CHANNEL_INFO_RETRIEVAL_FAILED' });
      });

    // We're making a copy in order not to mess with ChannelService state
    // in case we're going to cancel the action after changing some of the fields.
    this.values = angular.copy(ChannelService.getProperties());
  }

  save() {
    this.ChannelService.setProperties(this.values);
    this.ChannelService.saveChannel()
      .then(() => {
        this.HippoIframeService.reload();
        this.ChannelService.recordOwnChange();
        this.onSuccess({ key: 'CHANNEL_PROPERTIES_SAVE_SUCCESS' });
      })
      .catch(() => {
        this._showError('ERROR_CHANNEL_PROPERTIES_SAVE_FAILED');
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

  _showError(key, params) {
    this.FeedbackService.showError(key, params, this.feedbackParent);
  }
}
