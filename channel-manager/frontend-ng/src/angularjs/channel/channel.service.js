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
  constructor($log, $rootScope, $http, $state, SessionService, CatalogService, HstService, ConfigService, CmsService, SiteMapService) {
    'ngInject';

    this.$log = $log;
    this.$rootScope = $rootScope;
    this.$http = $http;
    this.$state = $state;

    this.SessionService = SessionService;
    this.CatalogService = CatalogService;
    this.HstService = HstService;
    this.ConfigService = ConfigService;
    this.CmsService = CmsService;
    this.SiteMapService = SiteMapService;

    this.channel = {};

    this.CmsService.subscribe('channel-changed-in-extjs', this._onChannelChanged, this);
  }

  _onChannelChanged() {
    this.$rootScope.$apply(() => {
      this.HstService.getChannel(this.channel.id)
        .then((channel) => {
          this._setChannel(channel);
        })
        .catch(() => {
          this.$log.error(`Cannot retrieve properties of the channel with id = "${this.channel.id}" from server`);
        });
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
    this.channelPrefix = this._makeChannelPrefix(); // precompute to be more efficient
    this.CatalogService.load(this._getMountId());
    this.SiteMapService.load(channel.siteMapId);
    this._augmentChannelWithPrototypeInfo();
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

  makeContextPrefix(contextPath) {
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
    return this.ConfigService.contextPaths.map((path) => this.makeContextPrefix(path));
  }

  _makeChannelPrefix() {
    let prefix = this.makeContextPrefix(this.channel.contextPath);

    if (this.channel.mountPath) {
      prefix += this.channel.mountPath;
    }

    return prefix;
  }

  makePath(renderPathInfo) {
    let path = this.channelPrefix;

    if (renderPathInfo) {
      path += renderPathInfo;
    }

    if (path === this.channel.contextPath) {
      // The best practice for proxy pass rules is to match on <context path>/ to delegate to the site webapp.
      // The iframe url should therefore end with '/'.
      path += '/';
    }

    return path;
  }

  extractRenderPathInfo(path) {
    if (!path.startsWith(this.channelPrefix)) {
      this.$log.warn(`Current path '${path}' does not match current channel's path prefix '${this.channelPrefix}'.`);
      return path;
    }

    let renderPathInfo = path.slice(this.channelPrefix.length);

    // remove trailing slash if any, HST's siteMapItem.renderPathInfo never has a trailing slash.
    if (renderPathInfo.endsWith('/')) {
      renderPathInfo = renderPathInfo.slice(0, renderPathInfo.length - 1);
    }

    return renderPathInfo;
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

  getSiteMapId() {
    return this.channel.siteMapId;
  }

  _augmentChannelWithPrototypeInfo() {
    this.getNewPageModel()
      .then((data) => {
        this.channel.hasPrototypes = data.prototypes && data.prototypes.length > 0;
      });
  }

  hasPrototypes() {
    return this.channel.hasPrototypes;
  }

  hasWorkspace() {
    return this.channel.workspaceExists;
  }

  getNewPageModel() {
    return this.HstService.doGet(this._getMountId(), 'newpagemodel')
      .then((response) => response.data);
  }

  _getMountId() {
    return this.channel.mountId;
  }

  _resetOwnChange() {
    this.channel.changedBySet.splice(this.channel.changedBySet.indexOf(this.ConfigService.cmsUser), 1);
    this.CmsService.publish('channel-changed-in-angular');
  }
}
