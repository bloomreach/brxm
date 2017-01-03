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

class HippoIframeService {

  constructor($q, $log, ChannelService, CmsService) {
    'ngInject';

    this.$q = $q;
    this.$log = $log;
    this.ChannelService = ChannelService;
    this.CmsService = CmsService;

    CmsService.subscribe('reload-page', () => this.reload());
  }

  initialize(iframeJQueryElement) {
    this.iframeJQueryElement = iframeJQueryElement;
    this.pageLoaded = false;
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

    this.deferredReload = this.$q.defer();
    this.iframeJQueryElement[0].contentWindow.location.reload();
    return this.deferredReload.promise;
  }

  // called by the hippoIframe controller when the processing of the loaded page is completed.
  signalPageLoadCompleted() {
    this.renderPathInfo = this._determineRenderPathInfo();
    this.pageLoaded = true;

    const deferred = this.deferredReload;
    if (deferred) {
      // delete the "state" before resolving the promise.
      delete this.deferredReload;
      deferred.resolve();
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
}

export default HippoIframeService;
