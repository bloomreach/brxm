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
  constructor(ChannelService) {
    'ngInject';

    this.changedBySet = ChannelService.getChannel().changedBySet;
    this.selectedChanges = [];
  }

  publishChanges() {
    this.onDone();
  }

  discardChanges() {
    this.onDone();
  }

  toggleAll() {
    if (this.selectedChanges.length === this.changedBySet.length) {
      this.selectedChanges = [];
    } else {
      this.selectedChanges = this.changedBySet;
    }
  }

  toggle(changedBy) {
    const index = this.selectedChanges.findIndex((element) => element === changedBy);

    if (index !== -1) {
      this.selectedChanges.splice(index, 1);
    } else {
      this.selectedChanges.push(changedBy);
    }
  }
}
