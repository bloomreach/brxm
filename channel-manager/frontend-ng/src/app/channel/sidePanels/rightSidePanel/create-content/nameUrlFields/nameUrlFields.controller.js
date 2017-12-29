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

import { Observable } from 'rxjs/Observable';
import 'rxjs/add/observable/fromEvent';
import 'rxjs/add/operator/debounceTime';
import 'rxjs/add/operator/filter';
import 'rxjs/add/operator/do';

class NameUrlFieldsController {
  constructor($element, $timeout, CreateContentService) {
    'ngInject';

    this.createContentService = CreateContentService;
    this.nameInputField = $element.find('#nameInputElement');
    this.isManualUrlMode = false;
    this.$timeout = $timeout;
  }


  $onInit() {
    this.nameField = this.nameField || '';
    this.urlField = this.urlField || '';

    Observable.fromEvent(this.nameInputField, 'keyup')
      .filter(() => !this.isManualUrlMode)
      .do(() => { this.urlUpdate = true; })
      .debounceTime(1000)
      .subscribe(() => {
        console.log(this.form);
        this.setDocumentUrlByName();
        this.urlUpdate = false;
      });
  }

  $onChanges(changes) {
    if (changes.hasOwnProperty('locale') && !changes.locale.isFirstChange()) {
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
