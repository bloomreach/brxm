/*
 * Copyright 2018-2020 Hippo B.V. (http://www.onehippo.com)
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
  CANCELABLE_PUBLICATION_REQUEST_PENDING: {
    titleKey: 'FEEDBACK_DOCUMENT_NOT_EDITABLE_TITLE',
    messageKey: 'FEEDBACK_DOCUMENT_CANCELABLE_PUBLICATION_REQUEST_PENDING_MESSAGE',
    cancelRequest: true,
    color: 'hippo-grey-200',
  },
  CORE_PROJECT: {
    titleKey: 'FEEDBACK_DOCUMENT_NOT_EDITABLE_TITLE',
    messageKey: 'FEEDBACK_DOCUMENT_HELD_BY_CORE_PROJECT_MESSAGE',
  },
  CREATE_WITH_UNSUPPORTED_VALIDATOR: {
    titleKey: 'FEEDBACK_DOCUMENT_NOT_EDITABLE_HERE_TITLE',
    messageKey: 'FEEDBACK_DOCUMENT_NO_EDITABLE_CONTENT_MESSAGE',
    linkToContentEditor: true,
  },
  DOCUMENT_INVALID: {
    titleKey: 'FEEDBACK_DOCUMENT_INVALID_TITLE',
    messageKey: 'FEEDBACK_DOCUMENT_INVALID_MESSAGE',
    linkToContentEditor: true,
  },
  DOES_NOT_EXIST: {
    titleKey: 'FEEDBACK_NOT_FOUND_TITLE',
    messageKey: 'FEEDBACK_NOT_FOUND_MESSAGE',
    disableContentButtons: true,
  },
  NO_CONTENT: {
    titleKey: 'FEEDBACK_DOCUMENT_NOT_EDITABLE_HERE_TITLE',
    messageKey: 'FEEDBACK_DOCUMENT_NO_EDITABLE_CONTENT_MESSAGE',
    linkToContentEditor: true,
  },
  NOT_A_DOCUMENT: {
    titleKey: 'FEEDBACK_NOT_A_DOCUMENT_TITLE',
    messageKey: 'FEEDBACK_NOT_A_DOCUMENT_MESSAGE',
    linkToContentEditor: true,
  },
  NOT_EDITABLE: {
    titleKey: 'FEEDBACK_DOCUMENT_NOT_EDITABLE_TITLE',
    messageKey: 'FEEDBACK_DOCUMENT_NOT_EDITABLE_MESSAGE',
    linkToContentEditor: true,
  },
  NOT_FOUND: {
    titleKey: 'FEEDBACK_NOT_FOUND_TITLE',
    messageKey: 'FEEDBACK_NOT_FOUND_MESSAGE',
    disableContentButtons: true,
  },
  OTHER_HOLDER: {
    titleKey: 'FEEDBACK_DOCUMENT_NOT_EDITABLE_TITLE',
    messageKey: 'FEEDBACK_DOCUMENT_HELD_BY_OTHER_USER_MESSAGE',
  },
  PART_OF_PROJECT: {
    titleKey: 'FEEDBACK_DOCUMENT_NOT_EDITABLE_TITLE',
    messageKey: 'FEEDBACK_DOCUMENT_PART_OF_PROJECT_MESSAGE',
    hasUser: true,
  },
  PROJECT_INVALID_STATE: {
    titleKey: 'FEEDBACK_DOCUMENT_NOT_EDITABLE_TITLE',
    messageKey: 'FEEDBACK_DOCUMENT_PROJECT_INVALID_STATE',
  },
  PROJECT_NOT_FOUND: {
    titleKey: 'FEEDBACK_DOCUMENT_NOT_EDITABLE_TITLE',
    messageKey: 'FEEDBACK_PROJECT_DOCUMENT_NOT_FOUND',
  },
  REQUEST_PENDING: {
    titleKey: 'FEEDBACK_DOCUMENT_NOT_EDITABLE_TITLE',
    messageKey: 'FEEDBACK_DOCUMENT_REQUEST_PENDING_MESSAGE',
  },
  UNAVAILABLE: {
    titleKey: 'FEEDBACK_DEFAULT_TITLE',
    messageKey: 'FEEDBACK_DEFAULT_MESSAGE',
    linkToContentEditor: true,
  },
  UNKNOWN_ERROR: {
    titleKey: 'FEEDBACK_DOCUMENT_NOT_EDITABLE_TITLE',
    messageKey: 'FEEDBACK_UNKNOWN_ERROR',
  },
};

function isDocument(object) {
  return object && object.id; // Document has an ID field, ErrorInfo doesn't.
}

function isErrorInfo(object) {
  return object && object.reason; // ErrorInfo has a reason field, Document doesn't.
}

function hasFields(document) {
  return document.fields && Object.keys(document.fields).length > 0;
}

function normalizeFields(fields, values, [node, ...path]) {
  return Object.fromEntries(Object.entries(values).map(([id, value]) => {
    if (node != null && id !== node) {
      return [id, value];
    }

    const field = fields.find(({ id: fieldId }) => fieldId === id);
    const normalized = normalizeValues(field, value, path); // eslint-disable-line no-use-before-define

    return [id, field.multiple ? normalized : normalized[0]];
  }));
}

function normalizeValues(field, values, path) {
  const node = field.multiple && path.length
    ? parseInt(path.shift(), 10)
    : undefined;

  return values.map((value, index) => (node != null && index !== node
    ? value
    : normalizeValue(field, value, path))); // eslint-disable-line no-use-before-define
}

function normalizeValue(field, value, path) {
  if (field.type === 'COMPOUND') {
    return normalizeFields(field.fields, value.fields, path);
  }

  if (field.type === 'CHOICE') {
    return normalizeValue(
      field.choices[value.chosenId],
      value.chosenValue,
      path,
    );
  }

  return value.value;
}

class ErrorResponse {
  constructor(reason, params) {
    this.data = {
      reason,
      params,
    };
  }
}

class ContentEditorService {
  constructor(
    $q,
    $state,
    $translate,
    ChannelService,
    CmsService,
    ContentService,
    DialogService,
    DocumentWorkflowService,
    FeedbackService,
    FieldService,
  ) {
    'ngInject';

    this.$q = $q;
    this.$state = $state;
    this.$translate = $translate;
    this.ChannelService = ChannelService;
    this.CmsService = CmsService;
    this.ContentService = ContentService;
    this.DialogService = DialogService;
    this.DocumentWorkflowService = DocumentWorkflowService;
    this.FeedbackService = FeedbackService;
    this.FieldService = FieldService;
  }

  _setDocumentId(id) {
    this.documentId = id;
    this.FieldService.setDocumentId(this.documentId);
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

  getDocumentDisplayName() {
    if (this.document) {
      return this.document.displayName;
    }

    return this.error && this.error.messageParams && this.error.messageParams.displayName;
  }

  getDocumentErrorMessages() {
    if (this.document) {
      return this.document.info.errorMessages || [];
    }
    return [];
  }

  getDocumentFieldValue(...path) {
    return path.reduce(
      (result, key) => result && result[key],
      normalizeFields(this.getDocumentType().fields, this.getDocument().fields, path),
    );
  }

  getPublicationState() {
    return this.publicationState;
  }

  getError() {
    return this.error;
  }

  markDocumentDirty() {
    this.documentDirty = true;
  }

  isDocumentDirty() {
    return this.documentDirty;
  }

  isPristine() {
    return !this.isDocumentDirty() || this.isKilled();
  }

  isEditing() {
    return angular.isDefined(this.document) && angular.isDefined(this.documentType);
  }

  isPublishAllowed() {
    return this.canPublish || this.canRequestPublication;
  }

  kill() {
    this.killed = true;
  }

  isKilled() {
    return this.killed;
  }

  open(documentId) {
    this.close();

    return this._loadDocument(documentId);
  }

  async _loadDocument(id) {
    this._setDocumentId(id);

    try {
      // close previously opened document if there are no validation errors
      await this._closeDocumentWhenValid(id);

      // create an editable instance
      const document = await this.ContentService.getEditableDocument(id);

      // don't edit documents without fields
      if (!hasFields(document)) {
        throw new ErrorResponse('NO_CONTENT', { displayName: document.displayName });
      }

      // ensure the document has a valid type
      await this.loadDocumentType(document);
      return document;
    } catch (e) {
      this._onLoadFailure(e);
      return null;
    }
  }

  async _closeDocumentWhenValid(id) {
    try {
      await this.CmsService.closeDocumentWhenValid(id)
        .then(() => this.ChannelService.updateNavLocation());
    } catch (e) {
      throw new ErrorResponse('DOCUMENT_INVALID');
    }
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

  _setErrorDocumentInvalid() {
    this._clearDocument();
    this.error = {
      titleKey: 'FEEDBACK_DOCUMENT_INVALID_TITLE',
      messageKey: 'FEEDBACK_DOCUMENT_INVALID_MESSAGE',
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
    this.canKeepDraft = document.info && document.info.canKeepDraft;
    this.retainable = document.info && document.info.retainable;

    delete this.error;
  }

  _reportUnsupportedFieldTypes(documentType) {
    if (!documentType.unsupportedFieldTypes) {
      return;
    }

    this.CmsService.reportUsageStatistic(
      'VisualEditingUnsupportedFields',
      { unsupportedFieldTypes: documentType.unsupportedFieldTypes.join(',') },
    );
  }

  _onLoadFailure(response) {
    this._clearDocument();

    let errorKey;
    let params = null;

    if (isErrorInfo(response.data)) {
      const errorInfo = response.data;
      errorKey = errorInfo.reason;
      params = this._extractErrorParams(errorInfo);

      if (errorInfo.params) {
        this.publicationState = errorInfo.params.publicationState;
      }
    } else {
      errorKey = response.status === 404
        ? 'NOT_FOUND'
        : 'UNAVAILABLE';
    }

    if (!ERROR_MAP[errorKey]) {
      this.error = ERROR_MAP.UNKNOWN_ERROR;
      this.error.messageParams = { errorKey };
    } else {
      this.error = ERROR_MAP[errorKey];
      if (params) {
        this.error.messageParams = params;
      }
    }
  }

  save(force) {
    this.document.info.retainable = false;
    this.retainable = false;
    return this._keepDraftOrSave(force);
  }

  _keepDraftOrSave(force) {
    return this._saveDocument(force)
      .catch((response) => {
        const result = this.$q.reject(); // tell the caller that saving has failed.

        let params;
        let errorKey = 'ERROR_UNABLE_TO_SAVE';

        if (isDocument(response.data)) {
          this._reloadDocumentType();
          this.document = response.data;

          const count = response.data.info && response.data.info.errorCount;
          errorKey = count !== 1
            ? 'DOCUMENT_CONTAINS_MULTIPLE_ERRORS'
            : 'DOCUMENT_CONTAINS_ONE_ERROR';
          params = { name: response.data.displayName, count };
        }

        if (isErrorInfo(response.data)) {
          const errorInfo = response.data;
          errorKey = `ERROR_${errorInfo.reason}`;
          params = this._extractErrorParams(errorInfo);
        }

        if (params) {
          this.FeedbackService.showError(errorKey, params);
        } else {
          this.FeedbackService.showError(errorKey);
        }

        return result;
      });
  }

  keepDraft() {
    if (this.document && this.document.info) {
      this.document.info.retainable = true;
      return this.ContentService.saveDocument(this.document)
        .then(savedDocument => this._onLoadSuccess(savedDocument, this.documentType));
    }
    return this.$q.resolve();
  }

  isRetainable() {
    return this.retainable;
  }

  isKeepDraftAllowed() {
    return this.canKeepDraft;
  }

  _saveDocument(force) {
    if (!force && !this.documentDirty) {
      return this.$q.resolve();
    }

    return this.ContentService.saveDocument(this.document)
      .then(savedDocument => this._onLoadSuccess(savedDocument, this.documentType));
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

  confirmClose(messageKey, messageParams, titleKey) {
    return this.confirmSaveOrDiscardChanges(messageKey, messageParams, titleKey)
      .then(() => this.discardChanges())
      .then(() => this.close());
  }

  /**
   * Possible return values:
   * - resolved promise with value 'SAVE' when changes have been saved
   * - resolved promise with value 'DISCARD' when changes have been discarded
   * - rejected promise when user canceled
   */
  confirmSaveOrDiscardChanges(messageKey, messageParams, titleKey) {
    return this._askSaveOrDiscardChanges(messageKey, messageParams, titleKey)
      .then((action) => {
        if (action === 'SAVE') {
          return this.save()
            .then(() => action); // let caller know that changes have been saved
        }

        return this.$q.resolve(action); // let caller know that changes have not been saved
      });
  }

  _askSaveOrDiscardChanges(messageKey, messageParams = {}, titleKey = 'SAVE_DOCUMENT_CHANGES_TITLE') {
    if (this.isPristine()) {
      return this.$q.resolve('DISCARD');
    }

    const translateParams = angular.copy(messageParams);
    translateParams.documentName = this.document.displayName;

    const message = this.$translate.instant(messageKey, translateParams);
    const title = this.$translate.instant(titleKey);

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

  discardChanges() {
    if (!this.isEditing() || this.isKilled()) {
      return this.$q.resolve();
    }

    return this.ContentService.discardChanges(this.document.id);
  }

  deleteDocument() {
    if (!this.isEditing() || this.isKilled()) {
      return this.$q.resolve();
    }

    return this.ContentService.deleteDocument(this.document.id)
      .catch((error) => {
        this.FeedbackService.showError(`ERROR_${error.data.reason}`, error.data.params);
      });
  }

  confirmPublication() {
    const params = { documentName: this.document.displayName };
    const textContent = this.$translate.instant(this._confirmPublicationTextKey(), params);
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
      return this.documentDirty
        ? 'CONFIRM_PUBLISH_DIRTY_DOCUMENT' : 'CONFIRM_PUBLISH_DOCUMENT';
    }

    return this.documentDirty
      ? 'CONFIRM_REQUEST_PUBLICATION_OF_DIRTY_DOCUMENT' : 'CONFIRM_REQUEST_PUBLICATION_OF_DOCUMENT';
  }

  _confirmPublicationOkKey() {
    if (this.canPublish) {
      return this.isDocumentDirty() ? 'SAVE_AND_PUBLISH' : 'PUBLISH';
    }

    return this.isDocumentDirty() ? 'SAVE_AND_REQUEST_PUBLICATION' : 'REQUEST_PUBLICATION';
  }

  async publish() {
    const notificationKey = this.canPublish ? 'NOTIFICATION_DOCUMENT_PUBLISHED' : 'NOTIFICATION_PUBLICATION_REQUESTED';
    const errorKey = this.canPublish ? 'ERROR_PUBLISH_DOCUMENT_FAILED' : 'ERROR_REQUEST_PUBLICATION_FAILED';
    const messageParams = { documentName: this.document.displayName };

    return this.ContentService
      .discardChanges(this.documentId)
      .then(() => this._publish()
        .then(() => this.FeedbackService.showNotification(notificationKey, messageParams))
        .then(() => this._reportPublishAction())
        .finally(() => this.ContentService.getEditableDocument(this.documentId)
          .then((saveDocument) => {
            this._onLoadSuccess(saveDocument, this.documentType);
          })
          .catch((response) => {
            if (this.canPublish) {
              // Document published. Getting an editable document should not have failed, so set the same error as
              // when getting an editable document fails.
              this._setErrorDocumentInvalid();
            } else {
              // Publication requested. Getting an editable document is expected to fail; _onLoadFailure will set an
              // error and remove the document so the 'document not editable' message is shown and the editor is
              // removed.
              this._onLoadFailure(response);
            }
            // Don't reject the promise: that would show the "workflow action failed" message, yet the workflow
            // action has succeeded. The error that has been set will make it clear to the user that the document
            // could not be created.
          })))
      .catch(() => {
        this.FeedbackService.showError(errorKey, messageParams);

        return this.$q.reject();
      });
  }

  _publish() {
    return this.canPublish
      ? this.DocumentWorkflowService.publish(this.documentId)
      : this.DocumentWorkflowService.requestPublication(this.documentId);
  }

  _reportPublishAction() {
    const eventName = this.canPublish ? 'VisualEditingLightboxPublish' : 'VisualEditingLightboxRequestPub';
    this.CmsService.reportUsageStatistic(eventName);
  }

  cancelRequestPublication() {
    return this.DocumentWorkflowService.cancelRequest(this.documentId)
      .catch(() => {
        this.FeedbackService.showError('ERROR_CANCEL_REQUEST_PUBLICATION_FAILED', {
          documentName: this.error && this.error.messageParams && this.error.messageParams.displayName,
        });

        return this.$q.reject();
      })
      .finally(() => this._loadDocument(this.documentId));
  }

  close() {
    this._clearDocument();

    delete this.documentId;
    delete this.documentType;
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
