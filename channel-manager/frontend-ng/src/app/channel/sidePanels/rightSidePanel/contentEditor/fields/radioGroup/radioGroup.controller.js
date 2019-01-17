/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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

class RadioGroupFieldController {
  constructor(ContentService) {
    'ngInject';

    this.ContentService = ContentService;
  }

  $onInit() {
    this._loadOptionsList();
  }

  isOrientationHorizontal() {
    return this.orientation !== 'vertical';
  }

  buttonValues() {
    return this.keys;
  }

  buttonDisplayValues(index) {
    return this.labels[index];
  }

  _loadOptionsList() {
    this.keys = [];
    this.labels = [];
    this.ContentService.getValueList(this.optionsSource, this.locale, this.sortComparator, this.sortBy, this.sortOrder)
      .then((document) => {
        document.forEach((item) => {
          this.keys.push(item.key);
          this.labels.push(item.label);
        });
      });
  }
}

export default RadioGroupFieldController;
