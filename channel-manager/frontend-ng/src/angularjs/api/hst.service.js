/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

let q;
let http;

function removeLeadingSlashes(path) {
  return path.replace(/^\/*/, '');
}

function removeTrailingSlashes(path) {
  return path.replace(/\/*$/, '');
}

function concatPaths(path1, path2) {
  const path1Trimmed = removeTrailingSlashes(path1.trim());
  const path2Trimmed = removeLeadingSlashes(path2.trim());
  return `${path1Trimmed}/${path2Trimmed}`;
}

export class HstService {
  constructor($q, $http, ConfigService) {
    'ngInject';

    q = $q;
    http = $http;

    this.config = ConfigService;
  }

  initializeSession(hostname, mountId) {
    return this.doGet(this.config.rootUuid, 'composermode', hostname, mountId)
      .then((response) => !!(response && response.data && response.data.canWrite));
  }

  getChannel(id) {
    return this.doGet(this.config.rootUuid, 'channels', id);
  }

  getSiteMap(id) {
    return this.doGet(id, 'pages')
      .then((response) => response.data.pages);
  }

  doGet(uuid, ...pathElements) {
    return this._callHst('GET', uuid, pathElements);
  }

  doGetWithParams(uuid, params, ...pathElements) {
    return this._callHst('GET', uuid, pathElements, undefined, params);
  }

  doPost(data, uuid, ...pathElements) {
    return this._callHst('POST', uuid, pathElements, data);
  }

  _callHst(method, uuid, pathElements, data, params) {
    const url = this._createApiUrl(uuid, pathElements, params);
    const headers = {
      'CMS-User': this.config.cmsUser,
      FORCE_CLIENT_HOST: 'true',
    };
    return q((resolve, reject) => {
      http({ method, url, headers, data })
        .success((response) => resolve(response))
        .error((error) => reject(error));
    });
  }

  _createApiUrl(uuid, pathElements, params) {
    let apiUrl = concatPaths(this.config.contextPath, this.config.apiUrlPrefix);
    apiUrl = concatPaths(apiUrl, uuid);
    apiUrl += './';

    pathElements.forEach((pathElement) => {
      apiUrl = concatPaths(apiUrl, pathElement);
    });

    if (params) {
      apiUrl += `?${this._serializeParams(params)}`;
    }

    return apiUrl;
  }

  _serializeParams(params) {
    const str = Object.keys(params)
      .map((param) => `${encodeURIComponent(param)}=${encodeURIComponent(params[param])}`);
    return str.join('&');
  }

  /**
   * Add a component to the specified container.
   *
   * @param catalogComponent
   * @param containerId
   * @returns {*} a promise. If creation is successful, it contains a JSON object describing new component.
   * Otherwise, it contains the error response.
   */
  addHstComponent(catalogComponent, containerId) {
    return this.doPost(null, containerId, 'create', catalogComponent.id)
      .then((response) => response.data);
  }

  removeHstComponent(containerId, componentId) {
    return this.doGet(containerId, 'delete', componentId);
  }
}
