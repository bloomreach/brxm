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

class HstService {
  constructor($q, $http, CmsService, ConfigService, PathService) {
    'ngInject';

    q = $q;
    http = $http;

    this.CmsService = CmsService;
    this.config = ConfigService;
    this.PathService = PathService;
  }

  initializeSession(hostname, mountId) {
    return this.doGet(this.config.rootUuid, 'composermode', hostname, mountId)
      .then((response) => {
        if (response) {
          return response.data;
        }
        return null;
      });
  }

  getChannel(id) {
    if (!id) {
      throw new Error('Channel id must be defined');
    }
    return this.doGet(this.config.rootUuid, 'channels', id);
  }

  getSiteMap(id) {
    return this.doGet(id, 'pages')
      .then(response => response.data.pages);
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

  doPostWithHeaders(uuid, headers, ...pathElements) {
    return this._callHst('POST', uuid, pathElements, undefined, undefined, headers);
  }

  doPostWithParams(data, uuid, params, ...pathElements) {
    return this._callHst('POST', uuid, pathElements, data, params);
  }

  doPut(data, uuid, ...pathElements) {
    return this._callHst('PUT', uuid, pathElements, data);
  }

  doPutWithHeaders(uuid, headers, ...pathElements) {
    return this._callHst('PUT', uuid, pathElements, undefined, undefined, headers);
  }

  doDelete(uuid, ...pathElements) {
    return this._callHst('DELETE', uuid, pathElements);
  }

  _callHst(method, uuid, pathElements, data, params, headers) {
    const url = this._createApiUrl(uuid, pathElements, params);

    headers = headers || {};
    headers['CMS-User'] = this.config.cmsUser;
    headers['Force-Client-Host'] = 'true';

    this.CmsService.publish('user-activity');

    return q((resolve, reject) => {
      http({ method, url, headers, data })
        .success(response => resolve(response))
        .error(error => reject(error));
    });
  }

  _createApiUrl(uuid, pathElements, params) {
    let apiUrl = this.PathService.concatPaths(this.config.contextPath, this.config.apiUrlPrefix);
    apiUrl = this.PathService.concatPaths(apiUrl, uuid);
    apiUrl += './';

    pathElements.forEach((pathElement) => {
      if (pathElement) {
        apiUrl = this.PathService.concatPaths(apiUrl, pathElement);
      }
    });

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

  /**
   * Add a component to the specified container.
   *
   * @param catalogComponent
   * @param containerId
   * @returns {*} a promise. If creation is successful, it contains a JSON object describing new component.
   * Otherwise, it contains the error response.
   */
  addHstComponent(catalogComponent, containerId) {
    return this.doPost(null, containerId, catalogComponent.id);
  }

  removeHstComponent(containerId, componentId) {
    return this._callHst('DELETE', containerId, [componentId]);
  }

  updateHstComponent(containerId, containerRepresentation) {
    return this._callHst('PUT', containerId, [], containerRepresentation);
  }
}

export default HstService;
