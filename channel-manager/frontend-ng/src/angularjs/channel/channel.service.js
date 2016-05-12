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
  constructor(
      $log,
      $rootScope,
      $http,
      $state,
      SessionService,
      CatalogService,
      FeedbackService,
      HstService,
      ConfigService,
      CmsService,
      SiteMapService,
      PathService
    ) {
    'ngInject';

    this.$log = $log;
    this.$rootScope = $rootScope;
    this.$http = $http;
    this.$state = $state;
    this.SessionService = SessionService;
    this.CatalogService = CatalogService;
    this.FeedbackService = FeedbackService;
    this.HstService = HstService;
    this.ConfigService = ConfigService;
    this.CmsService = CmsService;
    this.SiteMapService = SiteMapService;
    this.PathService = PathService;

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
    this.CmsService.subscribe('load-channel', (channel, initialPath) => {
      this.HstService.getChannel(channel.id).then((updatedChannel) => {
        this._load(updatedChannel).then((channelId) => {
          this.$state.go('hippo-cm.channel', { channelId, initialPath }, { reload: true });
        });
      });
      // TODO: handle error.
      // If this goes wrong, the CM won't work. display a toast explaining so
      // and switch back to the channel overview.

      this._loadGlobalFeatures();
    });

    // Handle reloading of iframe by BrowserSync during development
    this.CmsService.publish('reload-channel');
  }

  _loadGlobalFeatures() {
    this.HstService.doGet(this.ConfigService.rootUuid, 'features')
      .then((response) => {
        this.crossChannelPageCopySupported = response.data.crossChannelPageCopySupported;
      });
  }

  _setChannel(channel) {
    this.channel = channel;

    // precompute channel prefix to be more efficient
    const contextPrefix = this._makeContextPrefix(channel.contextPath);
    this.channelPrefix = this.PathService.concatPaths(contextPrefix, this.channel.mountPath);

    this.CatalogService.load(this.getMountId());
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

  _makeContextPrefix(contextPath) {
    let path = contextPath;
    if (this.channel.cmsPreviewPrefix) {
      path = this.PathService.concatPaths(path, this.channel.cmsPreviewPrefix);
    }
    return path;
  }

  getPreviewPaths() {
    return this.ConfigService.contextPaths.map((path) => this._makeContextPrefix(path));
  }

  makePath(renderPathInfo) {
    let path = this.channelPrefix;

    if (renderPathInfo) {
      path = this.PathService.concatPaths(path, renderPathInfo);
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

  getName() {
    return this.channel.name;
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
    return this.HstService.doPost(null, this.getMountId(), 'edit')
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

  publishChanges(users = [this.ConfigService.cmsUser]) {
    const url = 'userswithchanges/publish';
    return this.HstService.doPost({ data: users }, this.getMountId(), url)
      .then(() => this.resetUserChanges(users))
      .catch(() => this.FeedbackService.showError('ERROR_PUBLISH_CHANGES'));
  }

  discardChanges(users = [this.ConfigService.cmsUser]) {
    const url = 'userswithchanges/discard';
    return this.HstService.doPost({ data: users }, this.getMountId(), url)
      .then(() => this.resetUserChanges(users))
      .catch(() => this.FeedbackService.showError('ERROR_DISCARD_CHANGES'));
  }

  resetUserChanges(users) {
    users.forEach((user) => {
      this.channel.changedBySet.splice(this.channel.changedBySet.indexOf(user), 1);
    });
  }

  getSiteMapId() {
    return this.channel.siteMapId;
  }

  _augmentChannelWithPrototypeInfo() {
    this.getNewPageModel()
      .then((data) => {
        this._hasPrototypes = data.prototypes && data.prototypes.length > 0;
      });
  }

  hasPrototypes() {
    return this._hasPrototypes;
  }

  hasWorkspace() {
    return this.channel.workspaceExists;
  }

  getNewPageModel(mountId) {
    const params = mountId ? { mountId } : undefined;
    return this.HstService.doGetWithParams(this.getMountId(), params, 'newpagemodel')
      .then((response) => response.data);
  }

  getChannelInfoDescription() {
    const params = { locale: this.ConfigService.locale };
    return this.HstService.doGetWithParams(this.ConfigService.rootUuid, params, 'channels', this.channel.id, 'info');
  }

  saveChannel() {
    return this.HstService.doPut(this.channel, this.ConfigService.rootUuid, 'channels', this.channel.id);
  }

  isCrossChannelPageCopySupported() {
    return this.crossChannelPageCopySupported;
  }

  loadPageModifiableChannels() {
    const params = {
      previewConfigRequired: true,
      workspaceRequired: true,
    };
    this.HstService.doGetWithParams(this.ConfigService.rootUuid, params, 'channels')
      .then((response) => {
        this.pageModifiableChannels = response.data || [];
      });
  }

  getPageModifiableChannels() {
    return this.pageModifiableChannels;
  }

  getMountId() {
    return this.channel.mountId;
  }
}
