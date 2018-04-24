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

const PUBLICATION_STATE_ICON_NAMES = {
  new: 'mdi-minus-circle',
  live: 'mdi-check-circle',
  changed: 'mdi-alert',
  unknown: '',
};

const PUBLICATION_STATE_ICON_TOOLTIPS = {
  new: 'DOCUMENT_NEW_TOOLTIP',
  live: 'DOCUMENT_LIVE_TOOLTIP',
  changed: 'DOCUMENT_CHANGED_TOOLTIP',
  unknown: '',
};

class EditContentToolsCtrl {
  constructor($q, CmsService, ContentEditor, EditContentService) {
    'ngInject';

    this.$q = $q;
    this.CmsService = CmsService;
    this.ContentEditor = ContentEditor;
    this.EditContentService = EditContentService;
  }

  isDisabled() {
    const error = this.ContentEditor.getError();
    return error && error.disableContentButtons;
  }

  isEditing() {
    return this.ContentEditor.isEditing();
  }

  isPublicationStateAvailable() {
    return !!this.ContentEditor.getPublicationState();
  }

  getPublicationIconName() {
    return this._getPublicationStateValue(PUBLICATION_STATE_ICON_NAMES);
  }

  getPublicationIconTooltip() {
    return this._getPublicationStateValue(PUBLICATION_STATE_ICON_TOOLTIPS);
  }

  _getPublicationStateValue(map) {
    const publicationState = this.ContentEditor.getPublicationState();
    return map[publicationState] || map.unknown;
  }

  openContentEditor(exitMode) {
    this.exitMode = exitMode;
    this.EditContentService.stopEditing();
  }

  uiCanExit() {
    if (this.exitMode === 'view') {
      return this.ContentEditor.confirmSaveOrDiscardChanges('SAVE_CHANGES_ON_PUBLISH_MESSAGE')
        .then(() => this.ContentEditor.deleteDraft())
        .then(() => this._viewContent())
        .finally(() => this._clearExitMode());
    } else if (this.exitMode === 'edit') {
      this._editContent();
    }
    // yes, the UI can exit. Return something to make ESLint happy.
    this._clearExitMode();
    return true;
  }

  _viewContent() {
    this.CmsService.publish('open-content', this.ContentEditor.getDocumentId(), 'view');
    this.ContentEditor.close();
    this.CmsService.reportUsageStatistic('CMSChannelsContentPublish');
  }

  _editContent() {
    this.CmsService.publish('open-content', this.ContentEditor.getDocumentId(), 'edit');
    this.ContentEditor.close();
    this.CmsService.reportUsageStatistic('CMSChannelsContentEditor');
  }

  _clearExitMode() {
    delete this.exitMode;
  }
}

export default EditContentToolsCtrl;
