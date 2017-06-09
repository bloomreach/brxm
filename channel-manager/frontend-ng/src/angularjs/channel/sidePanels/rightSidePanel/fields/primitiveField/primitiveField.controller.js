/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

class PrimitiveFieldCtrl {
  constructor(FieldService) {
    'ngInject';

    this.FieldService = FieldService;
    this.autoDraftInterval = null;
  }

  getFieldName(index) {
    const fieldName = this.name ? `${this.name}/${this.fieldType.id}` : this.fieldType.id;
    return index > 0 ? `${fieldName}[${index}]` : fieldName;
  }

  getFieldError() {
    let combinedError = null;
    this.fieldValues.forEach((value, index) => {
      const fieldName = this.getFieldName(index);
      const field = this.form[fieldName];
      if (field) {
        combinedError = Object.assign(combinedError || {}, field.$error);
      }
    });
    return combinedError;
  }

  isValid() {
    return !this.fieldValues.some((fieldValue, index) => {
      const fieldName = this.getFieldName(index);
      const field = this.form[fieldName];
      return field && field.$invalid;
    });
  }

  focusPrimitive() {
    this.hasFocus = true;
    this.onFieldFocus();

    this.temporaryContent = angular.copy(this.fieldValues);
  }

  blurPrimitive() {
    delete this.hasFocus;
    this.onFieldBlur();

    if (angular.toJson(this.temporaryContent) !== angular.toJson(this.fieldValues)) {
      this.FieldService.draftField(this.getFieldName(), this.fieldValues);
    }
  }

  valueChanged() {
    this.FieldService.startDraftTimer(this.getFieldName(), this.fieldValues);
  }
}

export default PrimitiveFieldCtrl;
