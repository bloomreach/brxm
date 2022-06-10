/*
 * Copyright 2018-2022 Bloomreach (https://www.bloomreach.com)
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
  constructor($state, $q, CmsService) {
    'ngInject';

    this.$state = $state;
    this.$q = $q;
    this.CmsService = CmsService;
  }

  /**
   * @param currentPath the current path value (a string), can be undefined
   * @param pickerConfig the configuration properties for the path picker dialog
   * @param pickerContext the context properties for the path picker dialog
   * @returns {*} a promise that resolves to the picked path and its display value,
   *          as an object with two properties: { path, displayName }. The promise
   *          is rejected when picking is canceled.
   */
  pickPath(currentPath, pickerConfig, pickerContext) {
    return this._pickItem('show-path-picker', currentPath, pickerConfig, this._createContext(pickerContext));
  }

  /**
   * @param currentLink the current link object
   * @param pickerConfig the configuration properties for the link picker dialog
   * @param pickerContext the context properties for the link picker dialog
   * @returns {*} a promise that resolves to the picked link object.
   *          The promise is rejected when picking is canceled.
   */
  pickLink(currentLink, pickerConfig, pickerContext) {
    return this._pickItem('show-link-picker', currentLink, pickerConfig, this._createContext(pickerContext));
  }

  /**
   * @param currentImage the current image object
   * @param pickerConfig the configuration properties for the image picker dialog
   * @param pickerContext the context properties for the image picker dialog
   * @returns {*} a promise that resolved to the picked image object.
   *          The promise is rejected when picking is canceled.
   */
  pickImage(currentImage, pickerConfig, pickerContext) {
    return this._pickItem('show-image-picker', currentImage, pickerConfig, this._createContext(pickerContext));
  }

  _pickItem(event, value, config, context) {
    const deferred = this.$q.defer();

    this.CmsService.publish(event,
      { config, context },
      value,
      pickedItem => deferred.resolve(pickedItem),
      () => deferred.reject());

    return deferred.promise;
  }

  _createContext(pickerContext) {
    const stateContext = Object.fromEntries(Object.entries(this.$state.params)
      .filter(([, value]) => value !== null && value !== ''));

    return { ...stateContext, ...pickerContext }
  }
}

export default PickerService;
