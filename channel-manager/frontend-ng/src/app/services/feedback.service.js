/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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

const DELAY_NOTIFICATION = 1000;
const DELAY_ERROR = 3 * 1000;
const DELAY_DISMISSIBLE = 20 * 1000;

class FeedbackService {
  constructor($interpolate, $log, $translate, $mdToast) {
    'ngInject';

    this.$interpolate = $interpolate;
    this.$log = $log;
    this.$translate = $translate;
    this.$mdToast = $mdToast;
  }

  _showToast({
    text,
    delay,
    showDismissal = false,
    dismissal = showDismissal && this.$translate.instant('ERROR_TOAST_DISMISS'),
  }) {
    const toast = this.$mdToast.simple()
      .textContent(text)
      .position('top right')
      .hideDelay(delay);

    if (showDismissal) {
      toast.action(dismissal);
    }

    this.$mdToast.show(toast);
  }

  showNotification(key, params) {
    this._showToast({
      text: this.$translate.instant(key, params),
      delay: DELAY_NOTIFICATION,
    });
  }

  showError(key, params) {
    this._showToast({
      text: this.$translate.instant(key, params),
      delay: DELAY_ERROR,
    });
  }

  showDismissible(key, params) {
    this._showToast({
      text: this.$translate.instant(key, params),
      delay: DELAY_DISMISSIBLE,
      showDismissal: true,
    });
  }

  showErrorResponse(response, defaultKey, errorMap = {}, defaultParams = {}) {
    if (!response) {
      this.showError(defaultKey, defaultParams);
      return;
    }

    // Handle plain error message or fallback to ExtResponse
    const responseDebugMessage = (response.parameterMap && response.parameterMap.errorReason) || response.message;
    const responseErrorCode = response.error || response.errorCode;
    const responseParams = Object.assign(defaultParams, response.parameterMap || response.data);

    if (responseDebugMessage) {
      this.$log.info(responseDebugMessage);
    }

    let text;
    if (responseParams && responseParams.userMessage) {
      const template = responseParams.userMessage;
      delete responseParams.userMessage;
      text = this.$interpolate(template)(responseParams);
    } else {
      const key = errorMap[responseErrorCode] || defaultKey;
      text = this.$translate.instant(key, responseParams);
    }

    this._showToast({ text, delay: DELAY_ERROR });
  }
}


export default FeedbackService;
