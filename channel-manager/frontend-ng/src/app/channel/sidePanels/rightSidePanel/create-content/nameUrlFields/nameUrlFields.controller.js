/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

class NameUrlFieldsController {
  constructor ($element, $timeout, $scope, CreateContentService) {
    'ngInject';

    this.createContentService = CreateContentService;
    this.nameInputField = $element.find('#nameInputElement');
    this.isManualUrlMode = false;
    this.$timeout = $timeout;
    this.$scope = $scope;
    this.locale = 'en';
  }


  $onInit() {
    this.nameField = this.nameField || '';
    this.urlField = this.urlField || '';
    this.nameInputField.on('keyup', () => {
      if (this.isManualUrlMode) {
        return;
      }

      this.urlUpdate(true);
      this.$timeout(() => {
        this.setDocumentUrlByName();
        this.urlUpdate(false);
      }, 1000);
    });
  }

  $onChanges(changes) {
    if (changes.hasOwnProperty('locale')) {
      this.setDocumentUrlByName();
    }
  }

  setDocumentUrlByName() {
    return this.createContentService.generateDocumentUrlByName(this.nameField, this.locale).then((slug) => {
      this.urlField = slug;
    });
  }

  validateFields() {
    const conditions = [
      this.nameField.length !== 0, // name empty
      this.urlField.length !== 0, // url empty
      /\S/.test(this.nameField), // name is only whitespace(s)
      /\S/.test(this.urlField) // url is only whitespaces
    ];
    return conditions.every((condition) => condition === true);
  }

  setManualUrlEditMode(state) {
    if (state) {
      this.isManualUrlMode = true;
    } else {
      this.isManualUrlMode = false;
      this.setDocumentUrlByName();
    }
  }
}

export default NameUrlFieldsController;
