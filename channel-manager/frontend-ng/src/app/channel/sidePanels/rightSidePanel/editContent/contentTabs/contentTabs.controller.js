/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

class ContentTabsCtrl {
  constructor(
    $uiRouterGlobals,
    ContentEditor,
    HippoIframeService,
  ) {
    'ngInject';

    this.$uiRouterGlobals = $uiRouterGlobals;

    this.ContentEditor = ContentEditor;
    this.HippoIframeService = HippoIframeService;
  }

  $onInit() {
    this.showVersionsInfo = this.$uiRouterGlobals.params.showVersionsInfo;
  }

  uiCanExit() {
    if (this.ContentEditor.isRetainable()) {
      return this.ContentEditor.keepDraft()
        .finally(() => this.ContentEditor.close());
    }
    return this._confirmExit()
      .then(() => this.ContentEditor.discardChanges()
        .catch(() => {
          // ignore errors of discardChanges: if it fails (e.g. because an admin unlocked the document)
          // the editor should still be closed.
        })
        .finally(() => this.ContentEditor.close()));
  }

  _confirmExit() {
    const titleKey = this.ContentEditor.isDocumentXPage ? 'SAVE_XPAGE_CHANGES_TITLE' : 'SAVE_DOCUMENT_CHANGES_TITLE';
    const messageKey = this.ContentEditor.isDocumentXPage ? 'SAVE_CHANGES_TO_XPAGE' : 'SAVE_CHANGES_TO_DOCUMENT';

    return this.ContentEditor.confirmSaveOrDiscardChanges(messageKey, {}, titleKey)
      .then((action) => {
        if (action === 'SAVE') {
          this.HippoIframeService.reload();
        }
      });
  }
}

export default ContentTabsCtrl;
