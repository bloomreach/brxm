/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

class ContentEditorCtrl {
  constructor($mdDialog, $transitions, $uiRouterGlobals) {
    'ngInject';

    this.$mdDialog = $mdDialog;
    this.$transitions = $transitions;
    this.$uiRouterGlobals = $uiRouterGlobals;
  }

  $onInit() {
    this.documentId = this.$uiRouterGlobals.params.documentId;
    this.deregisterOnExit = this.$transitions.onExit({ from: '**.edit-content' }, () => this.confirmExit());
  }

  confirmExit() {
    const confirmationDialog = this.$mdDialog.confirm()
      .title(`Leave ${this.documentId} ?`)
      .textContent('Really?')
      .ok('Yes')
      .cancel('No');
    return this.$mdDialog.show(confirmationDialog);
  }

  $onDestroy() {
    this.deregisterOnExit();
  }
}

export default ContentEditorCtrl;
