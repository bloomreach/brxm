/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

import nameUrlFieldsTemplate from './nameUrlFieldsDialog/name-url-fields-dialog.html';
import NameUrlFieldsDialogCtrl from './nameUrlFieldsDialog/name-url-fields-dialog.controller';

const ERROR_MAP = {
  NO_CONTENT: {
    title: 'FEEDBACK_NOT_EDITABLE_HERE_TITLE',
    messageKey: 'FEEDBACK_NO_EDITABLE_CONTENT_MESSAGE',
    linkToContentEditor: true,
  },
  NOT_A_DOCUMENT: {
    title: 'FEEDBACK_NOT_A_DOCUMENT_TITLE',
    linkToContentEditor: true,
    messageKey: 'FEEDBACK_NOT_A_DOCUMENT_MESSAGE',
  },
  NOT_FOUND: {
    title: 'FEEDBACK_NOT_FOUND_TITLE',
    messageKey: 'FEEDBACK_NOT_FOUND_MESSAGE',
    disableContentButtons: true,
  },
  OTHER_HOLDER: {
    title: 'FEEDBACK_NOT_EDITABLE_TITLE',
    messageKey: 'FEEDBACK_HELD_BY_OTHER_USER_MESSAGE',
    hasUser: true,
  },
  REQUEST_PENDING: {
    title: 'FEEDBACK_NOT_EDITABLE_TITLE',
    messageKey: 'FEEDBACK_REQUEST_PENDING_MESSAGE',
  },
  UNAVAILABLE: { // default catch-all
    title: 'FEEDBACK_DEFAULT_TITLE',
    linkToContentEditor: true,
    messageKey: 'FEEDBACK_DEFAULT_MESSAGE',
  },
  UNKNOWN_VALIDATOR: {
    title: 'FEEDBACK_NOT_EDITABLE_HERE_TITLE',
    linkToContentEditor: true,
    messageKey: 'FEEDBACK_NO_EDITABLE_CONTENT_MESSAGE',
  },
};

class createContentStep2Controller {
  constructor(
    $scope,
    $element,
    $timeout,
    $translate,
    $mdConstant,
    $q,
    SidePanelService,
    ChannelService,
    CmsService,
    ContentService,
    DialogService,
    HippoIframeService,
    FeedbackService,
    CreateContentService,
    FieldService,
  ) {
    'ngInject';

    this.$scope = $scope;
    this.$element = $element;
    this.$timeout = $timeout;
    this.$translate = $translate;
    this.$q = $q;

    this.SidePanelService = SidePanelService;
    this.ChannelService = ChannelService;
    this.CmsService = CmsService;
    this.ContentService = ContentService;
    this.DialogService = DialogService;
    this.HippoIframeService = HippoIframeService;
    this.FeedbackService = FeedbackService;
    this.FieldService = FieldService;
    this.CreateContentService = CreateContentService;

    this.defaultTitle = $translate.instant('EDIT_CONTENT');

    CmsService.subscribe('kill-editor', (documentId) => {
      if (this.documentId === documentId) {
        this.close();
      }
    });

    // Prevent the default closing action bound to the escape key by Angular Material.
    // We should show the "unsaved changes" dialog first.
    $element.on('keydown', (e) => {
      if (e.which === $mdConstant.KEY_CODE.ESCAPE) {
        e.stopImmediatePropagation();
        this.close();
      }
    });
  }

  $onInit() {
    this.loadNewDocument();
  }

  _openEditNameUrlDialog() {
    return this.DialogService.show({
      template: nameUrlFieldsTemplate,
      controller: NameUrlFieldsDialogCtrl,
      controllerAs: '$ctrl',
      locals: {
        title: this.$translate.instant('CHANGE_DOCUMENT_NAME'),
        name: this.doc.displayName,
        url: '',
      },
      bindToController: true,
    });
  }

  _submitEditNameUrl(nameUrlObj) {
    this.doc.displayName = nameUrlObj.name;
  }

  editNameUrl() {
    this._openEditNameUrlDialog().then(nameUrlObj => this._submitEditNameUrl(nameUrlObj));
  }

  _resetBeforeStateChange() {
    this.onBeforeStateChange({ callback: message => this._discardAndClose(message) });
  }

  _resetState() {
    delete this.doc;
    delete this.documentId;
    delete this.docType;
    delete this.editing;
    delete this.feedback;
    this.title = this.defaultTitle;
    this._resetForm();
  }

  setFullWidth(state) {
    this.isFullWidth = state;
    this.onFullWidth({ state });
  }

  _resetForm() {
    if (this.form) {
      this.form.$setPristine();
    }
  }

  loadNewDocument() {
    const doc = this.CreateContentService.getDocument();
    this.documentId = doc.id;
    this.FieldService.setDocumentId(doc.id);
    this.loading = true;
    return this.ContentService.getDocumentType(doc.info.type.id)
      .then(docType => this._onLoadSuccess(doc, docType)).finally(() => delete this.loading);
  }

  _onLoadSuccess(doc, docType) {
    this.doc = doc;
    this.docType = docType;
    this.editing = true;

    this.title = this.$translate.instant('CREATE_NEW_DOCUMENT_TYPE', { documentType: docType.displayName });
    this._resizeTextareas();
  }

  // Might be removed, create draft failure will be handled elsewhere
  _onLoadFailure(response) {
    let errorKey;
    let params = {};

    if (this._isErrorInfo(response.data)) {
      const errorInfo = response.data;
      errorKey = errorInfo.reason;
      if (errorInfo.params) {
        params = this._extractErrorParams(errorInfo);
        if (errorInfo.params.displayName) {
          this.title = this.$translate.instant('EDIT_DOCUMENT', errorInfo.params);
        }
      }
    } else if (response.status === 404) {
      errorKey = 'NOT_FOUND';
    } else {
      errorKey = 'UNAVAILABLE';
    }
    this._handleErrorResponse(errorKey, params);
  }

  _noContentResponse(doc) {
    return {
      data: {
        reason: 'NO_CONTENT',
        params: {
          displayName: doc.displayName,
        },
      },
    };
  }

  _extractErrorParams(errorInfo) {
    const params = {};
    if (errorInfo.reason === 'OTHER_HOLDER') {
      params.user = errorInfo.params.userName || errorInfo.params.userId;
    }
    return params;
  }

  _handleErrorResponse(errorKey, params) {
    const error = ERROR_MAP[errorKey];

    if (error) {
      this.feedback = {
        title: error.title,
        message: this.$translate.instant(error.messageKey, params),
        linkToContentEditor: error.linkToContentEditor,
      };
      this.disableContentButtons = error.disableContentButtons;
    }
  }

  _resizeTextareas() {
    // Set initial size of textareas (see Angular Material issue #9745).
    // Use $timeout to ensure that the sidenav has become visible.
    this.$timeout(() => {
      this.$scope.$broadcast('md-resize-textarea');
    });
  }

  saveDocument() {
    this.onSave({ mode: 'edit', options: this.documentId });
    // return this._saveDraft()
    //   .then((savedDoc) => {
    //     this._resetState();
    //     this._resetForm();
    //     this.HippoIframeService.reload();
    //     this.CmsService.reportUsageStatistic('CMSChannelsSaveDocument');
    //     this.onSave({ id: 'edit', options: this.documentId });
    //   })
    //   .catch((response) => {
    //     let params = {};
    //     let errorKey = 'ERROR_UNABLE_TO_SAVE';
    //
    //     if (this._isErrorInfo(response.data)) {
    //       errorKey = `ERROR_${response.data.reason}`;
    //       params = this._extractErrorParams(response.data);
    //     } else if (this._isDocument(response.data)) {
    //       errorKey = 'ERROR_INVALID_DATA';
    //       this._reloadDocumentType();
    //     }
    //
    //     this.FeedbackService.showError(errorKey, params);
    //
    //     return this.$q.reject(); // tell the caller that saving has failed.
    //   });
  }

  _isDocument(obj) {
    return obj && obj.id; // Document has an ID field, ErrorInfo doesn't.
  }

  _isErrorInfo(obj) {
    return obj && obj.reason; // ErrorInfo has a reason field, Document doesn't.
  }

  _reloadDocumentType() {
    this.ContentService.getDocumentType(this.doc.info.type.id)
      .then((docType) => {
        this.docType = docType;
      })
      .catch((response) => {
        this._onLoadFailure(response);
      });
  }

  _saveDraft() {
    if (!this.isDocumentDirty()) {
      return this.$q.resolve();
    }
    return this.ContentService.saveDraft(this.doc);
  }

  isDocumentDirty() {
    return (this.doc && this.doc.info && this.doc.info.dirty) || this._isFormDirty();
  }

  _isFormDirty() {
    return this.form && this.form.$dirty;
  }

  close() {
    return this._discardAndClose()
      .then(() => {
        this._resetState();
        this.SidePanelService.close('right');
      });
  }

  _discardAndClose() {
    return this._confirmDiscardChanges()
      .then(() => {
        // speed up closing the panel by not returning the promise so the draft is deleted asynchronously
        this._deleteDocument();
      });
  }

  _confirmDiscardChanges() {
    const messageParams = {
      documentName: this.doc.displayName,
    };
    const confirm = this.DialogService.confirm()
      .title(this.$translate.instant('DISCARD_DOCUMENT', messageParams))
      .textContent(this.$translate.instant('CONFIRM_DISCARD_NEW_DOCUMENT', messageParams))
      .ok(this.$translate.instant('DISCARD'))
      .cancel(this.$translate.instant('CANCEL'));

    return this.DialogService.show(confirm);
  }

  _deleteDocument() {
    if (this.editing) {
      return this.ContentService.deleteDraft(this.documentId);
    }
    return this.$q.resolve();
  }
}

export default createContentStep2Controller;
