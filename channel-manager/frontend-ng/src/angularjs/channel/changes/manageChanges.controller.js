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
    this.selectedUsers = [];
    this.suffixYou = $translate.instant('SUBPAGE_CHANNELMANAGEMENT_SUFFIX_YOU');
  }

  selectAll() {
    this.usersWithChanges.forEach((user) => {
      if (!this.isChecked(user)) {
        this._checkUser(user);
      }
    });
  }

  selectNone() {
    this.usersWithChanges.forEach((user) => {
      if (this.isChecked(user)) {
        this._uncheckUser(user);
      }
    });
  }

  getLabel(user) {
    let label = user;
    if (user === this.ConfigService.cmsUser) {
      label += this.suffixYou;
    }
    return label;
  }

  toggle(user) {
    if (this.selectedUsers.includes(user)) {
      this._uncheckUser(user);
    } else {
      this._checkUser(user);
    }
  }

  isChecked(user) {
    return this.selectedUsers.includes(user);
  }

  isNoneSelected() {
    return this.selectedUsers.length === 0;
  }

  publishSelectedChanges() {
    this.ChannelService.publishChanges(this.selectedUsers)
      .then(() => this._resetSelection())
      .catch((response) => {
        // response might be undefined or null (for example when the network connection is lost)
        response = response || {};

        this.$log.info(response.message);
        this.FeedbackService.showErrorOnSubpage('ERROR_CHANGE_PUBLICATION_FAILED', response.data);
      });
  }

  discardSelectedChanges() {
    this._confirmDiscard().then(() => {
      this.ChannelService.discardChanges(this.selectedUsers)
        .then(() => this._resetSelection())
        .catch((response) => {
          // response might be undefined or null (for example when the network connection is lost)
          response = response || {};

          this.$log.info(response.message);
          this.FeedbackService.showErrorOnSubpage('ERROR_CHANGE_DISCARD_FAILED', response.data);
        });
    });
  }

  _confirmDiscard() {
    const confirm = this.DialogService.confirm()
      .title(this.$translate.instant('CONFIRM_DISCARD_CHANGES_TITLE'))
      .textContent(this.$translate.instant('CONFIRM_DISCARD_SELECTED_CHANGES_MESSAGE'))
      .ok(this.$translate.instant('BUTTON_YES'))
      .cancel(this.$translate.instant('BUTTON_NO'));

    return this.DialogService.show(confirm);
  }

  _resetSelection() {
    this.selectedUsers = [];
    this.CmsService.publish('channel-changed-in-angular');
    this.HippoIframeService.reload();
    this.onDone();
  }

  _checkUser(user) {
    this.selectedUsers.push(user);
  }

  _uncheckUser(user) {
    const index = this.selectedUsers.indexOf(user);
    this.selectedUsers.splice(index, 1);
  }
}
