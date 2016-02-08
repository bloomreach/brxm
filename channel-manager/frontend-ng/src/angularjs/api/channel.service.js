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

export class ChannelService {
  constructor($http, SessionService, CmsService, ConfigService) {
    'ngInject';

    this.$http = $http;
    this.SessionService = SessionService;
    this.CmsService = CmsService;
    this.ConfigService = ConfigService;

    this.channel = {};
  }

  load(channel) {
    return this.SessionService
      .authenticate(channel)
      .then(() => {
        this.channel = channel;

        return channel;
      });
  }

  getUrlForPath(path) {
    let url = this.channel.contextPath;
    if (url === '/') {
      url = '';
    }

    if (this.channel.cmsPreviewPrefix) {
      url += '/' + this.channel.cmsPreviewPrefix;
    }

    if (this.channel.mountPath) {
      url += this.channel.mountPath;
    }

    if (path) {
      url += path;
    }

    if (url === this.channel.contextPath) {
      // The best practice for proxy pass rules is to match on <context path>/ to delegate to the site webapp.
      // The iframe url should therefore end with '/'.
      url += '/';
    }

    return url;
  }

  getId() {
    return this.channel.id;
  }

  switchToChannel(id) {
    const url = this.channel.contextPath
      + this.ConfigService.apiUrlPrefix
      + this.ConfigService.rootResource
      + '/channels/' + id;
    const headers = {
      FORCE_CLIENT_HOST: 'true',
    };

    this.$http.get(url, { headers })
      .success((channel) => {
        this.channel = channel;
        this.CmsService.publish('switch-channel', channel.id); // update breadcrumb.
      }); // TODO add error handling
  }
}
