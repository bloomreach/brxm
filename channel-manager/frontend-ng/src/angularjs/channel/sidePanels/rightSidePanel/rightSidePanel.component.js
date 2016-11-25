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
  UNAVAILABLE: {
    title: 'UNAVAILABLE_CONTENT_TITLE',
    linkToFullEditor: true,
    messageKey: 'UNAVAILABLE',
  },
  UNAVAILABLE_CONTENT: {
    title: 'UNAVAILABLE_CONTENT_HERE_TITLE',
    linkToFullEditor: true,
    messageKey: 'UNAVAILABLE_CONTENT',
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
  constructor($scope, $element, $timeout, $translate, $q, ChannelSidePanelService, CmsService, ContentService,
              DialogService, HippoIframeService, FeedbackService) {
    'ngInject';

    this.$scope = $scope;
    this.$element = $element;
    this.$timeout = $timeout;
    this.$translate = $translate;
    this.$q = $q;

    this.ChannelSidePanelService = ChannelSidePanelService;
    this.CmsService = CmsService;
    this.ContentService = ContentService;
    this.DialogService = DialogService;
    this.HippoIframeService = HippoIframeService;
    this.FeedbackService = FeedbackService;

    this.defaultTitle = $translate.instant('EDIT_CONTENT');
    this.closeLabel = $translate.instant('CLOSE');
    this.cancelLabel = $translate.instant('CANCEL');

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
    delete this.loaded;
    delete this.editing;
    delete this.feedback;

    this.title = this.defaultTitle;
    this.state = 'UNAVAILABLE_CONTENT';

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
            this.docType = docType;
            this._onLoaded(doc);
            this.editing = true;
          })
      )
      .catch(response => this._onLoaded(response.data));
  }

  _onLoaded(doc) {
    if (doc) {
      this.doc = doc;
      if (doc.displayName) {
        this.title = this.$translate.instant('EDIT_DOCUMENT', doc);
      }
      if (doc.info && doc.info.editing) {
        this.state = doc.info.editing.state;
      }
      this._resizeTextareas();
    }

    this._updateFeedback();
    this.loaded = true;
  }

  _updateFeedback() {
    const error = ERROR_MAP[this.state];
    const params = {};

    if (error) {
      if (error.hasUser) {
        params.user = this._getHolder();
      }

      this.feedback = {
        title: error.title,
        message: this.$translate.instant(error.messageKey, params),
        linkToFullEditor: error.linkToFullEditor,
      };
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
        if (response.data && response.data.reason) {
          defaultKey = `ERROR_${response.data.reason}`;
        }
        this.FeedbackService.showErrorResponse(undefined, defaultKey, undefined, this.$element);
      });
  }

  _isDocument(object) {
    return object && object.id; // Document has an ID field, ErrorInfo doesn't.
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
    if (!this._isFormDirty()) {
      return this.$q.resolve();
    }
    return this.ContentService.saveDraft(this.doc);
  }

  editFullContent() {
    this.CmsService.publish('edit-content', this.documentId);
  }

  closeButtonLabel() {
    return this._isFormDirty() ? this.cancelLabel : this.closeLabel;
  }

  _isFormDirty() {
    return this.form && this.form.$dirty;
  }

  close() {
    this._confirmIfFormDirty().then(() => {
      this._deleteDraft();
      this._closePanel();
    });
  }

  _confirmIfFormDirty() {
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
