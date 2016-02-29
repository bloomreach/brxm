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
  constructor($http, SessionService, HstService, CmsService, ConfigService) {
    'ngInject';

    this.$http = $http;
    this.SessionService = SessionService;
    this.HstService = HstService;
    this.CmsService = CmsService;
    this.ConfigService = ConfigService;

    this.channel = {};
  }

  _setChannel(channel) {
    this.channel = channel;
    this.HstService.setContextPath(channel.contextPath);
  }

  getChannel() {
    return this.channel;
  }

  load(channel) {
    return this.SessionService
      .initialize(channel)
      .then(() => {
        this._setChannel(channel);
        return channel;
      });
  }

  getUrl(path) {
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
    this.HstService.getChannel(id)
      .then((channel) => {
        this._setChannel(channel);
        this.CmsService.publish('switch-channel', channel.id); // update breadcrumb.
      });// TODO add error handling
  }
}
