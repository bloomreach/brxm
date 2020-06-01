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
  constructor($element, $scope, $timeout, CmsService) {
    'ngInject';

    this.$element = $element;
    this.$scope = $scope;
    this.$timeout = $timeout;
    this.CmsService = CmsService;
  }

  $onInit() {
    // The hidden label pushes the buttons to the right when no image is shown.
    // In case of multiple images, only the buttons of the first image should be
    // pushed by both the default thumbnail width and the optional label text width.
    // All other buttons will be pushed by the default thumbnail width only.
    this.hiddenLabel = this.index === 0 ? this.ariaLabel + (this.isRequired ? ' *' : '') : '';

    if (this.index === 0) {
      this.$scope.$on('primitive-field:focus', ($event, focusEvent) => this.onFocusFromParent(focusEvent));
    }
  }

  onFocusFromParent(focusEvent) {
    // Don't let the click event bubble through the label as it can trigger an
    // unexpected click on the input element
    focusEvent.preventDefault();

    if (this.ngModel.$modelValue === '') {
      this.openImagePicker();
    } else {
      this._focusClearButton();
    }
  }

  setFocus() {
    if (this._hasImage()) {
      this._focusClearButton();
    } else {
      this._focusSelectButton();
    }
  }

  // set "buttonHasFocus" to false after a short timeout to prevent the bottom-border styling
  // of the image picker to flicker while tabbing; it *can* trigger a blur event, followed by
  // a immediate focus event, in which case the blue bottom border will be removed and added
  // again, resulting in annoying flickering of the UI.
  onBlurButton($event) {
    this.blurPromise = this.$timeout(() => {
      this.buttonHasFocus = false;
    }, 10);
    this.onBlur($event);
  }

  onFocusButton($event) {
    if (this.blurPromise) {
      this.$timeout.cancel(this.blurPromise);
    }
    this.buttonHasFocus = true;
    this.onFocus($event);
  }

  openImagePicker() {
    const uuid = this.ngModel.$modelValue;
    this.CmsService.publish('show-image-picker', this.config.imagepicker, { uuid },
      image => this._onImagePicked(image),
      () => this.setFocus(),
    );
  }

  _onImagePicked(image) {
    this.$scope.$apply(() => {
      // if no image has been picked yet, we rely on the focus-if directive to set focus on the image element during rendering.
      // Otherwise the focus-if directive will not trigger so we explicitly set focus on the image element.
      if (this.imagePicked) {
        this._focusClearButton();
      }
      this.imagePicked = true;
      this.url = image.url;
      this.ngModel.$setViewValue(image.uuid);
    });
  }

  _hasImage() {
    return this.$element.find('img').length > 0;
  }

  _focusClearButton() {
    this.$element.find('.hippo-imagelink-clear').focus();
  }

  _focusSelectButton() {
    this.$element.find('.hippo-imagelink-select').focus();
  }

  clearPickedImage() {
    // hide the image before the animations start to prevent it from being pushed down first
    this.$element.find('img').hide();
    this.imagePicked = false;
    this.url = '';
    this.ngModel.$setViewValue('');

    this._focusSelectButton();
  }
}

export default ImageLinkController;
