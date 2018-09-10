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

import NodeLinkController from '../nodeLink/nodeLink.controller';

class PathLinkController extends NodeLinkController {
  constructor($element, $scope, $timeout, CmsService) {
    'ngInject';

    super($element, $scope, $timeout, CmsService);
  }

  $onInit() {
    super.$onInit();

    this.CmsService.subscribe('path-picked', this._onPathPicked, this);
    this.CmsService.subscribe('path-canceled', this._onPathCanceled, this);
  }

  $onDestroy() {
    this.CmsService.unsubscribe('path-picked', this._onPathPicked, this);
    this.CmsService.unsubscribe('path-canceled', this._onPathCanceled, this);
  }

  openLinkPicker() {
    this.CmsService.publish('show-path-picker', this.name, this.ngModel.$modelValue, this.config.linkpicker);
  }

  _onPathPicked(field, path, displayValue) {
    if (field !== this.name) {
      return;
    }

    this.$scope.$apply(() => {
      if (this.linkPicked) {
        this._focusSelectButton();
      }
      this.linkPicked = true;
      this.displayName = displayValue;
      this.ngModel.$setViewValue(path);
    });
  }

  _onPathCanceled(field) {
    if (field !== this.name) {
      return;
    }

    this._focusSelectButton();
  }
}

export default PathLinkController;
