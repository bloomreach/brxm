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
    HstComponentService,
  ) {
    'ngInject';

    this.$q = $q;
    this.$translate = $translate;
    this.ContentService = ContentService;
    this.ContentEditor = ContentEditor;
    this.DialogService = DialogService;
    this.FeedbackService = FeedbackService;
    this.HstComponentService = HstComponentService;

    this._reset();
  }

  _reset() {
    delete this.documentLocale;
    delete this.documentUrl;
  }

  open(document, url, locale, componentInfo) {
    this._reset();

    this.componentInfo = componentInfo;

    return this.ContentEditor.loadDocumentType(document)
      .then((docType) => {
        this.documentLocale = locale;
        this.documentUrl = url;
        // Mark the document dirty; this will trigger the discard dialog and enable the save button
        this.ContentEditor.markDocumentDirty();

        return docType;
      });
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

  saveComponentParameter() {
    const parameterName = this.componentInfo.parameterName;

    if (!parameterName) {
      // no component parameter to update
      return this.$q.resolve();
    }

    const componentId = this.componentInfo.id;
    const componentName = this.componentInfo.label;
    const componentVariant = this.componentInfo.variant;
    const parameterBasePath = this.componentInfo.parameterBasePath;
    const document = this.ContentEditor.getDocument();

    return this.HstComponentService.setPathParameter(componentId, componentVariant, parameterName, document.repositoryPath, parameterBasePath)
      .then(() => {
        this.FeedbackService.showNotification('NOTIFICATION_DOCUMENT_SELECTED_FOR_COMPONENT', { componentName });
      })
      .catch(() => {
        this.FeedbackService.showError('ERROR_DOCUMENT_SELECTED_FOR_COMPONENT', { componentName });
      });
  }

  killEditor(documentId) {
    if (this.ContentEditor.getDocumentId() === documentId) {
      this.ContentEditor.kill();
      return true;
    }
    return false;
  }
}

export default Step2Service;
