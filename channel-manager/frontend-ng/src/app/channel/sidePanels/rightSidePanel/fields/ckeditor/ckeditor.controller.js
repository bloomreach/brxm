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
  constructor($scope, $element, $window, CKEditorService, CmsService, ConfigService, DomService, SharedSpaceToolbarService, FieldService) {
    'ngInject';

    this.$scope = $scope;
    this.$element = $element;
    this.$window = $window;
    this.CKEditorService = CKEditorService;
    this.CmsService = CmsService;
    this.ConfigService = ConfigService;
    this.DomService = DomService;
    this.FieldService = FieldService;
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
      editorConfig.removePlugins += 'sourcearea,resize,maximize';
      editorConfig.removeButtons = 'Source';

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

      this.editor.on('focus', $event => this.onEditorFocus($event));
      this.editor.on('openLinkPicker', event => this._openLinkPicker(event.data));
      this.editor.on('openImagePicker', event => this._openImageVariantPicker(event.data));

      // CKEditor has been replaced and instance is ready
      this.editor.on('instanceReady', () => {
        this.editableElement = this.$element.find('.cke_editable');
        this.editableElement.on('blur', ($event) => { this.blurEvent = $event; });
        this.editor.on('blur', () => this.onEditorBlur(this.blurEvent));

        this.editor.on('dialogShow', () => {
          this.SharedSpaceToolbarService.isToolbarPinned = true;
        });
        this.editor.on('dialogHide', () => { this.SharedSpaceToolbarService.isToolbarPinned = false; });
      });
      this._validate();
    });
  }

  $onDestroy() {
    this.editor.destroy();
    this.SharedSpaceToolbarService.hideToolbar();
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
    this._validate();
  }

  _validate() {
    this.fieldObject.$setValidity('required', true);

    const rawValue = this._getRawValue();
    // Validate 'required', field should have a value
    if (this.isRequired) {
      if (!rawValue.length) {
        this.fieldObject.$setValidity('required', false);
      }
    }
  }

  onEditorFocus() {
    this.$scope.$apply(() => {
      this.textAreaElement.addClass('focused');
    });

    this.onFocus({
      $event: {
        target: this.$element.find('.cke_editable'),
        customFocus: () => this.editor.focus(),
      },
    });

    if (!this.SharedSpaceToolbarService.isToolbarVisible) {
      this.SharedSpaceToolbarService.showToolbar({
        hasBottomToolbar: this.config.hippo && this.config.hippo.hasBottomToolbar,
      });
    }
    this._validate();
  }

  onEditorBlur($event) {
    this.onBlur({ $event });

    const relatedTarget = angular.element($event.relatedTarget);
    if (!this.FieldService.shouldPreserveFocus(relatedTarget) && this.SharedSpaceToolbarService.isToolbarPinned === false) {
      this.SharedSpaceToolbarService.hideToolbar();
    }

    this._validate();
  }

  _openLinkPicker(selectedLink) {
    this.SharedSpaceToolbarService.isToolbarPinned = true;
    const linkPickerConfig = this.config.hippopicker.internalLink;
    this.CmsService.publish('show-link-picker', this.id, linkPickerConfig, selectedLink, (link) => {
      this.editor.execCommand('insertInternalLink', link);
      this.SharedSpaceToolbarService.isToolbarPinned = false;
    }, () => {
      // Cancel callback
      this.editor.focus();
      this.SharedSpaceToolbarService.isToolbarPinned = false;
    });
  }

  _openImageVariantPicker(selectedImage) {
    const imagePickerConfig = this.config.hippopicker.image;
    this.SharedSpaceToolbarService.isToolbarPinned = true;
    this.CmsService.publish('show-image-variant-picker', this.id, imagePickerConfig, selectedImage, (image) => {
      // Images are rendered with a relative path, pointing to the binaries servlet. The binaries servlet always
      // runs at the same level; two directories up from the angular app. Because of this we need to prepend
      // all internal images with a prefix as shown below.
      image.f_url = `../../${image.f_url}`;
      this.editor.execCommand('insertImage', image);
      this.SharedSpaceToolbarService.isToolbarPinned = false;
    }, () => {
      // Cancel callback
      this.editor.focus();
      this.SharedSpaceToolbarService.isToolbarPinned = false;
    });
  }

  _getRawValue() {
    // CKEditor field value, stripped from any HTML entities or whitespaces.
    return $(this.editor.getSnapshot()).text().trim();
  }
}

export default CKEditorController;
