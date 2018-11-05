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

const LOADING_TIMEOUT_MS = 1000;

class RightSidePanelService {
  constructor($timeout) {
    'ngInject';

    this.$timeout = $timeout;
    this.loadingPromise = null;

    this.stopLoading();
    this.clearTitle();
    this.clearContext();
  }

  isLoading() {
    return this.loading;
  }

  startLoading() {
    if (this.loading) {
      return;
    }
    if (!this.loadingPromise) {
      this.loadingPromise = this.$timeout(() => {
        this.loadingPromise = null;
        this.loading = true;
      }, LOADING_TIMEOUT_MS);
    }
  }

  stopLoading() {
    if (this.loadingPromise) {
      this.$timeout.cancel(this.loadingPromise);
      this.loadingPromise = null;
    }
    this.loading = false;
  }

  getTitle() {
    return this.title;
  }

  getTooltip() {
    return this.tooltip;
  }

  setTitle(title, tooltip) {
    this.title = title;
    this.tooltip = tooltip || title;
  }

  clearTitle() {
    this.title = '';
    this.tooltip = '';
  }

  setContext(context) {
    this.context = context;
  }

  getContext() {
    return this.context;
  }

  clearContext() {
    this.context = '';
  }
}

export default RightSidePanelService;
