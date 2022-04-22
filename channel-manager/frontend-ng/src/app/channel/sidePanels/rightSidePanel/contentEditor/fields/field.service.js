/*
 * Copyright 2017-2022 Bloomreach (https://bloomreach.com)
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
  constructor($timeout, $rootScope, $q, ContentService, HippoIframeService) {
    'ngInject';

    this.$rootScope = $rootScope;
    this.$timeout = $timeout;
    this.$q = $q;
    this.ContentService = ContentService;
    this.HippoIframeService = HippoIframeService;

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

  setup(documentId) {
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
    this._abortThrottled(documentId, name);

    if (throttle) {
      return this._throttle(documentId, name, values);
    }

    try {
      return this._save(documentId, name, values);
    } finally {
      this._abortThrottled(documentId, name);
    }
  }

  _save(documentId, name, values) {
    return this.ContentService.saveField(documentId, name, this.cleanValues(values))
      .then((result) => {
        this.$rootScope.$emit('field:change');
        return result;
      });
  }

  _throttle(documentId, name, values) {
    if (!this.throttled[documentId]) {
      this.throttled[documentId] = {};
    }

    const deferred = this.$q.defer();
    this.throttled[documentId][name] = this.$timeout(() => {
      this._save(documentId, name, values)
        .then(deferred.resolve)
        .catch(deferred.reject);

      this._abortThrottled(documentId, name);
    }, this.AUTOSAVE_DELAY);

    return deferred.promise;
  }

  _abortThrottled(documentId, name) {
    if (!this._isThrottled(documentId, name)) {
      return;
    }

    this.$timeout.cancel(this.throttled[documentId][name]);
    delete this.throttled[documentId][name];
    if (!Object.keys(this.throttled[documentId] || {}).length) {
      delete this.throttled[documentId];
    }
  }

  _isThrottled(documentId, name) {
    return this.throttled[documentId] && this.throttled[documentId][name];
  }

  async reorder({
    documentId = this.getDocumentId(),
    name,
    order,
  }) {
    const reorder = await this.ContentService.reorderField(documentId, name, order);
    await this.HippoIframeService.reload();
    return reorder;
  }

  async add({ documentId = this.getDocumentId(), name }) {
    const addField = await this.ContentService.addField(documentId, name);
    await this.HippoIframeService.reload();
    return addField;
  }

  async remove({ documentId = this.getDocumentId(), name }) {
    const removeField = await this.ContentService.removeField(documentId, name)
    await this.HippoIframeService.reload();
    return removeField;
  }
}

export default FieldService;
