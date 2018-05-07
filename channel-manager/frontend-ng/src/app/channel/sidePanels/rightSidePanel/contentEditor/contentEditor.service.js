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
  DOES_NOT_EXIST: {
    titleKey: 'FEEDBACK_NOT_FOUND_TITLE',
    messageKey: 'FEEDBACK_NOT_FOUND_MESSAGE',
    disableContentButtons: true,
  },
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
  NOT_EDITABLE: {
    titleKey: 'FEEDBACK_NOT_EDITABLE_TITLE',
    linkToContentEditor: true,
    messageKey: 'FEEDBACK_NOT_EDITABLE_MESSAGE',
  },
  NOT_FOUND: {
    titleKey: 'FEEDBACK_NOT_FOUND_TITLE',
    messageKey: 'FEEDBACK_NOT_FOUND_MESSAGE',
    disableContentButtons: true,
  },
  OTHER_HOLDER: {
    titleKey: 'FEEDBACK_NOT_EDITABLE_TITLE',
    messageKey: 'FEEDBACK_HELD_BY_OTHER_USER_MESSAGE',
  },
  CANCELABLE_PUBLICATION_REQUEST_PENDING: {
    titleKey: 'FEEDBACK_NOT_EDITABLE_TITLE',
    messageKey: 'FEEDBACK_CANCELABLE_PUBLICATION_REQUEST_PENDING_MESSAGE',
    cancelRequest: true,
    color: 'hippo-grey-200',
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
  PART_OF_PROJECT: {
    title: 'FEEDBACK_NOT_EDITABLE_TITLE',
    messageKey: 'FEEDBACK_PART_OF_PROJECT_MESSAGE',
    hasUser: true,
  },
  CORE_PROJECT: {
    title: 'FEEDBACK_NOT_EDITABLE_TITLE',
    messageKey: 'FEEDBACK_HELD_BY_CORE_PROJECT_MESSAGE',
  },
  PROJECT_INVALID_STATE: {
    title: 'FEEDBACK_NOT_EDITABLE_TITLE',
    messageKey: 'FEEDBACK_PROJECT_INVALID_STATE',
  },
  PROJECT_NOT_FOUND: {
    title: 'FEEDBACK_NOT_EDITABLE_TITLE',
    messageKey: 'FEEDBACK_PROJECT_NOT_FOUND',
  },
};

class ContentEditorService {
  constructor($q, $translate, CmsService, ContentService, DialogService, FeedbackService, FieldService, WorkflowService) {
    'ngInject';

    this.$q = $q;
    this.$translate = $translate;
    this.CmsService = CmsService;
    this.ContentService = ContentService;
    this.DialogService = DialogService;
    this.FeedbackService = FeedbackService;
    this.FieldService = FieldService;
    this.WorkflowService = WorkflowService;
  }

  open(documentId) {
    this.close();
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

  getPublicationState() {
    return this.publicationState;
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

  isPublishAllowed() {
    return this.canPublish || this.canRequestPublication;
  }

  _loadDocument(id) {
    this._setDocumentId(id);

    return this.CmsService.closeDocumentWhenValid(id)
      .then(() => this.ContentService.createDraft(id)
        .then((document) => {
          if (this._hasFields(document)) {
            return this.loadDocumentType(document);
          }
          return this.$q.reject(this._noContentResponse(document));
        })
        .catch(response => this._onLoadFailure(response)))
      .catch(() => this._setErrorDraftInvalid());
  }

  _setDocumentId(id) {
    this.documentId = id;
    this.FieldService.setDocumentId(this.documentId);
  }

  _hasFields(document) {
    return document.fields && Object.keys(document.fields).length > 0;
  }

  loadDocumentType(document) {
    this._setDocumentId(document.id);
    return this.ContentService.getDocumentType(document.info.type.id)
      .then((documentType) => {
        this._onLoadSuccess(document, documentType);
        this._reportUnsupportedFieldTypes(documentType);
        return documentType;
      });
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
    this._clearDocument();
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
    this.canPublish = document.info && document.info.canPublish;
    this.canRequestPublication = document.info && document.info.canRequestPublication;
    this.publicationState = document.info && document.info.publicationState;

    delete this.error;
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
    this._clearDocument();

    let errorKey;
    let params = null;

    if (this._isErrorInfo(response.data)) {
      const errorInfo = response.data;
      errorKey = errorInfo.reason;
      params = this._extractErrorParams(errorInfo);

      if (errorInfo.params) {
        this.publicationState = errorInfo.params.publicationState;
      }
    } else if (response.status === 404) {
      errorKey = 'NOT_FOUND';
    } else {
      errorKey = 'UNAVAILABLE';
    }

    this.error = ERROR_MAP[errorKey];
    if (params) {
      this.error.messageParams = params;
    }
  }

  save() {
    return this._saveDraft()
      .catch((response) => {
        let params;
        let errorKey = 'ERROR_UNABLE_TO_SAVE';

        if (this._isErrorInfo(response.data)) {
          const errorInfo = response.data;
          errorKey = `ERROR_${errorInfo.reason}`;
          params = this._extractErrorParams(errorInfo);
        } else if (this._isDocument(response.data)) {
          errorKey = 'ERROR_INVALID_DATA';
          this._reloadDocumentType();
        }

        if (params) {
          this.FeedbackService.showError(errorKey, params);
        } else {
          this.FeedbackService.showError(errorKey);
        }

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

  _extractErrorParams(errorInfo) {
    if (!angular.isDefined(errorInfo.params)) {
      return undefined;
    }

    const params = angular.copy(errorInfo.params);

    const user = params.userName || params.userId;
    if (user) {
      params.user = user;
      delete params.userId;
      delete params.userName;
    }

    delete params.publicationState;

    if (errorInfo.reason === 'PART_OF_PROJECT') {
      params.projectName = errorInfo.params.projectName;
      params.projectState = errorInfo.params.projectState;
    }

    return params;
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

  kill() {
    this.killed = true;
  }

  confirmDiscardChanges(messageKey, titleKey) {
    if (this._doNotConfirm()) {
      return this.$q.resolve();
    }
    const translateParams = {
      documentName: this.document.displayName,
    };

    const confirm = this.DialogService.confirm()
      .textContent(this.$translate.instant(messageKey, translateParams))
      .ok(this.$translate.instant('DISCARD'))
      .cancel(this.$translate.instant('CANCEL'));

    if (titleKey) {
      confirm.title(this.$translate.instant(titleKey, translateParams));
    }

    return this.DialogService.show(confirm);
  }

  /**
   * Possible return values:
   * - resolved promise with value 'SAVE' when changes have been saved
   * - resolved promise with value 'DISCARD' when changes have been discarded
   * - rejected promise when user canceled
   */
  confirmSaveOrDiscardChanges(messageKey) {
    return this._askSaveOrDiscardChanges(messageKey)
      .then((action) => {
        switch (action) {
          case 'SAVE':
            return this._saveDraft()
              .then(() => action); // let caller know that changes have been saved
          default:
            return this.$q.resolve(action); // let caller know that changes have not been saved
        }
      });
  }

  _askSaveOrDiscardChanges(messageKey) {
    if (this._doNotConfirm()) {
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

  _doNotConfirm() {
    return !this.documentDirty // no pending changes, no dialog, continue normally
      || this.killed; // editor was killed, don't show dialog
  }

  deleteDraft() {
    if (this.isEditing() && !this.killed) {
      return this.ContentService.discardChanges(this.document.id);
    }
    return this.$q.resolve();
  }

  deleteDocument() {
    if (this.isEditing() && !this.killed) {
      return this.ContentService.deleteDocument(this.document.id)
        .catch((error) => {
          this.FeedbackService.showError(`ERROR_${error.data.reason}`, error.data.params);
        });
    }
    return this.$q.resolve();
  }

  confirmPublication() {
    const textContent = this.$translate.instant(this._confirmPublicationTextKey(), { documentName: this.document.displayName });
    const ok = this.$translate.instant(this._confirmPublicationOkKey());
    const cancel = this.$translate.instant('CANCEL');

    const confirm = this.DialogService.confirm()
      .textContent(textContent)
      .ok(ok)
      .cancel(cancel);

    return this.DialogService.show(confirm)
      .catch(() => {
        this._reportPublishCancelAction();
        return this.$q.reject();
      });
  }

  _reportPublishCancelAction() {
    const eventName = this.canPublish ? 'VisualEditingLightboxCancel' : 'VisualEditingLightboxRequestPubCancel';
    this.CmsService.reportUsageStatistic(eventName);
  }

  _confirmPublicationTextKey() {
    if (this.canPublish) {
      return this.documentDirty ?
        'CONFIRM_PUBLISH_DIRTY_DOCUMENT' : 'CONFIRM_PUBLISH_DOCUMENT';
    }
    return this.documentDirty ?
      'CONFIRM_REQUEST_PUBLICATION_OF_DIRTY_DOCUMENT' : 'CONFIRM_REQUEST_PUBLICATION_OF_DOCUMENT';
  }

  _confirmPublicationOkKey() {
    if (this.canPublish) {
      return this.documentDirty ? 'SAVE_AND_PUBLISH' : 'PUBLISH';
    }
    return this.documentDirty ? 'SAVE_AND_REQUEST_PUBLICATION' : 'REQUEST_PUBLICATION';
  }

  publish() {
    const workflowAction = this.canPublish ? 'publish' : 'requestPublication';
    const notificationKey = this.canPublish ? 'NOTIFICATION_DOCUMENT_PUBLISHED' : 'NOTIFICATION_PUBLICATION_REQUESTED';
    const errorKey = this.canPublish ? 'ERROR_PUBLISH_DOCUMENT_FAILED' : 'ERROR_REQUEST_PUBLICATION_FAILED';
    const messageParams = { documentName: this.document.displayName };

    return this.ContentService
      .discardChanges(this.documentId)
      .then(() =>
        this.WorkflowService.createWorkflowAction(this.documentId, workflowAction)
          .then(() => this.FeedbackService.showNotification(notificationKey, messageParams))
          .then(() => this._reportPublishAction())
          .finally(() =>
            this.ContentService.createDraft(this.documentId)
              .then((draftDocument) => {
                this._onLoadSuccess(draftDocument, this.documentType);
              })
              .catch((response) => {
                if (this.canPublish) {
                  // Document published. Creating the draft should not have failed, so set the same error as when
                  // opening a document fails.
                  this._setErrorDraftInvalid();
                } else {
                  // Publication requested. Creating the draft is expected to fail; _onLoadFailure will set an error and
                  // remove the document so the 'document not editable' message is shown and the editor is removed.
                  this._onLoadFailure(response);
                }
                // Don't reject the promise: that would show the "workflow action failed" message, yet the workflow
                // action has succeeded. The error that has been set will make it clear to the user that the draft
                // could not be created.
              }),
          ),
      )
      .catch(() => {
        this.FeedbackService.showError(errorKey, messageParams);
        return this.$q.reject();
      });
  }

  _reportPublishAction() {
    const eventName = this.canPublish ? 'VisualEditingLightboxPublish' : 'VisualEditingLightboxRequestPub';
    this.CmsService.reportUsageStatistic(eventName);
  }

  cancelRequestPublication() {
    return this.WorkflowService.createWorkflowAction(this.documentId, 'cancelRequest')
      .catch(() => {
        const errorData = {
          documentName: this.error && this.error.messageParams && this.error.messageParams.displayName
            ? this.error.messageParams.displayName
            : null,
        };
        this.FeedbackService.showError('ERROR_CANCEL_REQUEST_PUBLICATION_FAILED', errorData);
        return this.$q.reject();
      })
      .finally(() => this._loadDocument(this.documentId));
  }

  close() {
    delete this.documentId;
    delete this.documentType;
    this._clearDocument();
    delete this.error;
    delete this.killed;
  }

  _clearDocument() {
    delete this.document;
    delete this.documentDirty;
    delete this.canPublish;
    delete this.canRequestPublication;
    delete this.publicationState;
  }
}

export default ContentEditorService;
