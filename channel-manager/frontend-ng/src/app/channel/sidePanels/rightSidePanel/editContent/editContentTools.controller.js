/*
 * Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
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

  isEditing() {
    return this.ContentEditor.isEditing();
  }

  isPublicationStateAvailable() {
    return !!this.ContentEditor.getPublicationState();
  }

  getPublicationIconName() {
    return this._getPublicationStateValue(PUBLICATION_STATE_ICON_NAMES, this.ContentEditor.getPublicationState());
  }

  getPublicationIconTooltip() {
    return this._getPublicationStateValue(PUBLICATION_STATE_ICON_TOOLTIPS, this.ContentEditor.getPublicationState());
  }

  _getPublicationStateValue(map, publicationState) {
    return map[publicationState] || map.unknown;
  }

  openContentEditor(exitMode) {
    this.publicationStateOnExit = this.ContentEditor.getPublicationState();
    this.exitMode = exitMode;
    this.EditContentService.stopEditing();
  }

  uiCanExit() {
    if (this.exitMode === 'view') {
      return this.ContentEditor.confirmSaveOrDiscardChanges('SAVE_CHANGES_ON_PUBLISH_MESSAGE')
        .then(() => this.ContentEditor.discardChanges())
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
    const statisticEventName = this._getPublicationStateValue(REPORT_USAGE_STATISTIC_EVENT_NAMES, this.publicationStateOnExit);
    this.CmsService.reportUsageStatistic(statisticEventName);
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
