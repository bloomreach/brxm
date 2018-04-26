/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

class Step2Controller {
  constructor(
    $translate,
    ContentEditor,
    ContentService,
    CreateContentService,
    FeedbackService,
    RightSidePanelService,
    Step2Service,
    CmsService,
  ) {
    'ngInject';

    this.$translate = $translate;
    this.ContentEditor = ContentEditor;
    this.ContentService = ContentService;
    this.CreateContentService = CreateContentService;
    this.FeedbackService = FeedbackService;
    this.RightSidePanelService = RightSidePanelService;
    this.Step2Service = Step2Service;
    this.CmsService = CmsService;
  }

  $onInit() {
    this.documentIsSaved = false;
    this.switchingEditor = false;
  }

  allMandatoryFieldsShown() {
    return this.ContentEditor.isEditing() && this.ContentEditor.getDocumentType().canCreateAllRequiredFields;
  }

  switchEditor() {
    this.switchingEditor = true;
    this.CmsService.publish('open-content', this.ContentEditor.getDocumentId(), 'edit');
    this.ContentEditor.close();
    this.close();
  }

  save() {
    this.RightSidePanelService.startLoading();
    this.ContentEditor.saveStopEditing()
      .then(() => {
        this.documentIsSaved = true;
        this.FeedbackService.showNotification('NOTIFICATION_DOCUMENT_SAVED');
      })
      .then(() => this.Step2Service.saveComponentParameter())
      .then(() => {
        this.CreateContentService.finish(this.ContentEditor.getDocumentId());
      })
      .finally(() => {
        this.RightSidePanelService.stopLoading();
        this.CmsService.reportUsageStatistic('CreateContent2Done');
      });
  }

  close() {
    this.CreateContentService.stop();
  }

  getDocument() {
    return this.ContentEditor.getDocument();
  }

  uiCanExit() {
    if (this.documentIsSaved || this.switchingEditor) {
      return true;
    }
    return this.ContentEditor.confirmDiscardChanges('CONFIRM_DISCARD_NEW_DOCUMENT', 'DISCARD_DOCUMENT')
      .then(() => {
        this.ContentEditor.deleteDocument();
        this.CmsService.reportUsageStatistic('CreateContent2Cancel');
      });
  }

  openEditNameUrlDialog() {
    this.Step2Service.openEditNameUrlDialog();
  }
}

export default Step2Controller;
