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

  openContentEditor(mode) {
    this.mode = mode;

    this.EditContentService.stopEditing();

    // if (!isPromptUnsavedChanges) {
    //   this._closePanelAndOpenContent(mode);
    //   return;
    // }
    // const messageKey = mode === 'view'
    //   ? 'SAVE_CHANGES_ON_PUBLISH_MESSAGE'
    //   : 'SAVE_CHANGES_ON_SWITCH_TO_CONTENT_EDITOR_MESSAGE';
    //
    // this._dealWithPendingChanges(messageKey, () => {
    //   if (mode === 'view') {
    //     this._deleteDraft().finally(() => this._closePanelAndOpenContent(mode));
    //   } else {
    //     this._closePanelAndOpenContent(mode);
    //   }
    // });
  }

  uiCanExit() {
    if (this.mode === 'view') {
      return this.ContentEditor.confirmPendingChanges('SAVE_CHANGES_ON_PUBLISH_MESSAGE')
        .then(() => this._openContent());
    } else if (this.mode === 'edit') {
      this._openContent();
    }
    return this.$q.resolve();
  }

  _openContent() {
    const documentId = this.ContentEditor.getDocument().id;

    this.CmsService.publish('open-content', documentId, this.mode);

    // The CMS automatically unlocks content that is being viewed, so close the visual editor to reflect that.
    // It will will unlock the document if needed, so don't delete the draft here.
    this.ContentEditor.closeAndKeepDraft();

    if (this.mode === 'view') {
      this.CmsService.reportUsageStatistic('CMSChannelsContentPublish');
    } else if (this.mode === 'edit') {
      this.CmsService.reportUsageStatistic('CMSChannelsContentEditor');
    }
  }
}

export default EditContentToolsCtrl;
