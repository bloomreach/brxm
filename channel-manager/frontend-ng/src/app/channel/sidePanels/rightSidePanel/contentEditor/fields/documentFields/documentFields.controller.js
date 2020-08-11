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

class DocumentFieldsCtrl {
  constructor() {
    'ngInject';
  }

  $onInit() {
    this.onFieldFocus = this.onFieldFocus || angular.noop;
    this.onFieldBlur = this.onFieldBlur || angular.noop;
  }

  getFieldName(fieldType, index) {
    const fieldName = this.name ? `${this.name}/${fieldType.id}` : fieldType.id;
    return index > 0 ? `${fieldName}[${index + 1}]` : fieldName;
  }

  getFieldTypeHash(fieldType) {
    return `${fieldType.id}:${fieldType.validators}`;
  }

  hasValue(field) {
    const values = this.fieldValues[field.id];
    return angular.isArray(values) && values.length > 0;
  }
}

export default DocumentFieldsCtrl;
