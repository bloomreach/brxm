/*
 * Copyright 2017-2020 Hippo B.V. (http://www.onehippo.com)
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

class OverlayToggleController {
  constructor(
    $rootScope,
    PageStructureService,
    ProjectService,
    localStorageService,
  ) {
    'ngInject';

    this.PageStructureService = PageStructureService;
    this.ProjectService = ProjectService;
    this.localStorageService = localStorageService;

    $rootScope.$on('page:change', () => this.initiateOverlay());
  }

  $onInit() {
    this.storageKey = `channelManager.overlays.${this.name}`;
    this.initiateOverlay();
  }

  $onChanges(changes) {
    if (changes.channel) {
      this.initiateOverlay();
    }
  }

  initiateOverlay() {
    if (this._isInitiallyDisabled()) {
      this.disabled = true;
      this.state = false;
    } else {
      this.disabled = false;
      this.loadPersistentState();
    }

    this.onStateChange({ state: this.state });
  }

  setState(state) {
    this.state = state;
    this.localStorageService.set(this.storageKey, this.state);
    this.onStateChange({ state });
  }

  loadPersistentState() {
    const state = this.localStorageService.get(this.storageKey);

    if (state === null) {
      this.state = this.defaultState;
    } else {
      this.state = state;
    }
  }

  _isInitiallyDisabled() {
    if (!this.ProjectService.isBranch()) {
      return false;
    }

    if (this.ProjectService.isEditingAllowed(this.name)) {
      return false;
    }

    const page = this.PageStructureService.getPage();
    const pageMeta = page.getMeta();
    if (pageMeta.isXPage() && page.getContainers().some(container => container.isXPageEditable())) {
      return false;
    }

    return true;
  }
}

export default OverlayToggleController;
