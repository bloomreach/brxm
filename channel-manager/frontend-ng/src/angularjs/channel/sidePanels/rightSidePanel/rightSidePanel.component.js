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

import template from './rightSidePanel.html';

const ERROR_MAP = {
  UNAVAILABLE: { // default catch-all
    title: 'UNAVAILABLE_CONTENT_TITLE',
    linkToFullEditor: true,
    messageKey: 'UNAVAILABLE',
  },
  NOT_A_DOCUMENT: {
    title: 'UNAVAILABLE_CONTENT_HERE_TITLE',
    linkToFullEditor: true,
    messageKey: 'UNAVAILABLE_CONTENT',
  },
  NOT_FOUND: {
    title: 'UNAVAILABLE_NOT_FOUND_TITLE',
    messageKey: 'UNAVAILABLE_NOT_FOUND',
    hideContentButtons: true,
  },
  UNAVAILABLE_CUSTOM_VALIDATION_PRESENT: {
    title: 'UNAVAILABLE_DOCUMENT_HERE_TITLE',
    linkToFullEditor: true,
    messageKey: 'UNAVAILABLE_CUSTOM_VALIDATION_PRESENT',
  },
  UNAVAILABLE_HELD_BY_OTHER_USER: {
    title: 'UNAVAILABLE_DOCUMENT_TITLE',
    messageKey: 'UNAVAILABLE_HELD_BY_OTHER_USER',
    hasUser: true,
  },
  UNAVAILABLE_REQUEST_PENDING: {
    title: 'UNAVAILABLE_DOCUMENT_TITLE',
    messageKey: 'UNAVAILABLE_REQUEST_PENDING',
  },
};

export class ChannelRightSidePanelCtrl {
  constructor($scope, $element, $timeout, $translate, $q, ChannelSidePanelService, CmsService, ContentService, HippoIframeService, FeedbackService) {
    'ngInject';

    this.$scope = $scope;
    this.$element = $element;
    this.$timeout = $timeout;
    this.$translate = $translate;
    this.$q = $q;

    this.ChannelSidePanelService = ChannelSidePanelService;
    this.CmsService = CmsService;
    this.ContentService = ContentService;
    this.HippoIframeService = HippoIframeService;
    this.FeedbackService = FeedbackService;

    this.defaultTitle = $translate.instant('EDIT_CONTENT');

    ChannelSidePanelService.initialize('right', $element.find('.channel-right-side-panel'), (documentId) => {
      this.openDocument(documentId);
    });
  }

  openDocument(documentId) {
    this._resetState();
    this._loadDocument(documentId);
  }

  _resetState() {
    delete this.doc;
    delete this.editing;
    delete this.feedback;
    delete this.hideContentButtons;

    this.title = this.defaultTitle;

    this._resetForm();
  }

  _resetForm() {
    if (this.form) {
      this.form.$setPristine();
    }
  }

  _loadDocument(id) {
    this.documentId = id;
    this.ContentService.createDraft(id)
      .then(doc => this.ContentService.getDocumentType(doc.info.type.id)
          .then((docType) => {
            this.editing = true;
            this.docType = docType;
            this._onLoadResponse({ data: doc });
          })
      )
      .catch(response => this._onLoadResponse(response));
  }

  _onLoadResponse(response) {
    let errorKey;
    if (this._isDocument(response.data)) {
      this.doc = response.data;
      if (this.doc.displayName) {
        this.title = this.$translate.instant('EDIT_DOCUMENT', this.doc);
      }
      this._resizeTextareas();

      errorKey = this.doc.info.editing.state;
    } else if (this._isErrorInfo(response.data)) {
      errorKey = response.data.reason;
    } else if (response.status === 404) {
      errorKey = 'NOT_FOUND';
    } else {
      errorKey = 'UNAVAILABLE';
    }

    this._handleResponse(errorKey);
  }

  _handleResponse(errorKey) {
    const error = ERROR_MAP[errorKey];
    const error = ERROR_MAP[errorKey];

    if (error) {
      const params = {};
      if (error.hasUser) {
        params.user = this._getHolder();
      }

      this.feedback = {
        title: error.title,
        message: this.$translate.instant(error.messageKey, params),
        linkToFullEditor: error.linkToFullEditor,
      };
      this.hideContentButtons = error.hideContentButtons;
    }
  }

  _getHolder() {
    return this.doc.info.editing.holder.displayName
        || this.doc.info.editing.holder.id;
  }

  _resizeTextareas() {
    // Set initial size of textareas (see Angular Material issue #9745).
    // Use $timeout to ensure that the sidenav has become visible.
    this.$timeout(() => {
      this.$scope.$broadcast('md-resize-textarea');
    });
  }

  isLockedOpen() {
    return this.ChannelSidePanelService.isOpen('right');
  }

  saveDocument() {
    return this._saveDraft()
      .then((savedDoc) => {
        this.doc = savedDoc;
        this._resetForm();
        this.HippoIframeService.reload();
      })
      .catch((response) => {
        if (this._isDocument(response.data)) {
          // CHANNELMGR-898: handle validation error on a per-field basis
          return;
        }

        let defaultKey = 'ERROR_UNABLE_TO_SAVE';
        if (this._isErrorInfo(response.data)) {
          defaultKey = `ERROR_${response.data.reason}`;
        }
        this.FeedbackService.showErrorResponse(undefined, defaultKey, undefined, this.$element);
      });
  }

  _isDocument(object) {
    return object && object.id; // Document has an ID field, ErrorInfo doesn't.
  }

  _isErrorInfo(object) {
    return object && object.reason; // ErrorInfo has a reason field, Document doesn't.
  }

  viewFullContent() {
    this._saveDraft()
      // The CMS automatically unlocks content that is being viewed, so close the side-panel to reflect that.
      // It will will unlock the document if needed, so don't delete the draft here.
      .then(() => {
        this._closePanel();
        this.CmsService.publish('view-content', this.documentId);
      });
  }

  _saveDraft() {
    if (!this.form || this.form.$pristine) {
      return this.$q.resolve();
    }
    return this.ContentService.saveDraft(this.doc);
  }

  editFullContent() {
    this.CmsService.publish('edit-content', this.documentId);
  }

  close() {
    this._deleteDraft();
    this._closePanel();
  }

  _deleteDraft() {
    if (this.editing) {
      this.ContentService.deleteDraft(this.documentId);
    }
  }

  _closePanel() {
    this.ChannelSidePanelService.close('right')
      .then(() => {
        // clear document to save on binding overhead (a closed sidenav is not removed from the DOM)
        this._clearDocument();
      });
  }

  _clearDocument() {
    delete this.doc;
    delete this.docType;
  }
}

const channelRightSidePanelComponentModule = angular
  .module('hippo-cm.channel.rightSidePanelComponentModule', [])
  .component('channelRightSidePanel', {
    bindings: {
      editMode: '=',
    },
    controller: ChannelRightSidePanelCtrl,
    template,
  });

export default channelRightSidePanelComponentModule;
