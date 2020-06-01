/*
 * Copyright 2016-2020 Hippo B.V. (http://www.onehippo.com)
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

export default class MaskService {
  constructor($rootScope) {
    'ngInject';

    this.$rootScope = $rootScope;
    this.defaultMaskClass = 'masked';
  }

  _resetMaskClass() {
    this.maskClass = this.defaultMaskClass;
  }

  mask(optionalMaskClass = '') {
    this.$rootScope.$evalAsync(() => {
      this.isMasked = true;

      if (optionalMaskClass) {
        this.maskClass = `${this.defaultMaskClass} ${optionalMaskClass}`;
      } else {
        this._resetMaskClass();
      }
    });
  }

  unmask() {
    this.$rootScope.$evalAsync(() => {
      this.isMasked = false;
      this._resetMaskClass();
    });
  }
}
