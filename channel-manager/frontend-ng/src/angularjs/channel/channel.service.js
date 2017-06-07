/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

class ChannelService {
  constructor(
    $log,
    $q,
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
    PathService,
    ) {
    'ngInject';

    this.$log = $log;
    this.$q = $q;
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

    this.isToolbarDisplayed = true;
    this.channel = {};

    this.CmsService.subscribe('channel-changed-in-extjs', () => {
      $rootScope.$apply(() => this.reload());
    });
  }

  initialize() {
    this.CmsService.subscribe('load-channel', (channel, initialPath) => this._onLoadChannel(channel, initialPath));

    // Handle reloading of iframe by BrowserSync during development
    this.CmsService.publish('reload-channel');
  }

  clearChannel() {
    this.channel = {};
  }

  hasChannel() {
    return !!this.channel.id;
  }

  getChannel() {
    return this.channel;
  }

  reload(channelId = this.channel.id) {
    return this._load(channelId, this.channel.contextPath);
  }

  _onLoadChannel(channel, initialPath) {
    this._load(channel.id, channel.contextPath)
      .then((channelId) => {
        const initialRenderPath = this.PathService.concatPaths(this.getHomePageRenderPathInfo(), initialPath);
        this.$state.go('hippo-cm.channel', { channelId, initialRenderPath }, { reload: true });
      });
  }

  switchToChannel(contextPath, id) {
    return this._load(id, contextPath)
      .then((channelId) => {
        this.CmsService.publish('switch-channel', channelId); // update breadcrumb.
      });
  }

  _load(channelId, contextPath) {
    this.ConfigService.setContextPathForChannel(contextPath);

    return this._initChannel(channelId)
      .then(channel => this._setChannel(channel));
  }

  _initChannel(channelId) {
    return this.HstService.getChannel(channelId)
      .then(channel => this.SessionService.initialize(channel.hostname, channel.mountId)
        .then(() => this._ensurePreviewHstConfigExists(channel)),
      )
      .catch((error) => {
        // TODO: improve error handling.
        // If this goes wrong, the CM won't work. display a toast explaining so
        // and switch back to the channel overview.
        this.$log.error(`Failed to load channel '${channelId}'.`, error);
        return this.$q.reject();
      });
  }

  _setChannel(channel) {
    this.channel = channel;

    // precompute channel prefix to be more efficient
    this.channelPrefix = this._makeContextPrefix(channel.contextPath);

    this.CatalogService.load(this.getMountId());
    this.SiteMapService.load(channel.siteMapId);

    if (this.SessionService.hasWriteAccess()) {
      this._augmentChannelWithPrototypeInfo();
    }

    return channel.id;
  }

  _ensurePreviewHstConfigExists(channel) {
    if (this.SessionService.hasWriteAccess() && !channel.previewHstConfigExists) {
      return this.HstService.doPost(null, channel.mountId, 'edit')
        .then(() => this.HstService.getChannel(`${channel.id}-preview`))
        .catch((error) => {
          this.$log.error(`Failed to load channel '${channel.id}'.`, error.message);
          this.FeedbackService.showError('ERROR_ENTER_EDIT');

          // initialize the app with the non-editable channel so it becomes read-only
          return this.$q.resolve(channel);
        });
    }

    // channel is already editable or the user is not allowed to edit it
    return this.$q.resolve(channel);
  }

  _makeContextPrefix(contextPath) {
    return this.PathService.concatPaths('/', contextPath, this.channel.cmsPreviewPrefix);
  }

  getPreviewPaths() {
    return this.ConfigService.contextPaths.map(path => this._makeContextPrefix(path));
  }

  makePath(renderPathInfo) {
    let path = this.channelPrefix;

    if (renderPathInfo) {
      path = this.PathService.concatPaths(path, renderPathInfo);
    }

    if (path === this.channel.contextPath) {
      // The best practice for proxy pass rules is to match on <context path>/ to delegate to the site webapp.
      // The iframe url should therefore end with '/'.
      path = this.PathService.concatPaths(path, '/');
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

  getHomePageRenderPathInfo() {
    return this.channel.mountPath ? this.channel.mountPath : '';
  }

  getName() {
    return this.channel.name;
  }

  isEditable() {
    return this.SessionService.hasWriteAccess() && this.hasChannel() && this.channel.previewHstConfigExists;
  }

  recordOwnChange() {
    const user = this.ConfigService.cmsUser;

    if (this.channel.changedBySet.indexOf(user) === -1) {
      this.channel.changedBySet.push(user);
    }

    this.CmsService.publish('channel-changed-in-angular');
  }

  publishOwnChanges() {
    return this.HstService.doPost(null, this.getMountId(), 'publish')
      .then(() => this.reload());
  }

  publishChangesOf(users) {
    const url = 'userswithchanges/publish';
    return this.HstService.doPost({ data: users }, this.getMountId(), url)
      .then(() => this.reload());
  }

  discardOwnChanges() {
    return this.HstService.doPost(null, this.getMountId(), 'discard')
      .then(() => this.reload());
  }

  discardChangesOf(users) {
    const url = 'userswithchanges/discard';
    return this.HstService.doPost({ data: users }, this.getMountId(), url)
      .then(() => this.reload());
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
      .then(response => response.data);
  }

  getChannelInfoDescription() {
    const params = { locale: this.ConfigService.locale };
    return this.HstService.doGetWithParams(this.ConfigService.rootUuid, params, 'channels', this.channel.id, 'info');
  }

  getProperties() {
    return this.channel.properties;
  }

  setProperties(properties) {
    this.channel.properties = properties;
  }

  saveChannel() {
    return this.HstService.doPut(this.channel, this.ConfigService.rootUuid, 'channels', this.channel.id);
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

  getContentRootPath() {
    return this.channel.contentRoot;
  }

  deleteChannel() {
    return this.HstService.doDelete(this.ConfigService.rootUuid, 'channels', this.getId());
  }

  setToolbarDisplayed(state) {
    this.isToolbarDisplayed = state;
  }
}

export default ChannelService;
