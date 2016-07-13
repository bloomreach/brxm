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
  constructor($interpolate, $log, $translate, $mdToast) {
    'ngInject';

    this.$interpolate = $interpolate;
    this.$log = $log;
    this.$translate = $translate;
    this.$mdToast = $mdToast;
  }

  showError(key, params, parentJQueryElement) {
    const text = this.$translate.instant(key, params);
    this._show(text, parentJQueryElement);
  }

  showErrorOnSubpage(key, params) {
    this._showOnSubpage(this.$translate.instant(key, params));
  }

  /* eslint-disable no-param-reassign */
  showErrorResponseOnSubpage(response, defaultKey, errorMap = {}) {
    response = response || {};
    if (response.message) {
      this.$log.info(response.message);
    }

    let text;
    if (response.data && response.data.userMessage) {
      const template = response.data.userMessage;
      delete response.data.userMessage;
      text = this.$interpolate(template)(response.data);
    } else {
      const key = errorMap[response.errorCode] || defaultKey;
      text = this.$translate.instant(key, response.data);
    }

    this._showOnSubpage(text);
  }

  _showOnSubpage(text) {
    const feedbackParent = $('.subpage-feedback-parent');
    this._show(text, feedbackParent);
  }

  _show(text, parentJQueryElement = $('hippo-iframe')) {
    this.$mdToast.show(
      this.$mdToast.simple()
        .textContent(text)
        .position('top right')
        .hideDelay(HIDE_DELAY_IN_MS)
        .parent(parentJQueryElement)
    );
  }
}

