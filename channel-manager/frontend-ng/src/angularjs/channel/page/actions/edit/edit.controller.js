/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

export class PageEditCtrl {
  constructor($translate, SiteMapItemService, ChannelService) {
    'ngInject';

    this.ChannelService = ChannelService;

    const documentNone = {
      displayName: $translate.instant('SUBPAGE_PAGE_EDIT_PRIMARY_DOCUMENT_VALUE_NONE'),
      path: '',
    };

    // The PageActionsCtrl has retrieved the page meta-data when opening the page menu.
    // Now, it is available through the SiteMapItemService.
    this.page = SiteMapItemService.get();
    this.isEditable = SiteMapItemService.isEditable();

    this.heading = $translate.instant('SUBPAGE_PAGE_EDIT_TITLE', { pageName: this.page.name });
    this.title = this.page.pageTitle;
    this.availableDocuments = this.page.availableDocumentRepresentations;
    this.availableDocuments.unshift(documentNone);
    if (this.page.primaryDocumentRepresentation) {
      this.primaryDocument = this.availableDocuments.find((dr) => dr.path === this.page.primaryDocumentRepresentation.path);
    } else {
      this.primaryDocument = documentNone;
    }
  }

  back() {
    this.onDone();
  }

  save() {
    // TODO save changes
    this.onDone();
  }

  retrievePrototypes() {
    this.ChannelService.getNewPageModel()
      .then((data) => {
        this.prototypes = data.prototypes;
      })
      .catch(() => {
        this._showError('SUBPAGE_PAGE_ADD_ERROR_MODEL_RETRIEVAL_FAILED');
      });
  }
}

