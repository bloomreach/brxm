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

class EditContentService {
  constructor($state, $transitions, $translate, CmsService, ContentEditor, RightSidePanelService) {
    'ngInject';

    this.$state = $state;
    this.$translate = $translate;
    this.ContentEditor = ContentEditor;
    this.RightSidePanelService = RightSidePanelService;

    $transitions.onEnter(
      { entering: '**.edit-content' },
      transition => this._loadDocument(transition.params().documentId),
    );
    $transitions.onBefore(
      { from: '**.edit-content', to: 'hippo-cm' },
      () => this._onCloseChannel(),
    );

    CmsService.subscribe('kill-editor', (documentId) => {
      if (this.ContentEditor.getDocumentId() === documentId) {
        this.ContentEditor.kill();
        this.stopEditing();
      }
    });
  }

  startEditing(documentId) {
    this.$state.go('hippo-cm.channel.edit-content', { documentId });
  }

  stopEditing() {
    this.$state.go('^');
  }

  _loadDocument(documentId) {
    this._showDefaultTitle();
    this.RightSidePanelService.startLoading();
    this.ContentEditor.open(documentId)
      .then(() => {
        this._showDocumentTitle();
        this.RightSidePanelService.stopLoading();
      });
  }

  _showDefaultTitle() {
    const defaultTitle = this.$translate.instant('EDIT_CONTENT');
    this.RightSidePanelService.setTitle(defaultTitle);
  }

  _showDocumentTitle() {
    // when there's no document, the error's messageParams contain a 'displayName' property
    const document = this.ContentEditor.getDocument() || this.ContentEditor.getError().messageParams;
    const documentTitle = this.$translate.instant('EDIT_DOCUMENT', document);
    this.RightSidePanelService.setTitle(documentTitle);
  }

  _onCloseChannel() {
    return this.ContentEditor.confirmSaveOrDiscardChanges('SAVE_CHANGES_ON_CLOSE_CHANNEL')
      .then(() => this.ContentEditor.close());
  }
}

export default EditContentService;
