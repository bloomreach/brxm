/*
 * Copyright 2016-2019 Hippo B.V. (http://www.onehippo.com)
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

const FORM_HEADERS = {
  'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
};

class HstService {
  constructor(
    $http,
    $q,
    CmsService,
    ConfigService,
    PathService,
  ) {
    'ngInject';

    this.$q = $q;
    this.$http = $http;

    this.CmsService = CmsService;
    this.ConfigService = ConfigService;
    this.PathService = PathService;
  }

  initializeSession(channel) {
    this.contextPath = channel.contextPath;
    this.hostGroup = channel.hostGroup;

    return this.doGet(this.ConfigService.rootUuid, 'composermode', channel.hostname, channel.mountId)
      .then((response) => {
        if (response) {
          return response.data;
        }
        return null;
      });
  }

  getChannel(id, contextPath, hostGroup) {
    if (!id) {
      return this.$q.reject('Channel id must be defined');
    }

    const currentContextPath = this.contextPath;
    const currentHostGroup = this.hostGroup;
    try {
      this.contextPath = contextPath;
      this.hostGroup = hostGroup;
      return this.doGet(this.ConfigService.rootUuid, 'channels', id);
    } catch (e) {
      return this.$q.reject(e);
    } finally {
      this.contextPath = currentContextPath;
      this.hostGroup = currentHostGroup;
    }
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

  // The legacy HST endpoints expect FormData instead of JSON objects
  // e.g. org.hippoecm.hst.pagecomposer.jaxrs.services.ContainerItemComponentResource#moveAndUpdateVariant
  doPutForm(data, uuid, ...pathElements) {
    data = this._serializeParams(data);
    return this._callHst('PUT', uuid, pathElements, data, null, FORM_HEADERS);
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
    headers['CMS-User'] = this.ConfigService.cmsUser;
    headers.contextPath = this.contextPath;
    headers.hostGroup = this.hostGroup;

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

  _createApiUrl(uuid, pathElements, params) {
    let apiUrl = this.PathService.concatPaths(this.ConfigService.getCmsContextPath(), this.ConfigService.apiUrlPrefix);
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

  addHstComponent(catalogComponent, container, nextComponentId) {
    const containerId = container.getId();
    const pathElements = [catalogComponent.id];
    if (container.isXPageLayoutComponent()) {
      pathElements.unshift(container.getXPageLayoutHippoIdentifier());
    }

    return this.doPost(null, containerId, ...pathElements, nextComponentId);
  }

  updateHstContainer(containerId, containerRepresentation) {
    return this._callHst('PUT', containerId, [], containerRepresentation);
  }
}

export default HstService;
