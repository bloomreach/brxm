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

class FeedbackService {
  constructor($interpolate, $log, $translate, $mdToast) {
    'ngInject';

    this.$interpolate = $interpolate;
    this.$log = $log;
    this.$translate = $translate;
    this.$mdToast = $mdToast;
  }

  showError(key, params, parentJQueryElement) {
    this._showErrorMessage(key, params, parentJQueryElement);
  }

  showErrorOnSubpage(key, params) {
    const target = this._getSubpageTarget();
    this._showErrorMessage(key, params, target);
  }

  showErrorResponse(response, defaultKey, errorMap) {
    this._showErrorResponse(response, defaultKey, errorMap);
  }

  showErrorResponseOnSubpage(response, defaultKey, errorMap) {
    const target = this._getSubpageTarget();
    this._showErrorResponse(response, defaultKey, errorMap, target);
  }

  _getSubpageTarget() {
    return $('.subpage-feedback-parent');
  }

  _showErrorMessage(key, params, target) {
    const text = this.$translate.instant(key, params);
    this._show(text, target);
  }

  _showErrorResponse(response, defaultKey, errorMap = {}, target) {
    if (!response) {
      this._showErrorMessage(defaultKey, undefined, target);
      return;
    }

    // Handle plain error message or fallback to ExtResponse
    const responseDebugMessage = (response.parameterMap && response.parameterMap.errorReason) || response.message;
    const responseErrorCode = response.error || response.errorCode;
    const responseParams = response.parameterMap || response.data;

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

    this._show(text, target);
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


export default FeedbackService;
