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

class Step2Service {
  constructor($q, $translate, CmsService, ContentService, DialogService, FeedbackService, FieldService) {
    'ngInject';

    this.$q = $q;
    this.$translate = $translate;
    this.CmsService = CmsService;
    this.ContentService = ContentService;
    this.DialogService = DialogService;
    this.FeedbackService = FeedbackService;
    this.FieldService = FieldService;

    this._reset();
  }

  _reset() {
    this.data = {};
  }

  getData() {
    return this.data;
  }

  open(document, name, url, locale) {
    this._reset();

    return this.ContentService.getDocumentType(document.info.type.id)
      .then((docType) => {
        this.document = document;
        this.data.docType = docType;

        this.document.displayName = name;
        this.documentUrl = url;
        this.documentLocale = locale;

        this.FieldService.setDocumentId(document.id);

        return this.data;
      });
  }

  confirmDiscardChanges() {
    const messageParams = {
      documentName: this.data.document.displayName,
    };

    const confirm = this.DialogService.confirm()
      .title(this.$translate.instant('DISCARD_DOCUMENT', messageParams))
      .textContent(this.$translate.instant('CONFIRM_DISCARD_NEW_DOCUMENT', messageParams))
      .ok(this.$translate.instant('DISCARD'))
      .cancel(this.$translate.instant('CANCEL'));

    return this.DialogService.show(confirm);
  }

  deleteDraft(id) {
    return this.ContentService._send('DELETE', ['documents', id]);
  }

  setDraftNameUrl(documentId, data) {
    const nameUrl = { displayName: data.name, urlName: data.url };
    return this.ContentService._send('PUT', ['documents', documentId], nameUrl);
  }
}

export default Step2Service;
