/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

class EditComponentTabsCtrl {
  constructor(
    $uiRouterGlobals,
    $transitions,
  ) {
    'ngInject';

    this.$uiRouterGlobals = $uiRouterGlobals;
    this.$transitions = $transitions;
  }

  $onInit() {
    this._setActiveNavItem();
    this.$transitions.onSuccess({ to: '**.edit-component.*' }, () => this._setActiveNavItem());
    this.$transitions.onError({ to: '**.edit-component.*' }, () => this._setActiveNavItem());
  }

  _setActiveNavItem() {
    this.selectedNavItem = this.$uiRouterGlobals.current.name.split('.').pop();
  }
}

export default EditComponentTabsCtrl;
