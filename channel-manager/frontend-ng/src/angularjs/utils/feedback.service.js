/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

const HIDE_DELAY_IN_MS = 3000;

export class FeedbackService {
  constructor($translate, $mdToast) {
    'ngInject';

    this.$translate = $translate;
    this.$mdToast = $mdToast;
  }

  showError(errorKey, params, parentJQueryElement = $('hippo-iframe')) {
    this.$mdToast.show(
      this.$mdToast.simple()
        .textContent(this.$translate.instant(errorKey, params))
        .position('top right')
        .hideDelay(HIDE_DELAY_IN_MS)
        .parent(parentJQueryElement)
    );
  }

  showErrorOnSubpage(errorKey, params) {
    const feedbackParent = $('.subpage-feedback-parent');
    this.showError(errorKey, params, feedbackParent);
  }
}

