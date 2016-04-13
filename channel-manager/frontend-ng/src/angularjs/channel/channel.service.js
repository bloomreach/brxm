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
  constructor($rootScope, $http, $state, SessionService, CatalogService, HstService, ConfigService, CmsService, ChannelSiteMapService) {
    'ngInject';

    this.$rootScope = $rootScope;
    this.$http = $http;
    this.$state = $state;

    this.SessionService = SessionService;
    this.CatalogService = CatalogService;
    this.HstService = HstService;
    this.ConfigService = ConfigService;
    this.CmsService = CmsService;
    this.ChannelSiteMapService = ChannelSiteMapService;

    this.channel = {};

    this.CmsService.subscribe('channel-changed-in-extjs', this._onChannelChanged, this);
  }

  _onChannelChanged(channel) {
    this.$rootScope.$apply(() => {
      this.channel = channel;
    });
  }

  initialize() {
    this.CmsService.subscribe('load-channel', (channel) => {
      this.HstService.getChannel(channel.id).then((updatedChannel) => {
        this._load(updatedChannel).then((channelId) => {
          this.$state.go('hippo-cm.channel', { channelId }, { reload: true });
        });
      });
      // TODO: handle error.
      // If this goes wrong, the CM won't work. display a toast explaining so
      // and switch back to the channel overview.
    });

    // Handle reloading of iframe by BrowserSync during development
    this.CmsService.publish('reload-channel');
  }

  _setChannel(channel) {
    this.channel = channel;
    this.CatalogService.load(this._getMountId());
    this.ChannelSiteMapService.load(channel.siteMapId);
  }

  getChannel() {
    return this.channel;
  }

  _load(channel) {
    return this.SessionService
      .initialize(channel)
      .then(() => {
        this._setChannel(channel);
        return channel.id;
      });
  }

  getPreviewPath(contextPath) {
    let path = contextPath;
    if (path === '/') {
      path = '';
    }

    if (this.channel.cmsPreviewPrefix) {
      path += `/${this.channel.cmsPreviewPrefix}`;
    }
    return path;
  }

  getPreviewPaths() {
    return this.ConfigService.contextPaths.map((path) => this.getPreviewPath(path));
  }

  getUrl(path) {
    let url = this.getPreviewPath(this.channel.contextPath);

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
    return this.HstService.getChannel(id)
      .then((channel) => {
        this._setChannel(channel);
        this.CmsService.publish('switch-channel', channel.id); // update breadcrumb.
      });
  }

  hasPreviewConfiguration() {
    return this.channel.previewHstConfigExists === true;
  }

  createPreviewConfiguration() {
    return this.HstService.doPost(null, this._getMountId(), 'edit')
      .then(() => {
        this.channel.previewHstConfigExists = true;
      });
  }

  getCatalog() {
    return this.CatalogService.getComponents();
  }

  recordOwnChange() {
    const user = this.ConfigService.cmsUser;

    if (this.channel.changedBySet.indexOf(user) === -1) {
      this.channel.changedBySet.push(user);
    }

    this.CmsService.publish('channel-changed-in-angular');
  }

  publishOwnChanges() {
    return this.HstService.doPost(null, this._getMountId(), 'publish')
      .then((response) => {
        this._resetOwnChange();
        return response;
      });
  }

  discardOwnChanges() {
    return this.HstService.doPost(null, this._getMountId(), 'discard')
      .then((response) => {
        this._resetOwnChange();
        return response;
      });
  }

  _getMountId() {
    return this.channel.mountId;
  }

  _resetOwnChange() {
    this.channel.changedBySet.splice(this.channel.changedBySet.indexOf(this.ConfigService.cmsUser), 1);
    this.CmsService.publish('channel-changed-in-angular');
  }
}
