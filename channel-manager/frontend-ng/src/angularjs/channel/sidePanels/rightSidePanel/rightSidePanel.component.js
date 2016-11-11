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
  constructor($scope, $element, $timeout, ChannelSidePanelService, ContentService, HippoIframeService) {
    'ngInject';

    this.$scope = $scope;
    this.$timeout = $timeout;
    this.ChannelSidePanelService = ChannelSidePanelService;
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
      })
      .catch((error) => {
        if (error) {
          this.doc = error.data;
        } else {
          this.doc = {
            info: {
              editing: {
                state: "UNAVAILABLE"
              }
            }
          }
        }
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
    this.ContentService.saveDraft(this.doc)
      .then((savedDoc) => {
        this.doc = savedDoc;
        this._resetForm();
        this.HippoIframeService.reload();
      });
  }

  close() {
    if (this.doc) {
      this.ContentService.deleteDraft(this.doc.id);
    }
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
