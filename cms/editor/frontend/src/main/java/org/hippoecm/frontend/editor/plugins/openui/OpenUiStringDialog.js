/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

class OpenUiStringDialog {

  constructor(parameters) {
    this.parameters = parameters;
  }

  onConnect(connection) {
  }

  onDestroy() {
  }

  getMethods() {
    return {
      cancelDialog: this.cancelDialog.bind(this),
      closeDialog: this.closeDialog.bind(this),
      getDialogOptions: this.getDialogOptions.bind(this),
    };
  }

  cancelDialog() {
    return this._closeDialog((instance) => {
      instance.cancelDialog();
    });
  }

  closeDialog(value) {
    return this._closeDialog((instance) => {
      instance.closeDialog(value);
    });
  }

  _closeDialog(onSuccess) {
    return new Promise((resolve, reject) => {
      Wicket.Ajax.ajax({
        u: this.parameters.closeUrl,
        sh: [() => {
          const instance = OpenUi.getInstance(this.parameters.parentExtensionId);
          onSuccess(instance);
          resolve();
        }],
        fh: [(e) => {
          console.error('failed to close the dialog', e);
          reject(e);
        }],
      });
    });
  }

  getDialogOptions() {
    return this.parameters.dialogOptions;
  }
}

OpenUi.registerClass(OpenUiStringDialog);
