/*
 * Copyright 2016-2021 Hippo B.V. (http://www.onehippo.com)
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

class HippoIframeService {
  constructor(
    $http,
    $log,
    $q,
    $rootScope,
    ChannelService,
    CmsService,
    CommunicationService,
    ConfigService,
    DomService,
    PageStructureService,
    PageToolsService,
    ProjectService,
    ScrollService,
  ) {
    'ngInject';

    this.$http = $http;
    this.$log = $log;
    this.$q = $q;
    this.$rootScope = $rootScope;

    this.ChannelService = ChannelService;
    this.CmsService = CmsService;
    this.CommunicationService = CommunicationService;
    this.ConfigService = ConfigService;
    this.DomService = DomService;
    this.PageStructureService = PageStructureService;
    this.PageToolsService = PageToolsService;
    this.ProjectService = ProjectService;
    this.ScrollService = ScrollService;

    this._onPageChange = this._onPageChange.bind(this);
    this._onEditSharedContainers = this._onEditSharedContainers.bind(this);
    this._editSharedContainers = false;
  }

  initialize(hippoIframeJQueryElement, iframeJQueryElement) {
    this.hippoIframeJQueryElement = hippoIframeJQueryElement;
    this.iframeJQueryElement = iframeJQueryElement;
    this._deferredPageLoad = this.$q.defer();

    // Reloads the current page when the project changes so new data will be shown.
    // When another project became active the page reload will trigger a channel switch.
    this.ProjectService.afterChange('iframeReload', (projectIdIdentical) => {
      if (!projectIdIdentical) {
        this.reload(true);
      }
    });

    if (this._offPageChange) {
      this._offPageChange();
    }
    this._offPageChange = this.$rootScope.$on('page:change', this._onPageChange);

    if (this._offEditSharedContainers) {
      this._offEditSharedContainers();
    }
    this._offEditSharedContainers = this.$rootScope.$on('iframe:page:edit-shared-containers',
      this._onEditSharedContainers);
  }

  initializePath(channelRelativePath) {
    const initialRenderPath = this.ChannelService.makeRenderPath(channelRelativePath);

    if (angular.isString(channelRelativePath) // a null path means: reuse the current render path
      && this._isDifferentPage(initialRenderPath)) {
      return this.load(initialRenderPath);
    }
    return this.reload();
  }

  _isDifferentPage(renderPath) {
    return this.renderPathInfo !== renderPath || this._isDifferentContextPath();
  }

  _isDifferentContextPath() {
    const page = this.PageStructureService.getPage();

    return !page || page.getMeta().getContextPath() !== this.ChannelService.getChannel().contextPath;
  }

  isPageLoaded() {
    return this._deferredPageLoad === undefined;
  }

  load(renderPathInfo) {
    const targetPath = this.ChannelService.makePath(renderPathInfo);

    this._deferredPageLoad = this.$q.defer();

    if (targetPath !== this.src) {
      this.src = targetPath;
      this.renderPathInfo = renderPathInfo;
    } else if (this.iframeJQueryElement /* pro-forma check */) {
      // we may have set the src attribute of the iframe to the targetPath before,
      // but then navigated away from that location by following site-internal links.
      // In order to get back to the location that already matches the iframe's src attribute
      // value, we use jQuery's attr() method, which triggers a load of the specified src.
      this.iframeJQueryElement.attr('src', this.src);
    }
    return this._deferredPageLoad.promise;
  }

  getSrc() {
    return this.src;
  }

  getCurrentRenderPathInfo() {
    return this.renderPathInfo;
  }

  getCurrentPathInfo() {
    const page = this.PageStructureService.getPage();
    const meta = page && page.getMeta();
    return meta && meta.getPathInfo();
  }

  async reload(force = false) {
    if (!this.isPageLoaded()) {
      this.$log.warn('Trying to reload when a page load is already ongoing. Taking no action.');
      return this._deferredPageLoad.promise;
    }

    this._deferredPageLoad = this.$q.defer();

    await this.ScrollService.savePosition();
    if (force) {
      this.iframeJQueryElement.attr('src', this.ChannelService.makePath(this.getCurrentRenderPathInfo()));
    } else {
      await this.CommunicationService.reload();
    }

    return this._deferredPageLoad.promise;
  }

  async _onPageChange(event, data) {
    if (!data || !data.initial) {
      return;
    }

    this.ScrollService.restorePosition();
    this.PageToolsService.updatePageTools();

    this.$rootScope.$apply(() => {
      // delete the "state" before resolving the promise.
      const deferred = this._deferredPageLoad;
      if (deferred) {
        delete this._deferredPageLoad;
        deferred.resolve();
      }
    });

    this.CmsService.publish('user-activity');

    const pathInfo = this.PageStructureService.getPage().getMeta().getPathInfo();

    const renderPathInfo = this.getRenderPathByPathInfo(pathInfo);

    if (renderPathInfo !== this.renderPathInfo) {
      this.$rootScope.$evalAsync(() => { this.renderPathInfo = renderPathInfo; });
      this._editSharedContainers = false;
    }

    if (this.ConfigService.isDevMode()) {
      sessionStorage.channelPath = pathInfo;
    }
  }

  _onEditSharedContainers(event, data) {
    this.$rootScope.$apply(() => {
      this._editSharedContainers = data;
    });
  }

  liftIframeAboveMask() {
    this.isIframeLifted = true;
  }

  lowerIframeBeneathMask() {
    this.isIframeLifted = false;
  }

  lockWidth() {
    const hippoIframeWidth = this.hippoIframeJQueryElement.outerWidth();
    this.hippoIframeJQueryElement[0].style.setProperty('--locked-width', `${hippoIframeWidth}px`);
  }

  async getAsset(href) {
    const url = this.getAssetUrl(href);
    const { data } = await this.$http.get(url);

    return data;
  }

  getAssetUrl(href) {
    return this.DomService.getAssetUrl(`${href}?antiCache=${this.ConfigService.antiCache}`);
  }

  isEditSharedContainers() {
    return this._editSharedContainers;
  }

  getRenderPathByPathInfo(pathInfo) {
    const mountPath = this.ChannelService.getHomePageRenderPathInfo();

    let renderPathInfo = `${mountPath}${pathInfo}`;
    if (renderPathInfo !== '/' && renderPathInfo.endsWith('/')) {
      renderPathInfo = renderPathInfo.slice(0, renderPathInfo.length - 1);
    }
    return renderPathInfo;
  }
}

export default HippoIframeService;
