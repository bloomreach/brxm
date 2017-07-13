/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
    this.activeDraftTimers = {};
    this.AUTODRAFT_DELAY = 2000;

    this._focusedInput = null;
    this._customFocusCallback = null;
  }

  shouldPreserveFocus(relatedTarget) {
    const validExpressions = [
      () => relatedTarget.is('.btn-fullwidth'),
      () => relatedTarget.is('.btn-normalwidth'),
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
        this._focusedInput.focus();
      }
    }
  }

  setDocumentId(documentId) {
    this.documentId = documentId;
  }

  getDocumentId() {
    return this.documentId;
  }

  startDraftTimer(fieldName, fieldValue) {
    const documentId = this.getDocumentId();

    if (!this.activeDraftTimers[documentId]) this.activeDraftTimers[documentId] = {};

    this._clearFieldTimer(documentId, fieldName);

    this.activeDraftTimers[documentId][fieldName] = this.$timeout(() => {
      this.draftField(fieldName, fieldValue, documentId);
    }, this.AUTODRAFT_DELAY);
  }

  draftField(fieldName, fieldValue, documentId = this.getDocumentId()) {
    this._clearFieldTimer(documentId, fieldName);
    this.ContentService.draftField(documentId, fieldName, fieldValue);
    this._cleanupTimers(documentId);
  }

  _clearFieldTimer(documentId, fieldName) {
    if (this.activeDraftTimers[documentId] && this.activeDraftTimers[documentId][fieldName]) {
      this.$timeout.cancel(this.activeDraftTimers[documentId][fieldName]);
      delete this.activeDraftTimers[documentId][fieldName];
    }
  }

  _cleanupTimers(documentId) {
    const timers = this.activeDraftTimers[documentId];
    if (timers && Object.keys(timers).length === 0) {
      delete this.activeDraftTimers[documentId];
    }
  }
}

export default FieldService;
