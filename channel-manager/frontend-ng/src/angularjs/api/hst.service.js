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

const HANDSHAKE_PATH = '/composermode/';

let q;
let http;

export class HstService {
  constructor($q, $http, ConfigService) {
    'ngInject';

    q = $q;
    http = $http;

    this.config = ConfigService;
    this._contextPath = ConfigService.defaultContextPath;
  }

  get contextPath() {
    return this._contextPath;
  }

  set contextPath(path) {
    this._contextPath = path;
  }

  get apiPath() {
    return this._contextPath + this.config.apiUrlPrefix + this.config.rootResource;
  }

  initializeSession(channel) {
    const url = this.apiPath + HANDSHAKE_PATH + channel.hostname + '/';

    const headers = {
      'CMS-User': this.config.cmsUser,
      FORCE_CLIENT_HOST: 'true',
    };

    return q((resolve, reject) => {
      http.get(url, { headers })
        .success((response) => resolve(!!(response && response.data && response.data.canWrite)))
        .error((error) => reject(error));
    });
  }
}
