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
  constructor($scope, $element, CKEditorService, ConfigService) {
    'ngInject';

    this.$scope = $scope;
    this.$element = $element;
    this.CKEditorService = CKEditorService;
    this.ConfigService = ConfigService;
    this.textAreaElement = $element.find('textarea');
  }

  $onInit() {
    this.CKEditorService.loadCKEditor().then((CKEDITOR) => {
      const textAreaElement = this.$element.find('textarea')[0];

      this.config.language = this.ConfigService.locale;

      this.editor = CKEDITOR.replace(textAreaElement, this.config);
      this.editor.setData(this.model.$viewValue);

      this.editor.on('change', () => {
        const html = this.editor.getData();
        this.model.$setViewValue(html);
      });

      this.editor.on('focus', () => {
        this.textAreaElement.focus();
        this.$scope.$apply(() => this.onFocus());
      });

      this.editor.on('blur', () => {
        this.textAreaElement.blur();
        this.$scope.$apply(() => this.onBlur());
      });

      this.$scope.$on('$destroy', () => {
        this.editor.destroy();
      });
    });
  }
}

export default CKEditorController;
