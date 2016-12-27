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

class DialogService {
  constructor($mdDialog, CmsService) {
    'ngInject';

    this.$mdDialog = $mdDialog;
    this.CmsService = CmsService;
  }

  confirm() {
    return this.$mdDialog.confirm({
      onRemoving: () => this._removeMask(),
    });
  }

  alert() {
    return this.$mdDialog.alert({
      onRemoving: () => this._removeMask(),
    });
  }

  show(dialog) {
    this._showMask();

    const oldOnRemoving = dialog.onRemoving;
    dialog.onRemoving = () => {
      if (angular.isFunction(oldOnRemoving)) {
        oldOnRemoving.apply(dialog);
      }
      this._removeMask();
    };
    return this.$mdDialog.show(dialog);
  }

  _showMask() {
    this.CmsService.publish('show-mask');
  }

  _removeMask() {
    this.CmsService.publish('remove-mask');
  }

  hide() {
    return this.$mdDialog.hide();
  }
}

export default DialogService;
