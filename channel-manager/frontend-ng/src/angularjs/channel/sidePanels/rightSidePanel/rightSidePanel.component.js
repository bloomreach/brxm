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
  constructor($scope, $element, $timeout, $q, ChannelSidePanelService, CmsService, ContentService, HippoIframeService) {
    'ngInject';

    this.$scope = $scope;
    this.$timeout = $timeout;
    this.$q = $q;
    this.ChannelSidePanelService = ChannelSidePanelService;
    this.CmsService = CmsService;
    this.ContentService = ContentService;
    this.HippoIframeService = HippoIframeService;

    ChannelSidePanelService.initialize('right', $element.find('.channel-right-side-panel'), (documentId) => {
      this.openDocument(documentId);
    });
    this._clearDocument();
  }

  openDocument(documentId) {
    this._clearDocument();
    this._loadDocument(documentId);
  }

  _clearDocument() {
    this.doc = null;
    this.docType = null;
    this.focusedFieldId = null;
    this._resetForm();
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
            this.docType = docType;
            this._resizeTextareas();
          });
      });
    // TODO: handle error
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
      .then(() => this.HippoIframeService.reload());
  }

  viewFullContent() {
    this._saveDraft()
      // The CMS automatically unlocks content that is being viewed, so close the side-panel to reflect that.
      // It will will unlock the document if needed, so don't delete the draft here.
      .then(() => this._closePanel())
      .then(() => this.CmsService.publish('view-content', this.doc.id));
  }

  _saveDraft() {
    if (this.form.$pristine) {
      return this.$q.resolve();
    }
    return this.ContentService.saveDraft(this.doc)
      .then((savedDoc) => {
        this.doc = savedDoc;
        this._resetForm();
      });
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
    this.ChannelSidePanelService.close('right');
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
