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

import nameUrlFieldsDialogController from '../step2/nameUrlFieldsDialog/nameUrlFieldsDialog.controller';
import nameUrlFieldsDialogTemplate from '../step2/nameUrlFieldsDialog/nameUrlFieldsDialog.html';

class Step2Controller {
  constructor(
    $q,
    $translate,
    CreateContentService,
    Step2Service,
    FieldService,
    ContentService,
    DialogService,
    FeedbackService,
  ) {
    'ngInject';

    this.$q = $q;
    this.$translate = $translate;
    this.ContentService = ContentService;
    this.CreateContentService = CreateContentService;
    this.DialogService = DialogService;
    this.FeedbackService = FeedbackService;
    this.FieldService = FieldService;
    this.Step2Service = Step2Service;
  }

  $onInit() {
    this.documentIsSaved = false;
  }

  getData() {
    return this.Step2Service.getData();
  }

  discardAndClose() {
    return this._confirmDiscardChanges().then(async () => {
      try {
        await this.Step2Service.deleteDraft(this.documentId);
      } catch (error) {
        const errorKey = this.$translate.instant(`ERROR_${error.data.reason}`);
        this.FeedbackService.showError(errorKey, error.data.params);
      }
      return this.$q.resolve();
    });
  }

  save() {
    this.ContentService.saveDraft(this.getData().document)
      .then(() => {
        this.documentIsSaved = true;
        this.CreateContentService.finish(this.getData().document.id);
      });
  }

  close() {
    this.CreateContentService.stop();
  }

  uiCanExit() {
    if (this.documentIsSaved) {
      return true;
    }
    return this.Step2Service.confirmDiscardChanges()
      .then(async () => {
        try {
          await this.Step2Service.deleteDraft(this.getData().document.id);
        } catch (error) {
          const errorKey = this.$translate.instant(`ERROR_${error.data.reason}`);
          this.FeedbackService.showError(errorKey, error.data.params);
        }
        return true;
      });
  }

  openEditNameUrlDialog() {
    const data = this.getData();
    const dialog = {
      template: nameUrlFieldsDialogTemplate,
      controller: nameUrlFieldsDialogController,
      locals: {
        title: this.$translate.instant('CHANGE_DOCUMENT_NAME'),
        nameField: data.document.displayName,
        urlField: data.documentUrl,
        locale: data.documentLocale,
      },
      controllerAs: '$ctrl',
      bindToController: true,
    };

    return this.DialogService.show(dialog).then((result) => {
      this._onEditNameUrlDialogClose(result);
    });
  }

  _onEditNameUrlDialogClose(dialogData) {
    const data = this.getData();
    return this.Step2Service.setDraftNameUrl(data.document.id, dialogData)
      .then((result) => {
        data.document.displayName = result.displayName;
        data.documentUrl = result.urlName;
      })
      .catch((error) => {
        const errorKey = this.$translate.instant(`ERROR_${error.data.reason}`);
        this.FeedbackService.showError(errorKey, error.data.params);
      });
  }
}

export default Step2Controller;
