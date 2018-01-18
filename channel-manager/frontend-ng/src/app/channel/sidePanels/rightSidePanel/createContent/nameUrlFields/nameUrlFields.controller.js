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

const URL_UPDATE_DELAY = 400;

class NameUrlFieldsController {
  constructor($element, CreateContentService, throttle) {
    'ngInject';

    this.createContentService = CreateContentService;
    this.isManualUrlMode = false;
    this.urlUpdate = false;
    this.nameInputField = $element.find('#nameInputElement');
    this.updateUrlThrottle = throttle(() => this.updateUrl(), URL_UPDATE_DELAY, true);
  }

  $onInit() {
    this.nameField = this.nameField || '';
    this.urlField = this.urlField || '';
  }

  $onChanges(changes) {
    if (changes.hasOwnProperty('locale') && !changes.locale.isFirstChange()) {
      this.setDocumentUrlByName();
    }
  }

  onNameChange() {
    if (!this.isManualUrlMode) {
      this.updateUrlThrottle();
    }
  }

  updateUrl() {
    if (this.urlUpdate) {
      this.triggerAfterFinish = true;
      return;
    }

    this.urlUpdate = true;
    this.setDocumentUrlByName()
      .then(() => {
        this.urlUpdate = false;
        if (this.triggerAfterFinish) {
          this.triggerAfterFinish = false;
          this.updateUrl();
        }
      });
  }

  setDocumentUrlByName() {
    return this.createContentService.generateDocumentUrlByName(this.nameField, this.locale)
      .then((slug) => {
        this.urlField = slug;
      });
  }

  validateFields() {
    const conditions = [
      this.nameField.length !== 0, // name empty
      this.urlField.length !== 0, // url empty
      /\S/.test(this.nameField), // name is only whitespace(s)
      /\S/.test(this.urlField), // url is only whitespaces
    ];
    return conditions.every(condition => condition === true);
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
