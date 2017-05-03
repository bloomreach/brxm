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
  constructor($scope, $element, CKEditorService, CmsService, ConfigService) {
    'ngInject';

    this.$scope = $scope;
    this.$element = $element;
    this.CKEditorService = CKEditorService;
    this.CmsService = CmsService;
    this.ConfigService = ConfigService;
  }

  $onInit() {
    this.textAreaElement = this.$element.find('textarea');

    this.CKEditorService.loadCKEditor().then((CKEDITOR) => {
      this.config.language = this.ConfigService.locale;

      this.editor = CKEDITOR.replace(this.textAreaElement[0], this.config);
      this.editor.setData(this.ngModel.$viewValue);

      this.editor.on('change', () => this.onEditorChange());
      this.editor.on('focus', () => this.onEditorFocus());
      this.editor.on('blur', () => this.onEditorBlur());
      this.editor.on('openLinkPicker', event => this._openLinkPicker(event.data));
    });
  }

  $onDestroy() {
    this.editor.destroy();
  }

  onEditorChange() {
    this.$scope.$apply(() => {
      this.ngModel.$setViewValue(this.editor.getData());
    });
  }

  onEditorFocus() {
    this.$scope.$apply(() => {
      this.textAreaElement.addClass('focussed');
      this.onFocus();
    });
  }

  onEditorBlur() {
    this.$scope.$apply(() => {
      this.textAreaElement.removeClass('focussed');
      this.onBlur();
    });
  }

  _openLinkPicker(selectedLink) {
    this.CmsService.publish('show-link-picker', this.id, this._linkPickerConfig(), selectedLink, this._onLinkPicked.bind(this));
  }

  _linkPickerConfig() {
    return this.config.hippopicker.internalLink;
  }

  _onLinkPicked(link) {
    this.editor.execCommand('insertInternalLink', link);
  }
}

export default CKEditorController;
