/*
 * Copyright 2017-2021 Hippo B.V. (http://www.onehippo.com)
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

class FieldService {
  constructor($timeout, $q, ContentService) {
    'ngInject';

    this.$timeout = $timeout;
    this.$q = $q;
    this.ContentService = ContentService;

    this.documentId = null;
    this.throttled = {};
    this.AUTOSAVE_DELAY = 2000;

    this._focusedInput = null;
    this._customFocusCallback = null;
  }

  cleanValues(values) {
    const cleanedValues = angular.copy(values);
    // do not send back errorInfo
    if (angular.isArray(cleanedValues)) {
      cleanedValues.forEach(value => delete value.errorInfo);
    } else if (angular.isObject(cleanedValues)) {
      delete cleanedValues.errorInfo;
    }
    return cleanedValues;
  }

  shouldPreserveFocus(relatedTarget) {
    // validExpressions is an array containing functions.
    // The functions will be evaluated, and if at least one of them is true,
    // "true" will be returned, therefore input focus will be preserved. Otherwise, false.
    const validExpressions = [
      () => relatedTarget.is('.btn-full-screen') || relatedTarget.is('.btn-normal-screen'),
      () => relatedTarget.is('.btn-overlay-toggle'),
    ];

    return validExpressions.some(expression => expression());
  }

  setFocusedInput(element, customFocusCallback = null) {
    this._focusedInput = element;
    this._customFocusCallback = customFocusCallback;
  }

  unsetFocusedInput() {
    this._focusedInput = null;
    this._customFocusCallback = null;
  }

  triggerInputFocus() {
    if (this._focusedInput) {
      if (this._customFocusCallback) {
        this._customFocusCallback();
      } else {
        setTimeout(() => {
          this._focusedInput.focus();
        }, 0);
      }
    }
  }

  setDocumentId(documentId) {
    this.documentId = documentId;
  }

  getDocumentId() {
    return this.documentId;
  }

  save({
    documentId = this.getDocumentId(),
    name,
    values,
    throttle = false,
  }) {
    const wasThrottled = this._abortThrottled(documentId, name);

    if (throttle) {
      return this._throttle(documentId, name, values, !wasThrottled);
    }

    try {
      return this._save(documentId, name, values);
    } finally {
      this._abortThrottled(documentId, name);
    }
  }

  _save(documentId, name, values) {
    return this.ContentService.saveField(documentId, name, this.cleanValues(values));
  }

  _throttle(documentId, name, values, immediate) {
    if (!this.throttled[documentId]) {
      this.throttled[documentId] = {};
    }

    const deferred = this.$q.defer();
    this.throttled[documentId][name] = this.$timeout(() => {
      if (!immediate) {
        this._save(documentId, name, values)
          .then(deferred.resolve)
          .catch(deferred.reject);
      }

      this._abortThrottled(documentId, name);
    }, this.AUTOSAVE_DELAY);

    if (!immediate) {
      return deferred.promise;
    }

    return this._save(documentId, name, values);
  }

  _abortThrottled(documentId, name) {
    if (!this._isThrottled(documentId, name)) {
      return false;
    }

    this.$timeout.cancel(this.throttled[documentId][name]);
    delete this.throttled[documentId][name];
    if (!Object.keys(this.throttled[documentId] || {}).length) {
      delete this.throttled[documentId];
    }

    return true;
  }

  _isThrottled(documentId, name) {
    return this.throttled[documentId] && this.throttled[documentId][name];
  }

  reorder({
    documentId = this.getDocumentId(),
    name,
    order,
  }) {
    return this.ContentService.reorderField(documentId, name, order);
  }

  add({ documentId = this.getDocumentId(), name }) {
    return this.ContentService.addField(documentId, name);
  }
}

export default FieldService;
