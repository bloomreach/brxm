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

    this.cancelLabel = $translate.instant('CANCEL');
    this.closeLabel = $translate.instant('CLOSE');
    this.closing = false;
  }

  isEditing() {
    return this.ContentEditor.isEditing();
  }

  isDocumentDirty() {
    return this.ContentEditor.isDocumentDirty();
  }

  switchEditor() {
    this.CmsService.publish('open-content', this.ContentEditor.getDocumentId(), 'edit');
    this.ContentEditor.close();
    this.EditContentService.stopEditing();
  }

  isSaveAllowed() {
    return this.isEditing() && this.isDocumentDirty() && this.form.$valid;
  }

  save() {
    this.ContentEditor.save()
      .then(() => {
        this.form.$setPristine();
        this.HippoIframeService.reload();
        this.CmsService.reportUsageStatistic('CMSChannelsSaveDocument');
      });
  }

  uiCanExit() {
    return this._confirmExit()
      .then(() => {
        // don't return the result of deleteDraft: if it fails (e.g. because an admin 'stole' the document)
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
      return this.ContentEditor.confirmDiscardChanges();
    }
    return this.ContentEditor.confirmSaveOrDiscardChanges('SAVE_CHANGES_ON_BLUR_MESSAGE')
      .then((saved) => {
        if (saved) {
          this.HippoIframeService.reload();
        }
      });
  }

  closeButtonLabel() {
    return this.isDocumentDirty() ? this.cancelLabel : this.closeLabel;
  }

  close() {
    this.closing = true;
    this.EditContentService.stopEditing();
  }
}

export default EditContentMainCtrl;
