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

class EditContentMainCtrl {
  constructor($q, $translate, CmsService, ContentEditor, EditContentService, HippoIframeService) {
    'ngInject';

    this.$q = $q;
    this.CmsService = CmsService;
    this.ContentEditor = ContentEditor;
    this.EditContentService = EditContentService;
    this.HippoIframeService = HippoIframeService;

    this.closing = false;
  }

  notAllFieldsShown() {
    return this.ContentEditor.isEditing() && !this.ContentEditor.getDocumentType().allFieldsIncluded;
  }

  save() {
    this.HippoIframeService.reload();
    this.CmsService.reportUsageStatistic('CMSChannelsSaveDocument');
  }

  close() {
    this.closing = true;
    this.EditContentService.stopEditing();
  }

  switchEditor() {
    this.CmsService.publish('open-content', this.ContentEditor.getDocumentId(), 'edit');
    this.ContentEditor.close();
    this.EditContentService.stopEditing();
  }

  uiCanExit() {
    return this._confirmExit()
      .then(() => {
        // don't return the result of deleteDraft: if it fails (e.g. because an admin unlocked the document)
        // the editor should still be closed.
        this.ContentEditor.deleteDraft()
          .finally(() => this.ContentEditor.close());
      })
      .catch(() => {
        // user cancelled the exit
        this.closing = false;
        return this.$q.reject();
      });
  }

  _confirmExit() {
    if (this.closing) {
      return this.ContentEditor.confirmDiscardChanges('CONFIRM_DISCARD_UNSAVED_CHANGES_MESSAGE');
    }
    return this.ContentEditor.confirmSaveOrDiscardChanges('SAVE_CHANGES_ON_BLUR_MESSAGE')
      .then((action) => {
        if (action === 'SAVE') {
          this.HippoIframeService.reload();
        }
      });
  }
}

export default EditContentMainCtrl;
