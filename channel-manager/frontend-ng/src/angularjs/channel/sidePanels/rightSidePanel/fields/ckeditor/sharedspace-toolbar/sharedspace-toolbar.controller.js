/*
 * Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
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

class SharedSpaceToolbar {
  constructor(SharedSpaceToolbarService, $rootScope) {
    'ngInject';

    this.$rootScope = $rootScope;
    this.isVisible = this.isVisible || false;
    this.SharedSpaceToolbarService = SharedSpaceToolbarService;
    this.showBottomToolbar = null;
  }

  $onInit() {
    this.SharedSpaceToolbarService.registerTriggerCallback(this.setToolbarVisible.bind(this));
  }

  $onDestroy() {
    this.isVisible = false;
  }

  setToolbarVisible(state, options = {}) {
    this.isVisible = state;
    this.showBottomToolbar = options.hasBottomToolbar || false;
    this.$rootScope.$apply();
  }
}

export default SharedSpaceToolbar;
