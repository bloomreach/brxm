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
    return error && error.disableContentButtons;
  }

  _getPublicationStateValue(map, publicationState) {
    return map[publicationState] || map.unknown;
  }

  openContentEditor() {
    this.publicationStateOnExit = this.ContentEditor.getPublicationState();
    this.EditContentService.stopEditing();
  }

  uiCanExit() {
    return this.ContentEditor.confirmSaveOrDiscardChanges('SAVE_CHANGES_ON_PUBLISH_MESSAGE')
      .then(() => this.ContentEditor.discardChanges())
      .then(() => this._viewContent());
  }

  _viewContent() {
    this.CmsService.publish('open-content', this.ContentEditor.getDocumentId(), 'view');
    this.ContentEditor.close();
    const statisticEventName = this._getPublicationStateValue(REPORT_USAGE_STATISTIC_EVENT_NAMES, this.publicationStateOnExit);
    this.CmsService.reportUsageStatistic(statisticEventName);
  }
}

export default EditContentToolsCtrl;
