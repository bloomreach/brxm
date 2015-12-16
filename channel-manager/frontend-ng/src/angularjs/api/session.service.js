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

export class SessionService {
  constructor ($http, ConfigService) {
    'ngInject';

    this.$http = $http;
    this.handshakeUrlPart = ConfigService.apiUrlPrefix + '/cafebabe-cafe-babe-cafe-babecafebabe./composermode/';
    this.cmsUser = ConfigService.cmsUser;
    this.canEdit = false;
    this.sessionID = null;
  }

  authenticate (channel) {
    const url = channel.contextPath + this.handshakeUrlPart + channel.hostname + '/';
    const httpConfig = {
      headers: {
        'CMS-User': this.cmsUser,
        'FORCE_CLIENT_HOST': 'true'
      }
    };
    return new Promise((resolve, reject) => {
      this.$http.get(url, httpConfig)
        .success((response) => {
          this.canEdit = response.data.canWrite;
          this.sessionId = response.data.sessionId;
          resolve(channel);
        })
        .error((error) => reject(error));
    });
  }
}
