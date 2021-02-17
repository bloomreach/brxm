/*
 * Copyright 2016-2021 Hippo B.V. (http://www.onehippo.com)
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

  getFieldName(fieldType) {
    return `${this.name ? `${this.name}/` : ''}${fieldType.id}`;
  }

  getFieldTypeHash(fieldType) {
    return `${fieldType.id}:${fieldType.validators}`;
  }

  isCompound({ type }) {
    return type === 'COMPOUND';
  }

  isChoice({ type }) {
    return type === 'CHOICE';
  }

  isPrimitive(type) {
    return !this.isCompound(type) && !this.isChoice(type);
  }
}

export default DocumentFieldsCtrl;
