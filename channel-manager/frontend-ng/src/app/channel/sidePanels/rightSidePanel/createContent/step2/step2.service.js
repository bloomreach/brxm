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

import nameUrlFieldsDialogController from './nameUrlFieldsDialog/nameUrlFieldsDialog.controller';
import nameUrlFieldsDialogTemplate from './nameUrlFieldsDialog/nameUrlFieldsDialog.html';

class Step2Service {
  constructor(
    $q,
    $translate,
    ContentEditor,
    ContentService,
    DialogService,
    FeedbackService,
  ) {
    'ngInject';

    this.$q = $q;
    this.$translate = $translate;
    this.ContentService = ContentService;
    this.ContentEditor = ContentEditor;
    this.DialogService = DialogService;
    this.FeedbackService = FeedbackService;

    this._reset();
  }

  _reset() {
    this.data = {};
  }

  open(document, url, locale) {
    this._reset();

    return this.ContentEditor.loadDocumentType(document)
      .then((docType) => {
        this.documentUrl = url;
        this.documentLocale = locale;

        return docType;
      });
  }

  confirmDiscardChanges() {
    const document = this.ContentEditor.getDocument();
    if (!document) {
      return this.$q.resolve(true);
    }

    const messageParams = {
      documentName: document.displayName,
    };

    const confirm = this.DialogService.confirm()
      .title(this.$translate.instant('DISCARD_DOCUMENT', messageParams))
      .textContent(this.$translate.instant('CONFIRM_DISCARD_NEW_DOCUMENT', messageParams))
      .ok(this.$translate.instant('DISCARD'))
      .cancel(this.$translate.instant('CANCEL'));

    return this.DialogService.show(confirm);
  }

  openEditNameUrlDialog() {
    const document = this.ContentEditor.getDocument();
    const dialog = {
      template: nameUrlFieldsDialogTemplate,
      controller: nameUrlFieldsDialogController,
      locals: {
        title: this.$translate.instant('CHANGE_DOCUMENT_NAME'),
        nameField: document.displayName,
        urlField: this.documentUrl,
        locale: this.documentLocale,
      },
      controllerAs: '$ctrl',
      bindToController: true,
    };

    return this.DialogService.show(dialog)
      .then(dialogData => this.setDraftNameUrl(document.id, dialogData)
        .then((result) => {
          document.displayName = result.displayName;
          this.documentUrl = result.urlName;
        })
        .catch((error) => {
          const errorKey = this.$translate.instant(`ERROR_${error.data.reason}`);
          this.FeedbackService.showError(errorKey, error.data.params);
        }));
  }

  setDraftNameUrl(documentId, data) {
    const nameUrl = { displayName: data.name, urlName: data.url };
    return this.ContentService._send('PUT', ['documents', documentId], nameUrl);
  }
}

export default Step2Service;
