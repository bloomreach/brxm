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
  constructor(CmsService, ChannelService, HippoIframeService, HstService) {
    'ngInject';

    this.ChannelService = ChannelService;
    this.CmsService = CmsService;
    this.HippoIframeService = HippoIframeService;
    this.HstService = HstService;

    this.usersWithChanges = ChannelService.getChannel().changedBySet;
    this.selectedUsers = [];
  }

  publishSelectedChanges() {
    this.ChannelService.publishChanges(this.selectedUsers)
      .then(() => {
        this.resetSelection();
        this.HippoIframeService.reload();
      });
  }

  discardSelectedChanges() {
    this.ChannelService.discardChanges(this.selectedUsers)
      .then(() => {
        this.resetSelection();
        this.HippoIframeService.reload();
      });
  }

  resetSelection() {
    this.selectedUsers = [];
    this.onDone();
  }

  isChecked(user) {
    return this.selectedUsers.includes(user);
  }

  checkUser(user) {
    this.selectedUsers.push(user);
  }

  uncheckUser(user) {
    const index = this.selectedUsers.indexOf(user);
    this.selectedUsers.splice(index, 1);
  }

  toggle(user) {
    if (this.selectedUsers.includes(user)) {
      this.uncheckUser(user);
    } else {
      this.checkUser(user);
    }
  }

  allAreChecked() {
    return this.selectedUsers.length === this.usersWithChanges.length;
  }

  toggleAll() {
    if (this.allAreChecked()) {
      this.usersWithChanges.forEach((user) => {
        if (this.isChecked(user)) { // TODO double-up
          this.uncheckUser(user);
        }
      });
    } else {
      this.usersWithChanges.forEach((user) => {
        if (!this.isChecked(user)) {
          this.checkUser(user);
        }
      });
    }
  }
}
