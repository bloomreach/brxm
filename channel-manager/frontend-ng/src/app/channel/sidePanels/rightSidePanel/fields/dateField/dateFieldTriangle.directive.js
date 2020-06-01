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

/**
 * @note this directive can be safely removed when migrating to Angular
 */
class DateFieldTriangleDirective {
  constructor($mdUtil) {
    'ngInject';

    this.require = ['?^^dateField', '?^^mdDatepicker', '?^^mdInputContainer'];
    this.restrict = 'C';
    this.$mdUtil = $mdUtil;
  }

  link(scope, element, attrs, [dateField, mdDatepicker, mdInputContainer]) {
    if (!dateField || !mdDatepicker) {
      return;
    }

    const setFocused = (value) => {
      mdDatepicker.setFocused(value);

      if (mdInputContainer) {
        mdInputContainer.setFocused(value);
      }
    };

    // focus event should be handled after blur event on the input field
    element.on('focus', () => this.$mdUtil.nextTick(() => setFocused(true)));
    element.on('blur', () => setFocused(false));
  }
}

export default DateFieldTriangleDirective;
