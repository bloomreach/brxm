/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

class Step2Controller {
  constructor(
    $translate,
    CreateContentService,
    FieldService,
    ContentService,
    DialogService,
    FeedbackService,
  ) {
    'ngInject';

    this.$translate = $translate;
    this.CreateContentService = CreateContentService;
    this.FieldService = FieldService;
    this.ContentService = ContentService;
    this.DialogService = DialogService;
    this.FeedbackService = FeedbackService;

    this.title = 'Create new content';
    this.defaultTitle = 'Create new title';
    this.isFullWidth = false;
    this.loading = false;

    this.doc = null;
    this.docType = null;
  }

  $onInit() {
    this.loadNewDocument();
    this._resetBeforeStateChange();
  }

  loadNewDocument() {
    const doc = this.CreateContentService.getDocument();
    this.documentId = doc.id;
    this.FieldService.setDocumentId(doc.id);
    this.loading = true;
    return this.ContentService.getDocumentType(doc.info.type.id)
      .then((docType) => {
        this._onLoadSuccess(doc, docType);
        this.loading = false;
      }).catch(() => {
        this.loading = false;
      });
  }

  discardAndClose() {
    return this._confirmDiscardChanges().then(async () => {
      try {
        await this.CreateContentService.deleteDraft(this.documentId);
      } catch (error) {
        const errorKey = this.$translate.instant(`ERROR_${error.data.reason}`);
        this.FeedbackService.showError(errorKey, error.data.params);
        return Promise.reject();
      }

      return Promise.resolve();
    });
  }

  saveDocument() {
    this.ContentService.saveDraft(this.doc)
      .then(() => {
        this.onBeforeStateChange({ callback: () => Promise.resolve() });
        this.onSave({ documentId: this.doc.id });
      });
  }

  isDocumentDirty() {
    return (this.doc && this.doc.info && this.doc.info.dirty);
  }

  close() {
    return this.discardAndClose()
      .then(() => {
        this._resetState();
        this.onClose();
      })
      .catch(() => angular.noop());
  }

  openEditNameUrlDialog() {
    const dialog = {
      templateUrl: './nameUrlFieldsDialog/nameUrlFieldsDialog.html',
      controller: nameUrlFieldsDialogController,
      locals: {
        title: this.$translate.instant('CHANGE_DOCUMENT_NAME'),
        nameField: this.doc.displayName,
        urlField: this.documentUrl,
        locale: this.documentLocale,
      },
      controllerAs: '$ctrl',
      bindToController: true,
    };

    return this.DialogService.show(dialog).then((result) => {
      this._onEditNameUrlDialogClose(result);
    });
  }

  _onEditNameUrlDialogClose(data) {
    return this.CreateContentService.setDraftNameUrl(this.doc.id, data)
      .then((result) => {
        this.doc.displayName = result.displayName;
        this.documentUrl = result.urlName;
      })
      .catch((error) => {
        const errorKey = this.$translate.instant(`ERROR_${error.data.reason}`);
        this.FeedbackService.showError(errorKey, error.data.params);
      });
  }

  _resetState() {
    delete this.doc;
    delete this.documentId;
    delete this.docType;
    delete this.feedback;
    this.title = this.defaultTitle;
    this.onBeforeStateChange({ callback: () => Promise.resolve() });
  }

  _onLoadSuccess(doc, docTypeInfo) {
    this.doc = doc;
    this.docType = docTypeInfo;
    this.title = this.$translate.instant('CREATE_NEW_DOCUMENT_TYPE', { documentType: docTypeInfo.displayName });
    this.doc.displayName = this.options.name;
    this.documentUrl = this.options.url;
    this.documentLocale = this.options.locale;
  }

  _resetBeforeStateChange() {
    this.onBeforeStateChange({ callback: () => this.discardAndClose() });
  }

  _confirmDiscardChanges() {
    const messageParams = {
      documentName: this.doc.displayName,
    };

    const confirm = this.DialogService.confirm()
      .title(this.$translate.instant('DISCARD_DOCUMENT', messageParams))
      .textContent(this.$translate.instant('CONFIRM_DISCARD_NEW_DOCUMENT', messageParams))
      .ok(this.$translate.instant('DISCARD'))
      .cancel(this.$translate.instant('CANCEL'));

    return this.DialogService.show(confirm);
  }

  setWidthState(state) {
    this.isFullWidth = state;
    this.onFullWidth({ state });
  }
}

export default Step2Controller;
