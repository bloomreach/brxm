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

class PickerService {
  constructor($q, CmsService) {
    'ngInject';

    this.$q = $q;
    this.CmsService = CmsService;
  }

  /**
   * @param pickerConfig the configuration properties for the path picker dialog
   * @param currentPath the current path value (a string), can be undefined
   * @returns {*} a promise that resolves to the picked path and its display value,
   *          as an object with two properties: { path, displayName }. The promise
   *          is rejected when picking is canceled.
   */
  pickPath(pickerConfig, currentPath) {
    return this._pickItem('show-path-picker', pickerConfig, currentPath);
  }

  /**
   * @param pickerConfig the configuration properties for the link picker dialog
   * @param currentLink the current link object
   * @returns {*} a promise that resolves to the picked link object.
   *          The promise is rejected when picking is canceled.
   */
  pickLink(pickerConfig, currentLink) {
    return this._pickItem('show-link-picker', pickerConfig, currentLink);
  }

  /**
   * @param pickerConfig the configuration properties for the image picker dialog
   * @param currentImage the current image object
   * @returns {*} a promise that resolved to the picked image object.
   *          The promise is rejected when picking is canceled.
   */
  pickImage(pickerConfig, currentImage) {
    return this._pickItem('show-image-picker', pickerConfig, currentImage);
  }

  _pickItem(event, config, currentItem) {
    const deferred = this.$q.defer();

    this.CmsService.publish(event,
      config,
      currentItem,
      pickedItem => deferred.resolve(pickedItem),
      () => deferred.reject());

    return deferred.promise;
  }
}

export default PickerService;
