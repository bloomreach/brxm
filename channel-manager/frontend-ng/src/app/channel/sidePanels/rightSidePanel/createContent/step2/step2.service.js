/*
 * Copyright 2018-2020 Hippo B.V. (http://www.onehippo.com)
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
    CmsService,
    ContentEditor,
    ContentService,
    DialogService,
    FeedbackService,
    HstComponentService,
  ) {
    'ngInject';

    this.$q = $q;
    this.$translate = $translate;
    this.CmsService = CmsService;
    this.ContentEditor = ContentEditor;
    this.ContentService = ContentService;
    this.DialogService = DialogService;
    this.FeedbackService = FeedbackService;
    this.HstComponentService = HstComponentService;

    this._reset();
  }

  _reset() {
    delete this.documentLocale;
    delete this.documentUrl;
    delete this.xpage;
  }

  open(document, url, locale, componentInfo, xpage) {
    this._reset();

    this.componentInfo = componentInfo;

    return this.ContentEditor.loadDocumentType(document)
      .then((documentType) => {
        this.documentLocale = locale;
        this.documentUrl = url;
        this.xpage = xpage;

        this._reportUnsupportedRequiredFieldTypes(documentType);

        return documentType;
      });
  }

  _reportUnsupportedRequiredFieldTypes(documentType) {
    if (documentType.unsupportedRequiredFieldTypes) {
      const unsupportedMandatoryFieldTypes = documentType.unsupportedRequiredFieldTypes.join(',');
      const unsupportedFieldTypes = documentType.unsupportedFieldTypes
        ? documentType.unsupportedFieldTypes.join(',')
        : '';

      this.CmsService.reportUsageStatistic(
        'CreateContentUnsupportedFields',
        { unsupportedFieldTypes, unsupportedMandatoryFieldTypes },
      );
    }
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
      .then(dialogData => this.setDocumentNameUrl(document.id, dialogData)
        .then((result) => {
          document.displayName = result.displayName;
          this.documentUrl = result.urlName;
        })
        .catch((error) => {
          const errorKey = this.$translate.instant(`ERROR_${error.data.reason}`);
          this.FeedbackService.showError(errorKey, error.data.params);
        }));
  }

  setDocumentNameUrl(documentId, data) {
    const nameUrl = { displayName: data.name, urlName: data.url };
    return this.ContentService.setDocumentNameUrl(documentId, nameUrl);
  }

  saveComponentParameter() {
    const { parameterName } = this.componentInfo;

    if (!parameterName) {
      // no component parameter to update
      return this.$q.resolve();
    }

    const componentId = this.componentInfo.id;
    const componentName = this.componentInfo.label;
    const componentVariant = this.componentInfo.variant;
    const { parameterBasePath } = this.componentInfo;
    const document = this.ContentEditor.getDocument();

    return this.HstComponentService.setPathParameter(
      componentId, componentVariant, parameterName, document.repositoryPath, parameterBasePath,
    ).then(() => {
      this.FeedbackService.showNotification('NOTIFICATION_DOCUMENT_SELECTED_FOR_COMPONENT', { componentName });
    }).catch((response) => {
      const defaultErrorKey = 'ERROR_DOCUMENT_SELECTED_FOR_COMPONENT';
      const defaultErrorParams = { componentName };
      const errorMap = { ITEM_ALREADY_LOCKED: 'ERROR_DOCUMENT_SELECTED_FOR_COMPONENT_ALREADY_LOCKED' };

      this.FeedbackService.showErrorResponse(
        response && response.data, defaultErrorKey, errorMap, defaultErrorParams,
      );
    });
  }

  killEditor(documentId) {
    if (this.ContentEditor.getDocumentId() === documentId) {
      this.ContentEditor.kill();
      return true;
    }
    return false;
  }

  isXPage() {
    return !!this.xpage;
  }
}

export default Step2Service;
