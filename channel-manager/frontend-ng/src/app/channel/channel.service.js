/*
 * Copyright 2016-2019 Hippo B.V. (http://www.onehippo.com)
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
    CatalogService,
    CmsService,
    ConfigService,
    FeedbackService,
    HstService,
    PathService,
    ProjectService,
    SessionService,
    SiteMapService,
  ) {
    'ngInject';

    this.$log = $log;
    this.$q = $q;
    this.$rootScope = $rootScope;
    this.CatalogService = CatalogService;
    this.CmsService = CmsService;
    this.ConfigService = ConfigService;
    this.FeedbackService = FeedbackService;
    this.HstService = HstService;
    this.PathService = PathService;
    this.ProjectService = ProjectService;
    this.SessionService = SessionService;
    this.SiteMapService = SiteMapService;

    this.isToolbarDisplayed = true;
    this.channel = {};
  }

  /**
   * Loads a channel. When the channel to load does not have a preview configuration yet it will be
   * created. Note that a branched channel (e.g. with a branch ID in the channel ID) will always have a preview
   * configuration already.
   *
   * @param channelId the ID of the channel to load.
   * @param contextPath the context path of the web application that contains the channel
   * @param branchId the ID of the channel branch to show. Defaults to the active project.
   * @returns {*}
   */
  async initializeChannel(channelId, contextPath, hostGroup, branchId) {
    try {
      const channel = await this.HstService.getChannel(channelId, contextPath, hostGroup);
      await this.SessionService.initialize(channel);

      const previewChannel = await this._ensurePreviewHstConfigExists(channel);
      await this._loadProject(channel, branchId || this.ProjectService.selectedProject.id);
      this._setChannel(previewChannel);
    } catch (error) {
      if (this.hasChannel()) {
        // restore the session for the previous channel, but still reject the promise chain
        await this.SessionService.initialize(this.channel);
      }

      throw error;
    }
  }

  async _ensurePreviewHstConfigExists(channel) {
    if (!this.SessionService.hasWriteAccess() || channel.previewHstConfigExists) {
      // channel is already editable or the user is not allowed to edit it
      return channel;
    }

    try {
      await this.HstService.doPost(null, channel.mountId, 'edit');

      return await this._getPreviewChannel(channel);
    } catch (error) {
      this.$log.error(`Failed to load channel '${channel.id}'.`, error.message);
      this.FeedbackService.showErrorResponse(error.data, 'ERROR_ENTER_EDIT');

      throw error;
    }
  }

  async _getPreviewChannel(channel) {
    if (channel.preview) {
      return channel;
    }

    return this.HstService.getChannel(`${channel.id}-preview`, channel.contextPath, channel.hostGroup);
  }

  async _loadProject(channel, branchId) { // eslint-disable-line consistent-return
    if (this.ConfigService.projectsEnabled) {
      return this.ProjectService.load(channel.mountId, branchId);
    }
  }

  _setChannel(channel) {
    this.channel = channel;

    // precompute channel prefix to be more efficient
    this.channelPrefix = this._makeContextPrefix(channel.contextPath);

    this.CatalogService.load(this.getMountId());
    this.SiteMapService.load(this.getSiteMapId());

    if (this.SessionService.hasWriteAccess()) {
      this._augmentChannelWithPrototypeInfo();
    }

    this.CmsService.publish('set-breadcrumb', this.channel.name);
  }

  _makeContextPrefix(contextPath) {
    return this.PathService.concatPaths('/', contextPath, this.channel.cmsPreviewPrefix);
  }

  clearChannel() {
    this.channel = {};
    this.ProjectService.afterChangeListeners.delete('iframeReload');

    if (!this.isToolbarDisplayed) {
      this.setToolbarDisplayed(true);
    }
  }

  hasChannel() {
    return !!this.channel.id;
  }

  matchesChannel(channelId) {
    return this.hasChannel() && this.channel.id === channelId;
  }

  getChannel() {
    return this.channel;
  }

  async reload() {
    return this.initializeChannel(
      this.channel.id,
      this.channel.contextPath,
      this.channel.hostGroup,
      this.channel.branchId,
    );
  }

  makeRenderPath(channelRelativePath) {
    const path = this.PathService.concatPaths(this.getHomePageRenderPathInfo(), channelRelativePath);

    // let the HST know the host name of the rendered page via an internal query parameter
    return `${path}?org.hippoecm.hst.container.render_host=${this.channel.hostname}`;
  }

  makePath(renderPath) {
    let path = this.channelPrefix;

    if (renderPath) {
      path = this.PathService.concatPaths(path, renderPath);
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

  isConfigurationLocked() {
    return this.getChannel().configurationLocked;
  }

  recordOwnChange() {
    const user = this.ConfigService.cmsUser;

    if (this.channel.changedBySet.indexOf(user) === -1) {
      this.channel.changedBySet.push(user);
    }

    this.CmsService.publish('channel-changed-in-angular');
  }

  async publishOwnChanges() {
    await this.HstService.doPost(null, this.getMountId(), 'publish');
    await this.reload();
    this.$rootScope.$broadcast('channel:changes:publish');
  }

  async publishChangesOf(users) {
    await this.HstService.doPost({ data: users }, this.getMountId(), 'userswithchanges/publish');
    await this.reload();
    this.$rootScope.$broadcast('channel:changes:publish');
  }

  async discardOwnChanges() {
    await this.HstService.doPost(null, this.getMountId(), 'discard');
    await this.reload();
    this.$rootScope.$broadcast('channel:changes:discard');
  }

  async discardChangesOf(users) {
    await this.HstService.doPost({ data: users }, this.getMountId(), 'userswithchanges/discard');
    await this.reload();
    this.$rootScope.$broadcast('channel:changes:discard');
  }

  getSiteMapId() {
    return this.channel.siteMapId;
  }

  async _augmentChannelWithPrototypeInfo() {
    const data = await this.getNewPageModel();

    this._hasPrototypes = data.prototypes && data.prototypes.length > 0;
  }

  hasPrototypes() {
    return this._hasPrototypes;
  }

  hasWorkspace() {
    return this.channel.workspaceExists;
  }

  async getNewPageModel(mountId) {
    const params = mountId ? { mountId } : undefined;
    const response = await this.HstService.doGetWithParams(this.getMountId(), params, 'newpagemodel');

    return response.data;
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

  async loadPageModifiableChannels() {
    const params = {
      previewConfigRequired: true,
      workspaceRequired: true,
      skipBranches: true,
      skipConfigurationLocked: true,
    };

    const response = await this.HstService.doGetWithParams(this.ConfigService.rootUuid, params, 'channels');
    this.pageModifiableChannels = response.data || [];
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

  getHostGroup() {
    return this.channel.hostGroup;
  }

  deleteChannel() {
    return this.HstService.doDelete(this.ConfigService.rootUuid, 'channels', this.getId());
  }

  setToolbarDisplayed(state) {
    this.isToolbarDisplayed = state;
  }
}

export default ChannelService;
