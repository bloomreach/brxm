/*
 * Copyright 2016-2022 Hippo B.V. (http://www.onehippo.com)
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

const PROPERTY_URL = 'url';
const PROPERTY_SPA_URL = 'spaUrl';

class ChannelService {
  constructor(
    $log,
    $q,
    $state,
    $rootScope,
    $window,
    CatalogService,
    CmsService,
    ConfigService,
    NavappService,
    FeedbackService,
    HstService,
    PathService,
    ProjectService,
    RightSidePanelService,
    SessionService,
    SiteMapService,
  ) {
    'ngInject';

    this.$log = $log;
    this.$q = $q;
    this.$state = $state;
    this.$rootScope = $rootScope;
    this.$window = $window;
    this.CatalogService = CatalogService;
    this.CmsService = CmsService;
    this.ConfigService = ConfigService;
    this.NavappService = NavappService;
    this.FeedbackService = FeedbackService;
    this.HstService = HstService;
    this.PathService = PathService;
    this.ProjectService = ProjectService;
    this.RightSidePanelService = RightSidePanelService;
    this.SessionService = SessionService;
    this.SiteMapService = SiteMapService;

    this.isToolbarDisplayed = true;
    this.channel = {};

    this.NavappService.subscribe('navigate', (location, triggeredBy) => this.navigate(location, triggeredBy));
    this.NavappService.subscribe('beforeNavigation', () => this._beforeNavigation());
    this.NavappService.subscribe('beforeLogout', () => this._beforeLogout());

    this.CmsService.subscribe('activate-experience-manager', () => {
      // Reload the left side-panel contents when the experience-manager perspective is activated.
      // This ensures possible changes from other perspectives are displayed correctly.
      if (this.channel && this.channel.id) {
        this.CatalogService.load(this.getMountId());
        this.$rootScope.$emit('load-site-map');
      }
    });
  }

  /**
   * Loads a channel. When the channel to load does not have a preview configuration yet it will be
   * created. Note that a branched channel (e.g. with a branch ID in the channel ID) will always have a preview
   * configuration already.
   *
   * @param channelId the ID of the channel to load.
   * @param contextPath the context path of the web application that contains the channel
   * @param hostGroup the host group of the channel
   * @param branchId the ID of the channel branch to show. Defaults to the active project.
   * @param shouldLoadSiteMap whether to load the site map after loading the channel. Defaults to true.
   * @returns {*}
   */
  async initializeChannel(channelId, contextPath, hostGroup, branchId, shouldLoadSiteMap = true) {
    try {
      await this.SessionService.initializeContext(contextPath);

      const channel = await this.HstService.getChannel(channelId, contextPath, hostGroup);
      await this.SessionService.initializeState(channel);

      const previewChannel = await this._ensurePreviewHstConfigExists(channel);
      await this._loadProject(channel, branchId || this.ProjectService.selectedProject.id);
      this._setChannel(previewChannel);

      if (shouldLoadSiteMap) {
        this.$rootScope.$emit('load-site-map');
      }
    } catch (error) {
      if (this.hasChannel()) {
        // restore the session for the previous channel, but still reject the promise chain
        await this.SessionService.initializeState(this.channel);
      }

      throw error;
    }
  }

  async _ensurePreviewHstConfigExists(channel) {
    if (!this.SessionService.canWriteHstConfig() || channel.previewHstConfigExists) {
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

    this.updateNavLocation();
  }

  _makeContextPrefix(contextPath) {
    return this.PathService.concatPaths('/', contextPath, this.channel.cmsPreviewPrefix);
  }

  _beforeNavigation() {
    return this.RightSidePanelService.close()
      .then(() => true, () => false);
  }

  _beforeLogout() {
    return this.RightSidePanelService.close()
      .then(() => null);
  }

  clearChannel() {
    this.channel = {};
    this.ProjectService.afterChangeListeners.delete('iframeReload');

    if (!this.isToolbarDisplayed) {
      this.setToolbarDisplayed(true);
    }
    this.updateNavLocation();
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

  getId() {
    return this.channel.id;
  }

  getBaseId() {
    if (this.channel.branchOf) {
      return this.channel.branchOf;
    }

    if (this.channel.preview && this.channel.id.endsWith('-preview')) {
      return this.channel.id.slice(0, -('-preview'.length));
    }

    return this.channel.id;
  }

  getHomePageRenderPathInfo() {
    return this.channel.mountPath ? this.channel.mountPath : '';
  }

  getName() {
    return this.channel.name;
  }

  isEditable() {
    return this.SessionService.canWriteHstConfig() && this.hasChannel() && this.channel.previewHstConfigExists;
  }

  isConfigurationLocked() {
    return this.getChannel().configurationLocked;
  }

  async checkChanges() {
    this.$rootScope.$emit('page:check-changes');

    try {
      const { data: changedSet } = await this.HstService.doGet(this.getMountId(), 'mychanges');
      if (changedSet.length > 0) {
        this.recordOwnChange();
      }
    // eslint-disable-next-line no-empty
    } catch (ignore) {}
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

  setExternalPreviewProperties(externalPreviewEnabled, externalPreviewToken) {
    this.channel.externalPreviewEnabled = externalPreviewEnabled;
    this.channel.externalPreviewToken = externalPreviewToken;
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
      privilegeAllowed: 'hippo:channel-webmaster',
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
    return this.HstService
      .doDelete(this.ConfigService.rootUuid, 'channels', this.getId())
      .then(() => this.updateNavLocation());
  }

  setToolbarDisplayed(state) {
    this.isToolbarDisplayed = state;
  }

  navigate(location) {
    if (location.path === '') {
      return this.$state.go('hippo-cm')
        .then(() => {
          this.clearChannel();
          this.CmsService.publish('close-channel');
        });
    }

    return this.updateNavLocation();
  }

  updateNavLocation() {
    const location = this._getLocation();
    return this.NavappService.updateNavLocation(location);
  }

  _getLocation() {
    if (!this.hasChannel()) {
      return {
        path: 'experience-manager',
        addHistory: true,
      };
    }

    return {
      breadcrumbLabel: this.getName(),
      path: `experience-manager/${this.getId()}`,
      addHistory: true,
    };
  }

  getOrigin() {
    const channel = this.getChannel();
    const url = channel && (channel[PROPERTY_SPA_URL] || channel[PROPERTY_URL]);
    if (!url) {
      return;
    }

    try {
      const { origin } = new URL(url, this.$window.location.origin);

      // eslint-disable-next-line consistent-return
      return origin;
    } catch (error) {
      this.$log.error(`Invalid url for '${channel.id}'.`, error.message);
    }
  }

  getBaseDeliveryApiURL() {
    return this.getUrlWithPort(this.channel.url.concat('/resourceapi'));
  }

  getDeliveryApiPreviewURL(externalPreviewToken) {
    const endpointUrlParameter = this.getEndpointUrlParameter();
    if (endpointUrlParameter) {
      return endpointUrlParameter.concat('?preview-token=').concat(externalPreviewToken);
    }
    return this.getBaseDeliveryApiURL().concat('?preview-token=').concat(externalPreviewToken);
  }

  getUrlWithPort(baseUrl) {
    if (window.location.port !== "" && !baseUrl.includes(window.location.port)) {
      const url = new URL(baseUrl);
      return baseUrl.replace(url.host, window.location.hostname
        .concat(':').concat(window.location.port));
    }
    return baseUrl;
  }

  getEndpointUrlParameter() {
    if (this.channel.spaUrl) {
      const url = new URL(this.channel.spaUrl);
      return url.searchParams.get('endpoint');
    }
    return null;
  }

  getProjectChannelPreviewURL(externalPreviewToken) {
    if (this.channel.spaUrl) {
      if (this.getEndpointUrlParameter()) {
        return this.channel.spaUrl.concat('&token=').concat(externalPreviewToken);
      }
      return this.channel.spaUrl.concat('?token=').concat(externalPreviewToken)
        .concat('&endpoint=').concat(this.getBaseDeliveryApiURL());
    }
    return this.getUrlWithPort(this.channel.url).concat('?preview-token=').concat(externalPreviewToken);
  }
}

export default ChannelService;
