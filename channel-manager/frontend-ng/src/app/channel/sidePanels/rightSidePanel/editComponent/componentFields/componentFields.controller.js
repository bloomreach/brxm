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

import { throttle } from 'throttle-debounce';

const THROTTLE_PERIOD = 500;

class ComponentFieldsCtrl {
  constructor($scope, ComponentEditor, EditComponentService) {
    'ngInject';

    this.$scope = $scope;
    this.ComponentEditor = ComponentEditor;
    this.EditComponentService = EditComponentService;

    this.valueChanged = throttle(THROTTLE_PERIOD, this.valueChanged.bind(this));
  }

  valueChanged() {
    this.ComponentEditor.updatePreview()
      .catch(() => {
        this.EditComponentService.killEditor();
      });
  }

  blur(property) {
    this.ComponentEditor.setDefaultIfValueIsEmpty(property);
  }
}

export default ComponentFieldsCtrl;
