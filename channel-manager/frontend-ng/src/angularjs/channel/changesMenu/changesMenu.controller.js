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

export class ChangesMenuCtrl {
  constructor(
      $translate,
      ChannelService,
      ConfigService,
      DialogService,
      HippoIframeService,
    ) {
    'ngInject';
    this.$translate = $translate;
    this.ChannelService = ChannelService;
    this.DialogService = DialogService;
    this.HippoIframeService = HippoIframeService;

    this.showManageChanges = ConfigService.canManageChanges;
    this.hasOwnChanges = ChannelService.getChannel().changedBySet.indexOf(ConfigService.cmsUser) !== -1;
  }

  publish() {
    this.ChannelService.publishOwnChanges().then(() => this.HippoIframeService.reload());
    // TODO: what if this fails?
    // show a toast that all went well, or that the publication failed.
    // More information may be exposed by logging an error(?) in the console,
    // based on the actual error details from the back-end.
  }

  discard() {
    this._confirmDiscard().then(() => {
      this.ChannelService.discardOwnChanges().then(() => this.HippoIframeService.reload());
      // TODO: what if this fails?
      // show a toast that discarding the changed failed.
      // More information may be exposed by logging an error(?) in the console,
      // based on the actual error details from the back-end.
    });
  }

  _confirmDiscard() {
    const confirm = this.DialogService.confirm()
      .title(this.$translate.instant('CONFIRM_DISCARD_OWN_CHANGES_TITLE'))
      .textContent(this.$translate.instant('CONFIRM_DISCARD_OWN_CHANGES_MESSAGE'))
      .ok(this.$translate.instant('BUTTON_YES'))
      .cancel(this.$translate.instant('BUTTON_NO'));

    return this.DialogService.show(confirm);
  }
}
