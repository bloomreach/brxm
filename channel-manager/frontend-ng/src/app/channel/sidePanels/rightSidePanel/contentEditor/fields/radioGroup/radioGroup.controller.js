/*
 * Copyright 2019-2021 Hippo B.V. (http://www.onehippo.com)
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
  constructor($element, ContentService) {
    'ngInject';

    this.$element = $element;
    this.ContentService = ContentService;
  }

  $onInit() {
    this.keys = [];
    this.labels = [];
    if (this.fieldType === 'BOOLEAN_RADIO_GROUP') {
      this._setBooleanOptions();
    } else {
      this._loadOptionsList();
    }
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

  async _loadOptionsList() {
    const document = await this._getValueList();

    document.forEach((item) => {
      this.keys.push(item.key);
      this.labels.push(item.label);
    });
  }

  _getValueList() {
    return this.ContentService.getValueList(
      this.optionsSource,
      this.locale,
      this.sortComparator,
      this.sortBy,
      this.sortOrder,
    );
  }

  async _setBooleanOptions() {
    if (this.optionsSource) {
      const document = await this._getValueList();
      this._setItem(document, 'true');
      this._setItem(document, 'false');
      return;
    }
    this.keys = ['true', 'false'];
    this.labels = [this.trueLabel, this.falseLabel];
  }

  _setItem(document, itemKey) {
    const foundItem = document.find(item => item.key === itemKey);
    if (foundItem) {
      this.keys.push(foundItem.key);
      this.labels.push(foundItem.label);
      return;
    }
    this.keys.push(itemKey);
    this.labels.push(itemKey);
  }

  onBlur($event) {
    if (this.mdInputContainer) {
      this.mdInputContainer.setFocused(false);
    }

    this.$element.triggerHandler($event || 'blur');
  }

  onFocus($event) {
    if (this.mdInputContainer) {
      this.mdInputContainer.setFocused(true);
    }

    this.$element.triggerHandler($event || 'focus');
  }
}

export default RadioGroupFieldController;
