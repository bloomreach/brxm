/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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

export default class OpenuiStringFieldController {
  constructor($element, $log, ExtensionService, OpenUIService) {
    'ngInject';

    this.$element = $element;
    this.$log = $log;
    this.ExtensionService = ExtensionService;
    this.OpenUIService = OpenUIService;
  }

  $onInit() {
    if (this.mdInputContainer) {
      this.mdInputContainer.setHasValue(true);
    }
  }

  $onChanges(changes) {
    if (changes.extensionId) {
      this._initExtension(changes.extensionId.currentValue);
    }
  }

  _initExtension(id) {
    this.extension = this.ExtensionService.getExtension(id);
    this.OpenUIService.connect({
      url: this.ExtensionService.getExtensionUrl(this.extension),
      appendTo: this.$element[0],
      methods: {},
    })
      .catch((error) => {
        this.$log.warn(`Extension '${this.extension.displayName}' failed to connect with the client library.`, error);
      });
  }
}
