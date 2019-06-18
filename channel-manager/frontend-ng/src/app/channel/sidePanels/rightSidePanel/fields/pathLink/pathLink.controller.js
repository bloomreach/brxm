/*
 * Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
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

import NodeLinkController from '../nodeLink/nodeLink.controller';

export default class PathLinkController extends NodeLinkController {
  $onInit() {
    super.$onInit();

    this.$scope.$on('edit-component:select-document', (event, parameterName) => this._onSelectDocument(parameterName));
  }

  _onSelectDocument(parameterName) {
    if (this.name === parameterName) {
      this.ngModel.$setTouched();
      this.open();
    }
  }

  async _showPicker() {
    const { path: value, displayName } = await this.PickerService.pickPath(
      this.config.linkpicker,
      this.ngModel.$modelValue,
    );

    return { value, displayName };
  }
}
