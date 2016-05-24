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

export class ChangeManagementCtrl {
  constructor(
      $log,
      $translate,
      ChannelService,
      CmsService,
      ConfigService,
      DialogService,
      FeedbackService,
      HippoIframeService
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

    this.usersWithChanges = ChannelService.getChannel().changedBySet.sort();
    this.suffixYou = $translate.instant('SUBPAGE_CHANGEMANAGEMENT_SUFFIX_YOU');
    this.hasManagedChanges = false;
  }

  getLabel(user) {
    let label = user;
    if (user === this.ConfigService.cmsUser) {
      label += this.suffixYou;
    }
    return label;
  }

  publishChanges(user) {
    this.ChannelService.publishChanges([user])
      .then(() => this._onSuccess())
      .catch((response) => this._showError('ERROR_CHANGE_PUBLICATION_FAILED', response));
  }

  discardChanges(user) {
    this._confirmDiscard(user).then(() => {
      this.ChannelService.discardChanges([user])
        .then(() => this._onSuccess())
        .catch((response) => this._showError('ERROR_CHANGE_DISCARD_FAILED', response));
    });
  }

  publishAllChanges() {
    this._confirmPublish().then(() => {
      this.ChannelService.publishChanges(this.usersWithChanges)
        .then(() => this._onSuccess())
        .catch((response) => this._showError('ERROR_CHANGE_PUBLICATION_FAILED', response));
    });
  }

  discardAllChanges() {
    this._confirmDiscard().then(() => {
      this.ChannelService.discardChanges(this.usersWithChanges)
        .then(() => this._onSuccess())
        .catch((response) => this._showError('ERROR_CHANGE_DISCARD_FAILED', response));
    });
  }

  goBack() {
    if (this.hasManagedChanges) {
      this.CmsService.publish('channel-changed-in-angular');
      this.HippoIframeService.reload();
      this.hasManagedChanges = false;
    }

    this.onDone();
  }

  _onSuccess() {
    this.hasManagedChanges = true;
    this.usersWithChanges = this.ChannelService.getChannel().changedBySet.sort();

    if (!this.usersWithChanges.length) {
      this.goBack();
    }
  }

  _confirmDiscard(user) {
    const message = user ? 'CONFIRM_DISCARD_CHANGES_MESSAGE' : 'CONFIRM_DISCARD_ALL_CHANGES_MESSAGE';
    const confirm = this.DialogService.confirm()
      .textContent(this.$translate.instant(message, { user }))
      .ok(this.$translate.instant('BUTTON_DISCARD'))
      .cancel(this.$translate.instant('BUTTON_CANCEL'));

    return this.DialogService.show(confirm);
  }

  _confirmPublish() {
    const confirm = this.DialogService.confirm()
      .textContent(this.$translate.instant('CONFIRM_PUBLISH_ALL_CHANGES_MESSAGE'))
      .ok(this.$translate.instant('BUTTON_PUBLISH'))
      .cancel(this.$translate.instant('BUTTON_CANCEL'));

    return this.DialogService.show(confirm);
  }

  _showError(key, response) {
    // response might be undefined or null (for example when the network connection is lost)
    response = response || {};

    this.$log.info(response.message);
    this.FeedbackService.showError(key, response.data, this.feedbackParent);
  }
}
