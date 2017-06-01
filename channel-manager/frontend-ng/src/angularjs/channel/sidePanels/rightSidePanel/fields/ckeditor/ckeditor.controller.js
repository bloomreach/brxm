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
  constructor($scope, $element, $window, CKEditorService, CmsService, ConfigService, DomService, SharedSpaceToolbarService) {
    'ngInject';

    this.$scope = $scope;
    this.$element = $element;
    this.$window = $window;
    this.CKEditorService = CKEditorService;
    this.CmsService = CmsService;
    this.ConfigService = ConfigService;
    this.DomService = DomService;
    this.SharedSpaceToolbarService = SharedSpaceToolbarService;
  }

  $onInit() {
    this.textAreaElement = this.$element.find('textarea');

    this.CKEditorService.loadCKEditor().then((CKEDITOR) => {
      const editorConfig = angular.copy(this.config);

      editorConfig.sharedSpaces = {
        top: 'ckeditor-shared-space-top',
        bottom: 'ckeditor-shared-space-bottom',
      };

      editorConfig.extraPlugins = 'sharedspace,sourcedialog,autogrow';
      editorConfig.removePlugins += 'sourcearea,resize';

      editorConfig.language = this.ConfigService.locale;

      this._applyEditorCSS(editorConfig);

      this.editor = CKEDITOR.replace(this.textAreaElement[0], editorConfig);

      this.ngModel.$render = () => {
        this.processingModelUpdate = true;
        this.editor.setData(this.ngModel.$viewValue);
        this.processingModelUpdate = false;
      };

      this.editor.on('change', () => {
        if (!this.processingModelUpdate) {
          this.onEditorChange();
        }
      });

      this.editor.on('focus', () => this.onEditorFocus());
      this.editor.on('blur', () => this.onEditorBlur());
      this.editor.on('openLinkPicker', event => this._openLinkPicker(event.data));
      this.editor.on('openImagePicker', event => this._openImagePicker(event.data));
    });
  }

  $onDestroy() {
    this.editor.destroy();
  }

  _applyEditorCSS(editorConfig) {
    if (editorConfig.contentsCss) {
      if (!Array.isArray(editorConfig.contentsCss)) {
        editorConfig.contentsCss = [editorConfig.contentsCss];
      }
      const files = editorConfig.contentsCss.map(file => `../../${file}`);
      editorConfig.contentsCss = files;
      this.DomService.addCssLinks(this.$window, files);
    }
  }

  onEditorChange() {
    this.$scope.$apply(() => {
      this.ngModel.$setViewValue(this.editor.getData());
    });
  }

  onEditorFocus() {
    this.$scope.$apply(() => {
      this.textAreaElement.addClass('focused');
      this.onFocus();
    });

    this.SharedSpaceToolbarService.showToolbar({
      hasBottomToolbar: this.config.hasBottomToolbar,
    });
  }

  onEditorBlur() {
    this.$scope.$apply(() => {
      this.textAreaElement.removeClass('focused');
      this.onBlur();
    });

    this.SharedSpaceToolbarService.hideToolbar();
  }

  _openLinkPicker(selectedLink) {
    const linkPickerConfig = this.config.hippopicker.internalLink;
    this.CmsService.publish('show-link-picker', this.id, linkPickerConfig, selectedLink, (link) => {
      this.editor.execCommand('insertInternalLink', link);
    });
  }

  _openImagePicker(selectedImage) {
    const imagePickerConfig = this.config.hippopicker.image;
    this.CmsService.publish('show-image-picker', this.id, imagePickerConfig, selectedImage, (image) => {
      // Images are rendered with a relative path, pointing to the binaries servlet. The binaries servlet always
      // runs at the same level; two directories up from the angular app. Because of this we need to prepend
      // all internal images with a prefix as shown below.
      image.f_url = `../../${image.f_url}`;

      this.editor.execCommand('insertImage', image);
    });
  }
}

export default CKEditorController;
