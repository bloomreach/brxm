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

const REPORT_USAGE_STATISTIC_EVENT_NAMES = {
  new: 'VisualEditingOfflineIcon',
  live: 'VisualEditingOnlineIcon',
  changed: 'VisualEditingAlertIcon',
  unknown: 'VisualEditingUnknownIcon',
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
    if (error) {
      return error.disableContentButtons;
    }

    return !this.ContentEditor.isEditing();
  }

  _getPublicationStateValue(map, publicationState) {
    return map[publicationState] || map.unknown;
  }

  openContentEditor() {
    this.publicationStateOnExit = this.ContentEditor.getPublicationState();
    this.EditContentService.stopEditing();
    this.exitToContentEditor = true;
  }

  uiCanExit() {
    if (this.exitToContentEditor) {
      return this._saveContentEditor()
        .then(() => this._viewContent())
        .finally(this._clear());
    }
    // yes, the UI can exit. Return something to make ESLint happy.
    this._clear();
    return true;
  }

  _saveContentEditor() {
    return this.ContentEditor.isRetainable()
      ? this.ContentEditor.keepDraft()
      : this.ContentEditor.confirmSaveOrDiscardChanges('SAVE_CHANGES_TO_DOCUMENT')
        .then(() => this.ContentEditor.discardChanges());
  }

  _viewContent() {
    this.CmsService.publish('open-content', this.ContentEditor.getDocumentId(), 'view');
    this.ContentEditor.close();
    const statisticEventName = this._getPublicationStateValue(
      REPORT_USAGE_STATISTIC_EVENT_NAMES,
      this.publicationStateOnExit,
    );
    this.CmsService.reportUsageStatistic(statisticEventName);
  }

  _clear() {
    delete this.exitToContentEditor;
  }
}

export default EditContentToolsCtrl;
