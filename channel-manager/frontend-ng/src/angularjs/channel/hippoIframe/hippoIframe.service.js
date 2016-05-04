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

export class HippoIframeService {

  constructor($q, $log, ChannelService) {
    'ngInject';

    this.$q = $q;
    this.$log = $log;
    this.ChannelService = ChannelService;
  }

  initialize(iframeJQueryElement) {
    this.iframeJQueryElement = iframeJQueryElement;

    // start at the home page
    this.load(this.ChannelService.getHomePageRenderPathInfo());
  }

  load(renderPathInfo) {
    if (renderPathInfo !== this.renderPathInfo) {
      // navigate to a new page
      const targetSrc = this.ChannelService.makePath(renderPathInfo);
      if (targetSrc !== this.src) {
        this.src = targetSrc;
      } else if (this.iframeJQueryElement /* pro-forma-check */) {
        // the src attribute of the iframe already has the desired value, and
        // angular's 2-way binding won't trigger a reload, so use jQuery to achieve the desired effect
        this.iframeJQueryElement.attr('src', this.src);
      }
    } else {
      // we're already on the right page. We trigger a reload and forget about the src attribute
      this.reload();
    }
  }

  _extractRenderPathInfo(path) {
    this.renderPathInfo = this.ChannelService.extractRenderPathInfo(path);
  }

  getSrc() {
    return this.src;
  }

  getCurrentRenderPathInfo() {
    return this.renderPathInfo;
  }

  reload() {
    if (this._deferredReload) {
      this.$log.warn('Trying to reload when a reload is already ongoing. Taking no action.');
      return this._deferredReload.promise;
    }

    const deferred = this.$q.defer();
    if (this.iframeJQueryElement && this.iframeJQueryElement[0]) {
      this.iframeJQueryElement[0].contentWindow.location.reload();
      this._deferredReload = deferred;
    } else {
      this.$log.warn('Reload requested while iframe element unknown. Not reloading.');
      deferred.resolve(); // resolving (rather than rejecting) keeps error handling simpler
    }
    return deferred.promise;
  }

  // called by the hippoIframe controller when the processing of the loaded page is completed.
  signalPageLoadCompleted() {
    this._extractRenderPathInfo(this.iframeJQueryElement[0].contentWindow.location.pathname);

    const deferred = this._deferredReload;
    if (deferred) {
      // delete the "state" before resolving the promise.
      delete this._deferredReload;
      deferred.resolve();
    }
  }
}
