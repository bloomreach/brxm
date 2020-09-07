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
    localStorageService,
  ) {
    'ngInject';

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
    if (this.isInitiallyDisabled()) {
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
}

export default OverlayToggleController;
