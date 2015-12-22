/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

const DEFAULT_SETTINGS = {
  canWrite: false,
  sessionId: null
};

const HANDSHAKE_PATH = '/cafebabe-cafe-babe-cafe-babecafebabe./composermode/';

export class SessionService {
  constructor ($q, $http, ConfigService) {
    'ngInject';

    this.$q = $q;
    this.$http = $http;

    this.canWrite = DEFAULT_SETTINGS.canWrite;
    this.sessionID = DEFAULT_SETTINGS.sessionId;

    this.cmsUser = ConfigService.cmsUser;
    this.handshakePath = ConfigService.apiUrlPrefix + HANDSHAKE_PATH;
  }

  authenticate (channel) {
    const url = channel.contextPath + this.handshakePath + channel.hostname + '/';
    const headers = {
      'CMS-User': this.cmsUser,
      'FORCE_CLIENT_HOST': 'true'
    };

    return this.$q((resolve, reject) => {
      this.$http.get(url, { headers })
        .success((response) => {
          const data = (response && response.data) || DEFAULT_SETTINGS;
          this.canWrite = data.canWrite;
          this.sessionId = data.sessionId;
          resolve(channel);
        })
        .error((error) => reject(error));
    });
  }
}
