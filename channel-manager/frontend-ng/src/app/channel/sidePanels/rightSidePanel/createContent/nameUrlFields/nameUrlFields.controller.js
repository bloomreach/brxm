/*
 * Copyright 2017-2020 Hippo B.V. (http://www.onehippo.com)
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

import { throttle } from 'throttle-debounce';

const URL_UPDATE_DELAY = 400;

class NameUrlFieldsController {
  constructor($element, CreateContentService) {
    'ngInject';

    this.createContentService = CreateContentService;
    this.isManualUrlMode = false;
    this.isUrlUpdating = false;
    this.nameInputField = $element.find('.name-input-element');
    this.updateUrlThrottle = throttle(URL_UPDATE_DELAY, this.updateUrl.bind(this));
  }

  $onInit() {
    this.nameField = this.nameField || '';
    this.urlField = this.urlField || '';
    this.namePlaceholder = this.namePlaceholder || 'PLACEHOLDER_DOCUMENT_NAME';
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
    if (this.isUrlUpdating) {
      // Backend call in progress, make sure to update the url one more time after it has finished.
      // This prevents concurrent calls to a slow backend.
      this.triggerAfterFinish = true;
      return;
    }

    this.isUrlUpdating = true;
    this.setDocumentUrlByName()
      .then(() => {
        this.isUrlUpdating = false;
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
