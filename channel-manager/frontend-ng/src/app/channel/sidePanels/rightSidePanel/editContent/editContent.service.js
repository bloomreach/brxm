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

class EditContentService {
  constructor(
    $q,
    $rootScope,
    $state,
    $transitions,
    $translate,
    CmsService,
    ConfigService,
    ContentEditor,
    ContentService,
    HippoIframeService,
    ProjectService,
    RightSidePanelService,
  ) {
    'ngInject';

    this.$q = $q;
    this.$state = $state;
    this.$translate = $translate;
    this.ConfigService = ConfigService;
    this.ContentEditor = ContentEditor;
    this.ContentService = ContentService;
    this.HippoIframeService = HippoIframeService;
    this.ProjectService = ProjectService;
    this.RightSidePanelService = RightSidePanelService;

    $transitions.onEnter(
      { entering: '**.edit-content' },
      transition => this._loadDocument(transition.params().documentId),
    );
    $transitions.onBefore(
      { from: '**.edit-content', to: 'hippo-cm' },
      () => this._onCloseChannel(),
    );

    $transitions.onEnter(
      { entering: '**.edit-page.*' },
      transition => this._loadDocument(transition.params().documentId),
    );

    $transitions.onBefore(
      { from: '**.edit-page.*', to: 'hippo-cm' },
      () => this._onCloseChannel(),
    );

    $transitions.onExit(
      { exiting: '**.edit-page.*' },
      () => $rootScope.$emit('page:check-changes'),
    );

    CmsService.subscribe('kill-editor', (documentId) => {
      this._stopEditingDocument(documentId);
    });

    ProjectService.beforeChange('editContent', (projectIdIdentical) => {
      if (!projectIdIdentical) {
        return this._beforeSwitchProject();
      }
      return this.$q.resolve();
    });
  }

  isEditing(documentId) {
    return this.ContentEditor.getDocumentId() === documentId;
  }

  isEditorPristine() {
    return this.ContentEditor.isPristine();
  }

  reloadEditor() {
    return this.$state.go('hippo-cm.channel.edit-page.content', {
      documentId: this.ContentEditor.getDocumentId(),
      lastModified: Date.now(),
    });
  }

  _isEditingDocument() {
    return ['hippo-cm.channel.edit-content',
      'hippo-cm.channel.edit-page.content',
      'hippo-cm.channel.edit-page.versions'].includes(this.$state.$current.name);
  }

  _stopEditingDocument(documentId) {
    if (this._isEditingDocument() && this.ContentEditor.getDocumentId() === documentId) {
      this.ContentEditor.kill();
      this.stopEditing();
    }
  }

  _beforeSwitchProject() {
    if (this._isEditingDocument()) {
      const titleKey = this.ContentEditor.isDocumentXPage ? 'SAVE_XPAGE_CHANGES_TITLE' : 'SAVE_DOCUMENT_CHANGES_TITLE';
      const messageKey = this.ContentEditor.isDocumentXPage ? 'SAVE_CHANGES_TO_XPAGE' : 'SAVE_CHANGES_TO_DOCUMENT';

      return this.ContentEditor.confirmClose(messageKey, {}, titleKey)
        .then(() => this.stopEditing());
    }
    return this.$q.resolve();
  }

  startEditing(documentId, state) {
    const editingState = state || 'hippo-cm.channel.edit-content';
    const transition = () => this.$state.go(editingState, { documentId });
    if (!this.ConfigService.projectsEnabled) {
      transition.apply();
    } else {
      const selectedProjectId = this.ProjectService.selectedProject.id;
      this.ContentService.getDocument(documentId, selectedProjectId).then(
        (document) => {
          if (selectedProjectId && document.branchId !== selectedProjectId) {
            this._showDocumentTitle(document.displayName);
            this._setDocumentContext();
            this.$state.go('hippo-cm.channel.add-to-project', { documentId, nextState: editingState });
          } else {
            transition.apply();
          }
        },
      );
    }
  }

  branchAndEditDocument(documentId, state = 'hippo-cm.channel.edit-content') {
    this.ContentService.branchDocument(documentId)
      .then(() => this.$state.go(state, { documentId }))
      .then(() => this.HippoIframeService.reload());
  }

  stopEditing() {
    this.$state.go('hippo-cm.channel');
  }

  async _loadDocument(documentId) {
    this.RightSidePanelService.clearContext();

    this._showDefaultTitle();
    this.RightSidePanelService.startLoading();

    const document = await this.ContentEditor.open(documentId);
    if (document) {
      this.documentId = documentId;
    }
    this._showDocumentTitle(this.ContentEditor.getDocumentDisplayName());
    this._setDocumentContext();
    this.RightSidePanelService.stopLoading();
  }

  _showDefaultTitle() {
    const documentLabel = this.$translate.instant('DOCUMENT');
    this.RightSidePanelService.setTitle(documentLabel);
  }

  _setDocumentContext() {
    const messageKey = this.ContentEditor.isDocumentXPage ? 'PAGE' : 'DOCUMENT';
    const documentLabel = this.$translate.instant(messageKey);

    this.RightSidePanelService.setContext(documentLabel);
  }

  _showDocumentTitle(documentTitle) {
    this.RightSidePanelService.setTitle(documentTitle);
  }

  _onCloseChannel() {
    const titleKey = this.ContentEditor.isDocumentXPage ? 'SAVE_XPAGE_CHANGES_TITLE' : 'SAVE_DOCUMENT_CHANGES_TITLE';
    const messageKey = this.ContentEditor.isDocumentXPage ? 'SAVE_CHANGES_TO_XPAGE' : 'SAVE_CHANGES_TO_DOCUMENT';

    return this.ContentEditor.confirmClose(messageKey, {}, titleKey);
  }
}

export default EditContentService;
