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

import MultiActionDialogCtrl from './multiActionDialog/multiActionDialog.controller';
import multiActionDialogTemplate from './multiActionDialog/multiActionDialog.html';

const ERROR_MAP = {
  NO_CONTENT: {
    titleKey: 'FEEDBACK_NOT_EDITABLE_HERE_TITLE',
    messageKey: 'FEEDBACK_NO_EDITABLE_CONTENT_MESSAGE',
    linkToContentEditor: true,
  },
  NOT_A_DOCUMENT: {
    titleKey: 'FEEDBACK_NOT_A_DOCUMENT_TITLE',
    linkToContentEditor: true,
    messageKey: 'FEEDBACK_NOT_A_DOCUMENT_MESSAGE',
  },
  NOT_FOUND: {
    titleKey: 'FEEDBACK_NOT_FOUND_TITLE',
    messageKey: 'FEEDBACK_NOT_FOUND_MESSAGE',
    disableContentButtons: true,
  },
  OTHER_HOLDER: {
    titleKey: 'FEEDBACK_NOT_EDITABLE_TITLE',
    messageKey: 'FEEDBACK_HELD_BY_OTHER_USER_MESSAGE',
    hasUser: true,
  },
  REQUEST_PENDING: {
    titleKey: 'FEEDBACK_NOT_EDITABLE_TITLE',
    messageKey: 'FEEDBACK_REQUEST_PENDING_MESSAGE',
  },
  UNAVAILABLE: { // default catch-all
    titleKey: 'FEEDBACK_DEFAULT_TITLE',
    linkToContentEditor: true,
    messageKey: 'FEEDBACK_DEFAULT_MESSAGE',
  },
  UNKNOWN_VALIDATOR: {
    titleKey: 'FEEDBACK_NOT_EDITABLE_HERE_TITLE',
    linkToContentEditor: true,
    messageKey: 'FEEDBACK_NO_EDITABLE_CONTENT_MESSAGE',
  },
};

class ContentEditorService {
  constructor($q, $translate, CmsService, ContentService, DialogService, FeedbackService, FieldService) {
    'ngInject';

    this.$q = $q;
    this.$translate = $translate;
    this.CmsService = CmsService;
    this.ContentService = ContentService;
    this.DialogService = DialogService;
    this.FeedbackService = FeedbackService;
    this.FieldService = FieldService;
  }

  open(documentId) {
    this.documentId = document;
    return this._loadDocument(documentId);
  }

  getDocumentId() {
    return this.documentId;
  }

  getDocument() {
    return this.document;
  }

  getDocumentType() {
    return this.documentType;
  }

  isDocumentDirty() {
    return this.documentDirty;
  }

  markDocumentDirty() {
    this.documentDirty = true;
  }

  getError() {
    return this.error;
  }

  isEditing() {
    return angular.isDefined(this.document) && angular.isDefined(this.documentType);
  }

  _loadDocument(id) {
    this.documentId = id;
    this.FieldService.setDocumentId(this.documentId);
    return this.CmsService.closeDocumentWhenValid(id)
      .then(() => this.ContentService.createDraft(id)
        .then((document) => {
          if (this._hasFields(document)) {
            return this.ContentService.getDocumentType(document.info.type.id)
              .then((documentType) => {
                this._onLoadSuccess(document, documentType);
                this._reportUnsupportedFieldTypes(documentType);
              });
          }
          return this.$q.reject(this._noContentResponse(document));
        })
        .catch(response => this._onLoadFailure(response)))
      .catch(() => this._setErrorDraftInvalid());
  }

  _hasFields(document) {
    return document.fields && Object.keys(document.fields).length > 0;
  }

  _noContentResponse(document) {
    return {
      data: {
        reason: 'NO_CONTENT',
        params: {
          displayName: document.displayName,
        },
      },
    };
  }

  _setErrorDraftInvalid() {
    this.error = {
      titleKey: 'FEEDBACK_DRAFT_INVALID_TITLE',
      messageKey: 'FEEDBACK_DRAFT_INVALID_MESSAGE',
      linkToContentEditor: true,
    };
  }

  _onLoadSuccess(document, documentType) {
    this.document = document;
    this.documentType = documentType;
    this.documentDirty = document.info && document.info.dirty;
    delete this.error;

    // TODO: move this to the contentEditor component
    // this._resizeTextareas();
  }

  _reportUnsupportedFieldTypes(documentType) {
    if (documentType.unsupportedFieldTypes) {
      this.CmsService.reportUsageStatistic(
        'VisualEditingUnsupportedFields',
        { unsupportedFieldTypes: documentType.unsupportedFieldTypes.join(',') },
      );
    }
  }

  _onLoadFailure(response) {
    let errorKey;
    let params = {};

    if (this._isErrorInfo(response.data)) {
      const errorInfo = response.data;
      errorKey = errorInfo.reason;
      if (errorInfo.params) {
        params = errorInfo.params;
        params.user = errorInfo.params.userName || errorInfo.params.userId;
      }
    } else if (response.status === 404) {
      errorKey = 'NOT_FOUND';
    } else {
      errorKey = 'UNAVAILABLE';
    }

    this.error = ERROR_MAP[errorKey];
    this.error.messageParams = params;
  }

  save() {
    return this._saveDraft()
      .catch((response) => {
        let params = {};
        let errorKey = 'ERROR_UNABLE_TO_SAVE';

        if (this._isErrorInfo(response.data)) {
          errorKey = `ERROR_${response.data.reason}`;
          params = this._extractErrorParams(response.data);
        } else if (this._isDocument(response.data)) {
          errorKey = 'ERROR_INVALID_DATA';
          this._reloadDocumentType();
        }

        this.FeedbackService.showError(errorKey, params);

        return this.$q.reject(); // tell the caller that saving has failed.
      });
  }

  _saveDraft() {
    if (!this.documentDirty) {
      return this.$q.resolve();
    }
    return this.ContentService.saveDraft(this.document)
      .then(savedDocument => this._onLoadSuccess(savedDocument, this.documentType));
  }

  _isDocument(obj) {
    return obj && obj.id; // Document has an ID field, ErrorInfo doesn't.
  }

  _isErrorInfo(obj) {
    return obj && obj.reason; // ErrorInfo has a reason field, Document doesn't.
  }

  _reloadDocumentType() {
    this.ContentService.getDocumentType(this.document.info.type.id)
      .then((documentType) => {
        this.documentType = documentType;
      })
      .catch((response) => {
        this._onLoadFailure(response);
      });
  }

  confirmPendingChanges(messageKey) {
    return this._confirmSaveOrDiscardChanges(messageKey)
      .then((action) => {
        if (action === 'SAVE') {
          return this._saveDraft();
        }
        return this.$q.resolve();
      });
  }

  _confirmSaveOrDiscardChanges(messageKey) {
    if (!this.documentDirty) {
      // no pending changes, no dialog, continue normally
      return this.$q.resolve('DISCARD');
    }

    const message = this.$translate.instant(messageKey, { documentName: this.document.displayName });
    const title = this.$translate.instant('SAVE_CHANGES_TITLE');

    return this.DialogService.show({
      template: multiActionDialogTemplate,
      controller: MultiActionDialogCtrl,
      controllerAs: '$ctrl',
      locals: {
        title,
        message,
        actions: ['DISCARD', 'SAVE'],
      },
      bindToController: true,
    });
  }

  deleteDraft() {
    if (this.isEditing()) {
      return this.ContentService.deleteDraft(this.document.id);
    }
    return this.$q.resolve();
  }

  close() {
    delete this.documentId;
    delete this.document;
    delete this.documentType;
    delete this.documentDirty;
    delete this.error;
  }
}

export default ContentEditorService;
