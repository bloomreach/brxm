/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
    this.inputElement = this.$element.find('input');
  }

  openImagePicker() {
    const uuid = this.ngModel.$modelValue;
    this.CmsService.publish('show-image-picker', this.id, this.config.imagepicker, { uuid }, (image) => {
      this.$scope.$apply(() => {
        this.url = image.url;
        this.ngModel.$setViewValue(image.uuid);
      });
    }, () => {
      // Cancel callback
    });
  }

  clearPickedImage() {
    this.url = '';
    this.ngModel.$setViewValue('');
    this.inputElement.blur();
  }
}

export default ImageLinkController;
