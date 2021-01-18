/*
 * Copyright 2018-2021 Hippo B.V. (http://www.onehippo.com)
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

import DateValue from './dateValue.model';

class DateFieldController {
  constructor($element, ConfigService) {
    'ngInject';

    this.$element = $element;
    this.ConfigService = ConfigService;
  }

  $onInit() {
    this.ngModel.$render = () => {
      this.dateValue = new DateValue(this.ngModel.$viewValue,
        this.ConfigService.timeZone,
        this.fieldType.type === 'DATE_ONLY');
    };
  }

  valueChanged() {
    this.ngModel.$setViewValue(this.dateValue.toDateString());
  }

  setToNow() {
    this.dateValue.setToNow();
    this.valueChanged();
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

export default DateFieldController;
