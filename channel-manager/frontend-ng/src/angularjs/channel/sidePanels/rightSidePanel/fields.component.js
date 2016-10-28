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
  constructor($scope, $timeout, ChannelSidePanelService, ContentService) {
    'ngInject';

    this.$scope = $scope;
    this.$timeout = $timeout;
    this.ChannelSidePanelService = ChannelSidePanelService;
    this.ContentService = ContentService;
  }

  isEmptyMultiple(field) {
    return field.multiple && (!this.fieldValues[field.id] || this.fieldValues[field.id].length === 0);
  }

  hasFocusedField(field) {
    let hasFocusedField = false;
    if (field.fields && field.fields.findIndex(newField => newField.focused) !== -1) {
      hasFocusedField = true;
    }
    return hasFocusedField;
  }

  getFieldAsArray(fieldId) {
    const field = this.fieldValues[fieldId];
    return angular.isArray(field) ? field : [field];
  }

  onFieldFocus(field) {
    field.focused = true;
  }

  onFieldBlur(field) {
    field.focused = false;
  }
}

const channelFieldsComponentModule = angular
  .module('hippo-cm.channel.fieldsComponentModule', [])
  .component('channelFields', {
    bindings: {
      fieldTypes: '=',
      fieldValues: '=',
    },
    controller: ChannelFieldsCtrl,
    template,
  });

export default channelFieldsComponentModule;
