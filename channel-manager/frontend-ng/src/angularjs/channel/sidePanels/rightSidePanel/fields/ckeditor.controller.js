/*
 * Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
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

class CKEditorController {
  constructor($scope, $element, CKEditorService) {
    'ngInject';

    this.scope = $scope;
    this.CKEditorService = CKEditorService;
    this.CKEditorElement = $element;
  }

  $onInit() {
    this.CKEditorService.loadCKEditor().then((CKEDITOR) => {
      const textAreaElement = this.CKEditorElement.find('textarea')[0];
      const editorConfig = this.CKEditorService.getConfigByType(this.ckeditorType);

      this.editor = CKEDITOR.replace(textAreaElement, editorConfig);
      this.editor.setData(this.model.$viewValue);

      this.editor.on('change', () => {
        this.scope.$evalAsync(() => {
          const html = this.editor.getData();
          this.model.$setViewValue(html);
        });
      });

      this.editor.on('focus', () => {
        this.onFocus();
        this.scope.$apply();
      });

      this.editor.on('blur', () => {
        this.onBlur();
        this.scope.$apply();
      });
    });

    this.scope.$on('$destroy', () => {
      this.editor.destroy();
    });
  }
}

export default CKEditorController;
