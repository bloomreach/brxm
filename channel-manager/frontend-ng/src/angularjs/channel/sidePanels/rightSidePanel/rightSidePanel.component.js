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

export class ChannelRightSidePanelCtrl {
  constructor($scope, $element, $timeout, $translate, $q, ChannelSidePanelService, CmsService, ContentService, HippoIframeService, FeedbackService) {
    'ngInject';

    this.$scope = $scope;
    this.$timeout = $timeout;
    this.$translate = $translate;
    this.$q = $q;

    this.ChannelSidePanelService = ChannelSidePanelService;
    this.CmsService = CmsService;
    this.ContentService = ContentService;
    this.HippoIframeService = HippoIframeService;
    this.FeedbackService = FeedbackService;

    ChannelSidePanelService.initialize('right', $element.find('.channel-right-side-panel'), (documentId) => {
      this.openDocument(documentId);
    });
  }

  openDocument(documentId) {
    this._resetForm();
    this._loadDocument(documentId);
  }

  _resetForm() {
    if (this.form) {
      this.form.$setPristine();
    }
  }

  _loadDocument(id) {
    this.ContentService.createDraft(id)
      .then((doc) => {
        this.ContentService.getDocumentType(doc.info.type.id)
          .then((docType) => {
            this.doc = doc;
            this.state = this.doc.info.editing.state;
            this.docType = docType;
            this._resizeTextareas();
          });
      })
      .catch((error) => {
        if (error) {
          this.doc = error.data;
          this.state = this.doc.info.editing.state;
        } else {
          this.state = 'UNAVAILABLE_CONTENT';
        }

        const errorMap = {
          UNAVAILABLE: {
            title: 'UNAVAILABLE_CONTENT_TITLE',
            linkToFullEditor: true,
            message: this.$translate.instant('UNAVAILABLE'),
          },
          UNAVAILABLE_CONTENT: {
            title: 'UNAVAILABLE_CONTENT_HERE_TITLE',
            linkToFullEditor: true,
            message: this.$translate.instant('UNAVAILABLE_CONTENT'),
          },
          UNAVAILABLE_CUSTOM_VALIDATION_PRESENT: {
            title: 'UNAVAILABLE_DOCUMENT_HERE_TITLE',
            linkToFullEditor: true,
            message: this.$translate.instant('UNAVAILABLE_CUSTOM_VALIDATION_PRESENT'),
          },
          UNAVAILABLE_HELD_BY_OTHER_USER: {
            title: 'UNAVAILABLE_DOCUMENT_TITLE',
            message: this.$translate.instant('UNAVAILABLE_HELD_BY_OTHER_USER', { user: this.doc.info.editing.holder.displayName ? this.doc.info.editing.holder.displayName : this.doc.info.editing.holder.id }),
          },
          UNAVAILABLE_REQUEST_PENDING: {
            title: 'UNAVAILABLE_DOCUMENT_TITLE',
            message: this.$translate.instant('UNAVAILABLE_REQUEST_PENDING'),
          },
        };
        this.unavailableTitle = errorMap[this.state].title;
        this.unavailableMessage = errorMap[this.state].message;
        this.linkToFullEditor = errorMap[this.state].linkToFullEditor;
      });
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
        if (response && response.data && response.data.reason) {
          this.FeedbackService.showErrorResponse(response, `ERROR_${response.data.reason}`);
        } else {
          this.FeedbackService.showErrorResponse(response, 'ERROR_UNABLE_TO_SAVE');
        }
      });
  }

  viewFullContent() {
    this._saveDraft()
      // The CMS automatically unlocks content that is being viewed, so close the side-panel to reflect that.
      // It will will unlock the document if needed, so don't delete the draft here.
      .then(() => {
        this._closePanel();
        this.CmsService.publish('view-content', this.doc.id);
      });
  }

  _saveDraft() {
    if (this.form.$pristine) {
      return this.$q.resolve();
    }
    return this.ContentService.saveDraft(this.doc);
  }

  editFullContent() {
    this.CmsService.publish('edit-content', this.doc.id);
  }

  close() {
    this._deleteDraft();
    this._closePanel();
  }

  _deleteDraft() {
    if (this.doc) {
      this.ContentService.deleteDraft(this.doc.id);
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
