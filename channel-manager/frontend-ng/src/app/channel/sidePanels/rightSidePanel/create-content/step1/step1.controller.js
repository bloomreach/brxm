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

import { Observable } from 'rxjs';

class RightSidePanelCtrl {
  constructor(
    $translate,
    CreateContentService,
    FeedbackService) {
    'ngInject';

    this.$translate = $translate;
    this.CreateContentService = CreateContentService;
    this.FeedbackService = FeedbackService;

    this.title = 'Create new content';
    this.isFullWidth = false;
    this.documentType = null;

    this.nameUrlFields = {
      nameField: '',
      urlField: '',
      urlUpdate: false,
    };

    this.documentLocationField = {
      rootPath: null,
      defaultPath: null,
    };
  }

  $onInit() {
    if (!this.options) {
      throw new Error('Input "options" is required');
    }

    if (!this.options.templateQuery) {
      throw new Error('Configuration option "templateQuery" is required');
    }

    Observable.fromPromise(this.CreateContentService.getTemplateQuery(this.options.templateQuery)).subscribe(
      templateQuery => this._onLoadDocumentTypes(templateQuery.documentTypes),
      error => this._onErrorLoadingTemplateQuery(error),
    );
  }

  submit() {
    const document = {
      name: this.nameUrlFields.nameField,
      slug: this.nameUrlFields.urlField,
      templateQuery: this.options.templateQuery,
      documentTypeId: this.documentType,
      rootPath: this.documentLocationField.rootPath,
      defaultPath: this.documentLocationField.defaultPath,
    };

    Observable.fromPromise(this.CreateContentService.createDraft(document)).subscribe(
      () => {
        this.onContinue({
          data: {
            name: this.nameUrlFields.nameField,
            url: this.nameUrlFields.urlField,
            locale: this.locale,
          },
        });
      },
      error => this._onErrorCreatingDraft(error),
    );
  }

  setWidthState(state) {
    this.isFullWidth = state;
    this.onFullWidth({ state });
  }

  setLocale(locale) {
    this.locale = locale;
  }

  _onLoadDocumentTypes(types) {
    this.documentTypes = types;

    if (this.documentTypes.length === 1) {
      this.documentType = this.documentTypes[0].id;
    }
  }

  _onErrorLoadingTemplateQuery(error) {
    if (error.data && error.data.reason) {
      const errorKey = this.$translate.instant(`ERROR_${error.data.reason}`);
      this.FeedbackService.showError(errorKey);
    } else {
      console.error('Unknown error loading template query', error);
    }
  }

  _onErrorCreatingDraft(error) {
    if (error.data && error.data.reason) {
      const errorKey = this.$translate.instant(`ERROR_${error.data.reason}`);
      this.FeedbackService.showError(errorKey);
    } else {
      console.error('Unknown error creating new draft document', error);
    }
  }
}

export default RightSidePanelCtrl;
