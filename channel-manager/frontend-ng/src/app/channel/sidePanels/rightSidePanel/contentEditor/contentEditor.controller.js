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
  constructor($scope, $translate, ContentEditor) {
    'ngInject';

    this.$scope = $scope;
    this.ContentEditor = ContentEditor;
    this.cancelLabel = $translate.instant('CANCEL');
    this.closeLabel = $translate.instant('CLOSE');
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

  isSaveAllowed() {
    return this.isEditing() && this._isDocumentDirty() && this.form.$valid && this.ContentEditor.getDocumentType().canCreateAllRequiredFields;
  }

  _isDocumentDirty() {
    return this.ContentEditor.isDocumentDirty();
  }

  notAllFieldsShown() {
    return this.ContentEditor.isEditing() && !this.ContentEditor.getDocumentType().allFieldsIncluded;
  }

  alternativeStep2() {
    return this.ContentEditor.isEditing() && !this.ContentEditor.getDocumentType().canCreateAllRequiredFields;
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

  closeButtonLabel() {
    return this._isDocumentDirty() ? this.cancelLabel : this.closeLabel;
  }

  save() {
    this.ContentEditor.save()
      .then(() => {
        this.form.$setPristine();
        this.onSave();
      });
  }

  switchEditor() {
    if (angular.isFunction(this.onSwitchEditor)) {
      this.onSwitchEditor();
    }
  }
}

export default ContentEditorCtrl;
