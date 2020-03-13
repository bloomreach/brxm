/*
 * Copyright 2016-2020 Hippo B.V. (http://www.onehippo.com)
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
    $log,
    $q,
    $rootScope,
    ChannelService,
    CmsService,
    ConfigService,
    DomService,
    PageStructureService,
    PageToolsService,
    ProjectService,
    ScrollService,
  ) {
    'ngInject';

    this.$log = $log;
    this.$q = $q;
    this.$rootScope = $rootScope;

    this.ChannelService = ChannelService;
    this.CmsService = CmsService;
    this.ConfigService = ConfigService;
    this.DomService = DomService;
    this.PageStructureService = PageStructureService;
    this.PageToolsService = PageToolsService;
    this.ProjectService = ProjectService;
    this.ScrollService = ScrollService;

    this._onPageChange = this._onPageChange.bind(this);
  }

  initialize(hippoIframeJQueryElement, iframeJQueryElement) {
    this.hippoIframeJQueryElement = hippoIframeJQueryElement;
    this.iframeJQueryElement = iframeJQueryElement;
    this.pageLoaded = false;

    // Reloads the current page when the project changes so new data will be shown.
    // When another project became active the page reload will trigger a channel switch.
    this.ProjectService.afterChange('iframeReload', (projectIdIdentical) => {
      if (!projectIdIdentical) {
        this.reload();
      }
    });

    if (this._offPageChange) {
      this._offPageChange();
    }
    this._offPageChange = this.$rootScope.$on('page:change', this._onPageChange);
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
    return this.pageLoaded;
  }

  load(renderPathInfo) {
    const targetPath = this.ChannelService.makePath(renderPathInfo);

    this.pageLoaded = false;

    if (targetPath !== this.src) {
      // setting the src attribute of the iframe makes us go to the desired page.
      this.src = targetPath;
    } else if (this.iframeJQueryElement /* pro-forma check */) {
      // we may have set the src attribute of the iframe to the targetPath before,
      // but then navigated away from that location by following site-internal links.
      // In order to get back to the location that already matches the iframe's src attribute
      // value, we use jQuery's attr() method, which triggers a load of the specified src.
      this.iframeJQueryElement.attr('src', this.src);
    }
    return this.$q.resolve();
  }

  getSrc() {
    return this.src;
  }

  getCurrentRenderPathInfo() {
    return this.renderPathInfo;
  }

  reload() {
    if (!this.pageLoaded) {
      return this.$q.resolve();
    }

    if (this.deferredReload) {
      this.$log.warn('Trying to reload when a reload is already ongoing. Taking no action.');
      return this.deferredReload.promise;
    }

    this.ScrollService.savePosition();

    this.deferredReload = this.$q.defer();
    this.iframeJQueryElement[0].contentWindow.location.reload();

    return this.deferredReload.promise;
  }

  _onPageChange(event, data) {
    if (!data || !data.initial) {
      return;
    }

    this.renderPathInfo = this._determineRenderPathInfo();
    this.$rootScope.$apply(() => {
      this.pageLoaded = true;
    });

    this.ScrollService.restorePosition();
    this.PageToolsService.updatePageTools();

    const deferred = this.deferredReload;
    if (deferred) {
      // delete the "state" before resolving the promise.
      delete this.deferredReload;
      deferred.resolve();
    }

    this.CmsService.publish('user-activity');

    if (this.ConfigService.isDevMode()) {
      sessionStorage.channelPath = this.renderPathInfo;
    }
  }

  _determineRenderPathInfo() {
    try {
      const loadedPath = this.iframeJQueryElement[0].contentWindow.location.pathname;
      return this.ChannelService.extractRenderPathInfo(loadedPath);
    } catch (ignoredError) {
      // if pathname is not found, reset renderPathInfo
      return undefined;
    }
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

  getAssetUrl(href) {
    return this.DomService.getAssetUrl(`${href}?antiCache=${this.ConfigService.antiCache}`);
  }
}

export default HippoIframeService;
