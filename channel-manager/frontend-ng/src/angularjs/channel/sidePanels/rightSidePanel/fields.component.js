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

import template from './fields.html';

export class ChannelFieldsCtrl {

  $onInit() {
    this.onFieldFocus = this.onFieldFocus || angular.noop;
    this.onFieldBlur = this.onFieldBlur || angular.noop;
  }

  hasValue(field) {
    const values = this.fieldValues[field.id];
    return angular.isArray(values) && values.length > 0;
  }

  getDisplayNameForCompound(field, index) {
    if (this.hasValue(field)) {
      const values = this.fieldValues[field.id];
      if (values.length > 1) {
        return `${field.displayName} (${index + 1})`;
      }
    }

    return field.displayName;
  }

  focusCompound(fieldTypeId, index) {
    this.compoundWithFocusedField = this.fieldValues[fieldTypeId][index];
    this.onFieldFocus();
  }

  blurCompound() {
    this.compoundWithFocusedField = null;
    this.onFieldBlur();
  }

  hasFocusedField(fieldTypeId, index) {
    return this.compoundWithFocusedField === this.fieldValues[fieldTypeId][index];
  }
}

const channelFieldsComponentModule = angular
  .module('hippo-cm.channel.fieldsComponentModule', [])
  .component('channelFields', {
    bindings: {
      fieldTypes: '=',
      fieldValues: '=',
      onFieldFocus: '&',
      onFieldBlur: '&',
    },
    controller: ChannelFieldsCtrl,
    template,
  });

export default channelFieldsComponentModule;
