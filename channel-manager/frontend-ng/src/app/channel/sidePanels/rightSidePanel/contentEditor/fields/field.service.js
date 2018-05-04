/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
  constructor($timeout, ContentService) {
    'ngInject';

    this.$timeout = $timeout;
    this.ContentService = ContentService;

    this.documentId = null;
    this.activeSaveTimers = {};
    this.AUTOSAVE_DELAY = 2000;

    this._focusedInput = null;
    this._customFocusCallback = null;
  }

  shouldPreserveFocus(relatedTarget) {
    // validExpressions is an array containing functions.
    // The functions will be evaluated, and if at least one of them is true,
    // "true" will be returned, therefore input focus will be preserved. Otherwise, false.
    const validExpressions = [
      () => relatedTarget.is('.btn-fullwidth') || relatedTarget.is('.btn-normalwidth'),
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

  startSaveTimer(fieldName, fieldValue) {
    const documentId = this.getDocumentId();

    if (!this.activeSaveTimers[documentId]) this.activeSaveTimers[documentId] = {};

    this._clearFieldTimer(documentId, fieldName);

    this.activeSaveTimers[documentId][fieldName] = this.$timeout(() => {
      this.saveField(fieldName, fieldValue, documentId);
    }, this.AUTOSAVE_DELAY);
  }

  saveField(fieldName, fieldValue, documentId = this.getDocumentId()) {
    this._clearFieldTimer(documentId, fieldName);
    this.ContentService.saveField(documentId, fieldName, fieldValue);
    this._cleanupTimers(documentId);
  }

  _clearFieldTimer(documentId, fieldName) {
    if (this.activeSaveTimers[documentId] && this.activeSaveTimers[documentId][fieldName]) {
      this.$timeout.cancel(this.activeSaveTimers[documentId][fieldName]);
      delete this.activeSaveTimers[documentId][fieldName];
    }
  }

  _cleanupTimers(documentId) {
    const timers = this.activeSaveTimers[documentId];
    if (timers && Object.keys(timers).length === 0) {
      delete this.activeSaveTimers[documentId];
    }
  }
}

export default FieldService;
