/*
 * Copyright 2020 Bloomreach
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

class TargetingService {
  constructor(
    $http,
    $q,
    $window,
    PathService,
  ) {
    'ngInject';

    this.$http = $http;
    this.$q = $q;
    this.$window = $window;
    this.PathService = PathService;

    this.init();
  }

  init() {
    const iframe = this.$window.parent;
    if (!iframe.Hippo) {
      throw new Error('Failed to retrieve Hippo object from global scope');
    }

    if (!iframe.Hippo.Targeting) {
      throw new Error('Failed to retrieve targeting configuration from global scope, is relevance enabled?');
    }

    const { Targeting } = iframe.Hippo;

    this._apiUrl = Targeting.HttpProxy.REST_URL;
    this._collectors = Targeting.CollectorPlugins || {};
  }

  async getPersonas() {
    const params = {
      collectors: Object.keys(this._collectors).join(','),
      ...this._getDefaultParams(),
    };

    try {
      const result = await this._execute('GET', ['personas'], null, params);
      return this._success('Personas loaded successfully', result);
    } catch (e) {
      return this._failure('Failed to load personas', e);
    }
  }

  async getCharacteristics() {
    try {
      const result = await this._execute('GET', ['characteristics'], null, this._getDefaultParams());
      return this._success('Characteristics loaded successfully', result);
    } catch (e) {
      return this._failure('Failed to load characteristics', e);
    }
  }

  async getCharacteristic(id) {
    try {
      const result = await this._execute('GET', ['characteristics', id], null, this._getDefaultParams());
      return this._success(`Characteristic "${id}" loaded successfully`, result);
    } catch (e) {
      return this._failure(`Failed to load characteristic "${id}"`, e);
    }
  }

  _getDefaultParams() {
    return {
      'Force-Client-Host': true,
      antiCache: Date.now(),
    };
  }

  _success(message, data) {
    if (data.hasOwnProperty('data')) {
      data.message = data.message || message;
      return data;
    }

    return {
      data,
      message,
      reloadRequired: false,
      success: true,
    };
  }

  _failure(message, data) {
    if (data.hasOwnProperty('data')) {
      data.message = data.message || message;
      return data;
    }

    return {
      data,
      message,
      reloadRequired: false,
      success: false,
    };
  }

  _execute(method, pathElements, data, params, headers = {}) {
    const url = this._createApiUrl(pathElements, params);
    const canceller = this.$q.defer();
    const promise = this.$q((resolve, reject) => {
      this.$http({
        method,
        url,
        headers,
        data,
        timeout: canceller.promise,
      })
        .then(response => resolve(response.data))
        .catch(error => reject(error.data));
    });

    promise.cancel = () => {
      canceller.resolve();

      return promise;
    };

    return promise;
  }

  _createApiUrl(pathElements = [], params) {
    pathElements.unshift(this._apiUrl);

    let apiUrl = this.PathService.concatPaths(...pathElements);

    if (params) {
      apiUrl += `?${this._serializeParams(params)}`;
    }

    return apiUrl;
  }

  _serializeParams(params) {
    const str = Object.keys(params)
      .map(param => `${encodeURIComponent(param)}=${encodeURIComponent(params[param])}`);
    return str.join('&');
  }
}

export default TargetingService;
