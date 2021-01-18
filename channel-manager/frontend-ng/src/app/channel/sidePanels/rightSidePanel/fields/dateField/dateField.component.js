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

import './dateField.scss';
import template from './dateField.html';
import DateFieldFieldCtrl from './dateField.controller';

const dateFieldComponent = {
  bindings: {
    disabled: '<?',
    fieldType: '<',
    name: '<',
  },
  controller: DateFieldFieldCtrl,
  template,
  require: {
    ngModel: 'ngModel',
    mdInputContainer: '?^^mdInputContainer',
  },
};

export default dateFieldComponent;
