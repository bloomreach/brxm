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

import MultiActionDialogCtrl from './multiActionDialog/multiActionDialog.controller';
import multiActionDialogTemplate from './multiActionDialog/multiActionDialog.html';

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

class RightSidePanelCtrl {
  constructor(
    $scope,
    $element,
    $mdConstant,
    $timeout,
    $translate,
    $q,
    SidePanelService,
    ChannelService,
    CmsService,
    ContentService,
    DialogService,
    HippoIframeService,
    FeedbackService,
    localStorageService,
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
    this.localStorageService = localStorageService;
    this.FieldService = FieldService;

    this.defaultTitle = $translate.instant('EDIT_CONTENT');
    this.closeLabel = $translate.instant('CLOSE');
    this.cancelLabel = $translate.instant('CANCEL');
    this.deleteDraftOnClose = true;

    this.lastSavedWidth = null;
    this.isFullWidth = false;

    SidePanelService.initialize('right', $element.find('.right-side-panel'),
      // onOpen
      (documentId) => {
        this.openDocument(documentId);
        this._onOpen();
      },
      // onClose
      () => this._releaseDocument());

    // Prevent the default closing action bound to the escape key by Angular Material.
    // We should show the "unsaved changes" dialog first.
    $element.on('keydown', (e) => {
      if (e.which === $mdConstant.KEY_CODE.ESCAPE) {
        e.stopImmediatePropagation();
        this.close();
      }
    });

    CmsService.subscribe('kill-editor', (documentId) => {
      if (this.documentId === documentId) {
        this.deleteDraftOnClose = false;
        this._closePanel();
      }
    });
  }

  $onInit() {
    this.lastSavedWidth = this.localStorageService.get('rightSidePanelWidth') || '440px';
  }

  openDocument(documentId) {
    if (this.documentId === documentId) {
      return;
    }
    this._dealWithPendingChanges('SAVE_CHANGES_ON_BLUR_MESSAGE', () => {
      this._deleteDraft().finally(() => {
        this._resetState();
        this._loadDocument(documentId);
      });
    });
  }

  _resetState() {
    delete this.doc;
    delete this.documentId;
    delete this.docType;
    delete this.editing;
    delete this.feedback;
    delete this.disableContentButtons;

    this.title = this.defaultTitle;
    this.deleteDraftOnClose = true;

    this._resetForm();
  }

  _resetForm() {
    if (this.form) {
      this.form.$setPristine();
    }
  }

  _loadDocument(id) {
    this.documentId = id;
    this.FieldService.setDocumentId(this.documentId);
    this.loading = true;
    this.CmsService.closeDocumentWhenValid(id)
      .then(() => this.ContentService.createDraft(id)
          .then((doc) => {
            if (this._hasFields(doc)) {
              return this.ContentService.getDocumentType(doc.info.type.id)
                .then(docType => this._onLoadSuccess(doc, docType));
            }
            return this.$q.reject(this._noContentResponse(doc));
          })
          .catch(response => this._onLoadFailure(response)))
      .catch(() => this._showFeedbackDraftInvalid())
      .finally(() => delete this.loading);
  }

  _showFeedbackDraftInvalid() {
    this.feedback = {
      title: 'FEEDBACK_DRAFT_INVALID_TITLE',
      message: this.$translate.instant('FEEDBACK_DRAFT_INVALID_MESSAGE'),
      linkToContentEditor: true,
    };
  }

  _hasFields(doc) {
    return doc.fields && Object.keys(doc.fields).length > 0;
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
  _onLoadSuccess(doc, docType) {
    this.doc = doc;
    this.docType = docType;
    this.editing = true;

    if (this.doc.displayName) {
      this.title = this.$translate.instant('EDIT_DOCUMENT', this.doc);
    }
    this._resizeTextareas();
  }

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

  isLockedOpen() {
    return this.SidePanelService.isOpen('right');
  }

  saveDocument() {
    return this._saveDraft()
      .then((savedDoc) => {
        this.doc = savedDoc;
        this._resetForm();
        this.HippoIframeService.reload();
        this.CmsService.reportUsageStatistic('CMSChannelsSaveDocument');
      })
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

  openContentEditor(mode, isPromptUnsavedChanges = true) {
    this.deleteDraftOnClose = false;

    if (!isPromptUnsavedChanges) {
      this._closePanelAndOpenContent(mode);
      return;
    }
    const messageKey = mode === 'view'
      ? 'SAVE_CHANGES_ON_PUBLISH_MESSAGE'
      : 'SAVE_CHANGES_ON_SWITCH_TO_CONTENT_EDITOR_MESSAGE';

    this._dealWithPendingChanges(messageKey, () => {
      if (mode === 'view') {
        this._deleteDraft().finally(() => this._closePanelAndOpenContent(mode));
      } else {
        this._closePanelAndOpenContent(mode);
      }
    });
  }

  _dealWithPendingChanges(messageKey, done) {
    this._confirmSaveOrDiscardChanges(messageKey)
      .then((action) => {
        if (action === 'SAVE') {
          // don't return the result of saveDocument so a failing save does not invoke the 'done' function
          this.saveDocument().then(done);
        } else {
          done(); // discard
        }
      });
  }

  _confirmSaveOrDiscardChanges(messageKey) {
    if (!this._isFormDirty()) {
      return this.$q.resolve('DISCARD'); // No pending changes, no dialog, continue normally.
    }

    const message = this.$translate.instant(messageKey, { documentName: this.doc.displayName });
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

  _closePanelAndOpenContent(mode) {
    // The CMS automatically unlocks content that is being viewed, so close the side-panel to reflect that.
    // It will will unlock the document if needed, so don't delete the draft here.
    this._closePanel();

    // mode can be 'view' or 'edit', so the event names can be 'view-content' and 'edit-content'
    this.CmsService.publish('open-content', this.documentId, mode);

    if (mode === 'view') {
      this.CmsService.reportUsageStatistic('CMSChannelsContentPublish');
    } else if (mode === 'edit') {
      this.CmsService.reportUsageStatistic('CMSChannelsContentEditor');
    }
  }

  _saveDraft() {
    if (!this._isFormDirty()) {
      return this.$q.resolve();
    }
    return this.ContentService.saveDraft(this.doc);
  }

  closeButtonLabel() {
    return this._isFormDirty() ? this.cancelLabel : this.closeLabel;
  }

  _isFormDirty() {
    return this.form && this.form.$dirty;
  }

  _onOpen() {
    this.$element.addClass('sidepanel-open');
    this.$element.css('width', this.lastSavedWidth);
    this.$element.css('max-width', this.lastSavedWidth);
  }

  close() {
    return this._releaseDocument()
      .then(() => this._closePanel());
  }

  _releaseDocument() {
    if (this.deleteDraftOnClose) {
      return this._confirmDiscardChanges()
        .then(() => {
          // speed up closing the panel by not returning the promise so the draft is deleted asynchronously
          this._deleteDraft();
        });
    }
    return this.$q.resolve();
  }

  onResize(newWidth) {
    this.lastSavedWidth = `${newWidth}px`;
    this.localStorageService.set('rightSidePanelWidth', this.lastSavedWidth);
  }

  _confirmDiscardChanges() {
    if (!this._isFormDirty()) {
      return this.$q.resolve();
    }
    const messageParams = {
      documentName: this.doc.displayName,
    };
    const confirm = this.DialogService.confirm()
      .textContent(this.$translate.instant('CONFIRM_DISCARD_UNSAVED_CHANGES_MESSAGE', messageParams))
      .ok(this.$translate.instant('DISCARD'))
      .cancel(this.cancelLabel);

    return this.DialogService.show(confirm);
  }

  _deleteDraft() {
    if (this.editing) {
      return this.ContentService.deleteDraft(this.documentId);
    }
    return this.$q.resolve();
  }

  _closePanel() {
    this.SidePanelService.close('right')
      .then(() => this._resetState())
      .finally(() => {
        this.$element.removeClass('sidepanel-open');
        this.$element.css('max-width', '0px');
        this.setFullWidth(false);
      });

    if (!this.ChannelService.isToolbarDisplayed) {
      this.ChannelService.setToolbarDisplayed(true);
    }
  }

  setFullWidth(state) {
    if (state === true) {
      this.$element.addClass('fullwidth');
      this.ChannelService.setToolbarDisplayed(false);
      this.CmsService.reportUsageStatistic('CMSChannelsFullScreen');
    } else {
      this.$element.removeClass('fullwidth');
      this.ChannelService.setToolbarDisplayed(true);
    }
    this.isFullWidth = state;

    // this.FieldService.triggerInputFocus();
  }
}

export default RightSidePanelCtrl;
