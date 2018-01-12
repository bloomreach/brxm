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
  constructor($translate, CmsService, ContentEditor, EditContentService, HippoIframeService) {
    'ngInject';

    this.CmsService = CmsService;
    this.ContentEditor = ContentEditor;
    this.EditContentService = EditContentService;
    this.HippoIframeService = HippoIframeService;

    this.cancelLabel = $translate.instant('CANCEL');
    this.closeLabel = $translate.instant('CLOSE');
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

  closeButtonLabel() {
    return this.isDocumentDirty() ? this.cancelLabel : this.closeLabel;
  }

  close() {
    this.EditContentService.stopEditing();
  }
}

export default EditContentMainCtrl;
