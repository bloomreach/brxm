/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

class DateFieldController {
  constructor(FieldService) {
    'ngInject';

    this.FieldService = FieldService;
    this.dateValues = [];
  }

  $onInit() {
    this.onFieldFocus = this.onFieldFocus || angular.noop;
    this.onFieldBlur = this.onFieldBlur || angular.noop;

    for (let i = 0; i < this.fieldValues.length; i++) {
      this.dateValues.push(new DateValue(this.fieldValues[i].value))
    }

  }

  getFieldName(index) {
    const fieldName = this.name ? `${this.name}/${this.fieldType.id}` : this.fieldType.id;
    return index > 0 ? `${fieldName}[${index + 1}]` : fieldName;
  }

  valueChanged() {
    this._updateFieldValues();
    this.FieldService.startDraftTimer(this.getFieldName(), this.fieldValues);
  }

  focusDateField($event = null) {
    this.hasFocus = true;
    this.onFieldFocus();

    this.oldValues = angular.copy(this.fieldValues);
    this.FieldService.unsetFocusedInput();

    if ($event) {
      $event.target = angular.element($event.target);
      this.FieldService.setFocusedInput($event.target, $event.customFocus);
    }
  }

  blurDateField($event = null) {
    if ($event) {
      $event.target = angular.element($event.relatedTarget);

      if (this.FieldService.shouldPreserveFocus($event.target)) {
        this.FieldService.triggerInputFocus();
        return;
      }
    }
    delete this.hasFocus;
    this.onFieldBlur();
    this._draftField();
  }

  _updateFieldValues() {
    for (let i = 0; i < this.fieldValues.length; i++) {
      this.fieldValues[i].value = this.dateValues[i].value;
    }
  }

  _draftField() {
    if (!angular.equals(this.oldValues, this.fieldValues)) {
      this.FieldService.draftField(this.getFieldName(), this.fieldValues);
    }
  }
}

class DateValue {

  constructor(value) {
    this.value = value;
  }

  /**
   * @returns {string}
   */
  get hours() {
    let timestamp = Date.parse(this.value);
    if (angular.isNumber(timestamp)) {
      return '' + new Date(timestamp).getHours();
    }
    return '';
  }

  /**
   * @param {string} hours
   */
  set hours(hours) {
    if (hours && hours.length > 0 && !DateValue.isNumeric(hours)) {
      return;
    }

    let timestamp = Date.parse(this.value);
    if (angular.isNumber(timestamp)) {
      let date = new Date(timestamp);
      date.setHours(hours);
      this.value = this.value.substr(0, 11) + DateValue.pad(date.getUTCHours(), 2) + this.value.substr(13);
    }
  }

  /**
   * @returns {string}
   */
  get minutes() {
    let timestamp = Date.parse(this.value);
    if (angular.isNumber(timestamp)) {
      return '' + new Date(timestamp).getMinutes();
    }
    return '';
  }

  /**
   * @param {string} minutes
   */
  set minutes(minutes) {
    if (minutes && minutes.length > 0 && !DateValue.isNumeric(minutes)) {
      return;
    }

    let timestamp = Date.parse(this.value);
    if (angular.isNumber(timestamp)) {
      let date = new Date(timestamp);
      date.setMinutes(minutes);
      this.value = this.value.substr(0, 14) + DateValue.pad(date.getUTCMinutes(), 2) + this.value.substr(16);
    }

  }

  static pad(num, size) {
    let result = num + "";
    while (result.length < size) {
      result = "0" + result;
    }
    return result;
  }

  static isNumeric(n) {
    return !isNaN(parseFloat(n)) && isFinite(n);
  }

}

export default DateFieldController;