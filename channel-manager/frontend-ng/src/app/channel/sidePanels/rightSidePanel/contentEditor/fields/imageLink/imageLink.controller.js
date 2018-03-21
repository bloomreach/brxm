/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

class ImageLinkController {
  constructor($scope, $element, CmsService) {
    'ngInject';

    this.$scope = $scope;
    this.$element = $element;
    this.CmsService = CmsService;
  }

  $onInit() {
    // The hidden label pushes the buttons to the right when no image is shown.
    // In case of multiple images, only the buttons of the first image should be
    // pushed by both the default thumbnail width and the optional label text width.
    // All other buttons will be pushed by the default thumbnail width only.
    this.hiddenLabel = this.index === 0 ? this.ariaLabel + (this.isRequired ? ' *' : '') : '';
    this.selectElement = this.$element.find('.hippo-imagelink-select');
  }

  openImagePicker() {
    const uuid = this.ngModel.$modelValue;
    this.CmsService.publish('show-image-picker', this.config.imagepicker, { uuid },
      image => this._onImagePicked(image),
      () => this._onImagePickCancelled(),
    );
  }

  _onImagePicked(image) {
    this.$scope.$apply(() => {
      this.imagePicked = true;
      this.url = image.url;
      this.ngModel.$setViewValue(image.uuid);
      this._focusSelectButton();
    });
  }

  _onImagePickCancelled() {
    this._focusSelectButton();
  }

  _focusSelectButton() {
    // focus the select button so pressing ESC again closes the right side-panel
    this.selectElement.focus();
  }

  clearPickedImage() {
    // hide the image before the animations start to prevent it from being pushed down first
    this.$element.find('img').hide();
    this.imagePicked = false;
    this.url = '';
    this.ngModel.$setViewValue('');
  }
}

export default ImageLinkController;
