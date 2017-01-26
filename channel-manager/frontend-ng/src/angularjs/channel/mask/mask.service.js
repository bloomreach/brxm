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

class MaskService {
  constructor() {
    this.defaultMaskClass = 'masked';
    this.clickHandler = angular.noop;
  }

  resetMaskClass() {
    this.maskClass = this.defaultMaskClass;
  }

  onClick(clickHandler) {
    this.clickHandler = clickHandler;
  }

  removeClickHandler() {
    this.clickHandler = angular.noop;
  }

  mask(optionalMaskClass = '') {
    this.isMasked = true;

    if (optionalMaskClass) {
      this.maskClass = `${this.defaultMaskClass} ${optionalMaskClass}`;
    } else {
      this.resetMaskClass();
    }
  }

  unmask() {
    this.isMasked = false;
    this.resetMaskClass();
  }
}

export default MaskService;
