/*
 * Copyright 2016-2019 Hippo B.V. (http://www.onehippo.com)
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

import template from './compoundField.html';
import CompoundFieldCtrl from './compoundField.controller';

const compoundFieldComponent = {
  bindings: {
    name: '<',
    fieldType: '<',
    fieldValue: '<',
  },
  controller: CompoundFieldCtrl,
  template,
  require: {
    form: '^^form',
    parent: '?^^compoundField',
  },
};

export default compoundFieldComponent;
