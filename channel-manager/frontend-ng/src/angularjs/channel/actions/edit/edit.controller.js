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

export class ChannelEditCtrl {
  constructor($element, $translate, FeedbackService, ChannelService, HippoIframeService) {
    'ngInject';
    this.ChannelService = ChannelService;
    this.FeedbackService = FeedbackService;
    this.HippoIframeService = HippoIframeService;

    this.feedbackParent = $element.find('.feedback-parent');

    this.pageTitle = $translate.instant('SUBPAGE_CHANNEL_EDIT_TITLE', {
      channelName: ChannelService.getName(),
    });

    ChannelService.getChannelInfoDescription()
      .then((channelInfoDescription) => {
        this.fieldGroups = channelInfoDescription.fieldGroups;
        this.labels = channelInfoDescription.i18nResources;
      })
      .catch(() => {
        this.onError({ key: 'ERROR_CHANNEL_INFO_RETRIEVAL_FAILED' });
      });

    this.values = ChannelService.getChannel().properties;
  }

  save() {
    this.ChannelService.saveProperties(this.values)
      .then(() => {
        this.HippoIframeService.reload();
        this.ChannelService.recordOwnChange();
        this.onSuccess({ key: 'CHANNEL_PROPERTIES_SAVE_SUCCESS' });
      })
      .catch(() => {
        this._showError('ERROR_CHANNEL_PROPERTIES_SAVE_FAILED');
      });
  }

  getLabel(fieldName) {
    return this.labels[fieldName];
  }

  _showError(key, params) {
    this.FeedbackService.showError(key, params, this.feedbackParent);
  }

  back() {
    this.onDone();
  }
}
