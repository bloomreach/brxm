/*
 * Copyright 2015-2016 Hippo B.V. (http://www.onehippo.com)
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
  constructor(SessionService) {
    'ngInject';

    this.channel = {};
    this.SessionService = SessionService;
  }

  load(channel, path = '') {
    return this.SessionService
      .authenticate(channel)
      .then(() => {
        this.channel = channel;
        this.path = path;

        return channel;
      });
  }

  getUrl() {
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

    if (this.path) {
      url += this.path;
    }

    if (url === this.channel.contextPath) {
      // The best practice for proxy pass rules is to match on <context path>/ to delegate to the site webapp.
      // The iframe url should therefore end with '/'.
      url += '/';
    }

    return url;
  }

  getMountId() {
    return this.channel.mountId;
  }

  switchToChannel(mountId) {
    console.log('Switch to channel ', mountId);
  }
}
