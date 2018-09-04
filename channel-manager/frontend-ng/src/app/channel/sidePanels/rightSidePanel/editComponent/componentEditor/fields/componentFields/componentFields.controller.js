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

class ComponentFieldsCtrl {
  constructor() {
    'ngInject';
  }

  $onInit() {
    this.onFieldFocus = this.onFieldFocus || angular.noop;
    this.onFieldBlur = this.onFieldBlur || angular.noop;
  }

  valueChanged() {
    console.log('TODO: implement ComponentFieldsCtrl.valueChanged');
  }

  focusPrimitive() {
    this.hasFocus = true;
  }

  blurPrimitive() {
    delete this.hasFocus;
  }
}

export default ComponentFieldsCtrl;
