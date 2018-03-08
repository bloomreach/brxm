/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

class NameUrlFieldsDialogController {
  constructor($mdDialog, title, nameField, urlField, locale) {
    'ngInject';

    this.$mdDialog = $mdDialog;
    this.title = title;
    this.nameField = nameField;
    this.urlField = urlField;
    this.locale = locale;
    this.isUrlUpdating = false;
  }

  validateFields() {
    this.nameField = this.nameField || '';
    this.urlField = this.urlField || '';
    const conditions = [
      this.nameField.length !== 0, // name empty
      this.urlField.length !== 0, // url empty
      /\S/.test(this.nameField), // name is only whitespace(s)
      /\S/.test(this.urlField), // url is only whitespaces
    ];
    return conditions.every(condition => condition === true);
  }

  submit() {
    return this.$mdDialog.hide({
      name: this.nameField,
      url: this.urlField,
    });
  }

  cancel() {
    return this.$mdDialog.cancel();
  }
}

export default NameUrlFieldsDialogController;
