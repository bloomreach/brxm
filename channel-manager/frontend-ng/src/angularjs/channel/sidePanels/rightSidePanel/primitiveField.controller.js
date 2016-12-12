/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

  $onInit() {
    this.onFieldFocus = this.onFieldFocus || angular.noop;
    this.onFieldBlur = this.onFieldBlur || angular.noop;
  }

  hasValue() {
    return angular.isArray(this.fieldValues) && this.fieldValues.length > 0;
  }

  getFieldName(index) {
    const fieldName = this.name ? `${this.name}/${this.fieldType.id}` : this.fieldType.id;
    return index > 0 ? `${fieldName}[${index}]` : fieldName;
  }

  getFieldError() {
    return this.fieldValues.length === 1 ? this._getSingleFieldError() : this._getMultipleFieldError();
  }

  _getSingleFieldError() {
    const fieldName = this.getFieldName();
    const field = this.form[fieldName];
    return field ? field.$error : null;
  }

  _getMultipleFieldError() {
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

  focusFieldType() {
    this.onFieldFocus();
  }

  blurFieldType() {
    this.onFieldBlur();
  }
}

export default PrimitiveFieldCtrl;
