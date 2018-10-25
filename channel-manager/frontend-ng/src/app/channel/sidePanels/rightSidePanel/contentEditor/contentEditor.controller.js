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

class ContentEditorCtrl {
  constructor(
    $q,
    $scope,
    $translate,
    CmsService,
    ContentEditor,
  ) {
    'ngInject';

    this.$q = $q;
    this.$scope = $scope;
    this.CmsService = CmsService;
    this.ContentEditor = ContentEditor;
  }

  $onInit() {
    this._monitorDirty();
  }

  _monitorDirty() {
    this.$scope.$watch('$ctrl.form.$dirty', (dirty) => {
      if (dirty) {
        this.ContentEditor.markDocumentDirty();
      }
    });
  }

  isEditing() {
    return this.ContentEditor.isEditing();
  }

  getFieldTypes() {
    return this.ContentEditor.getDocumentType().fields;
  }

  getFieldValues() {
    return this.ContentEditor.getDocument().fields;
  }

  getError() {
    return this.ContentEditor.getError();
  }

  cancelRequestPublication() {
    this.CmsService.reportUsageStatistic('VisualEditingCancelRequest');
    return this.showLoadingIndicator(() => this.ContentEditor.cancelRequestPublication());
  }

  showLoadingIndicator(action) {
    this.loading = true;
    return this.$q.resolve(action())
      .finally(() => {
        this.loading = false;
      });
  }
}

export default ContentEditorCtrl;
