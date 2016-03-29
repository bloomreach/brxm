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

  constructor($q, $log) {
    'ngInject';

    this.$q = $q;
    this.$log = $log;
  }

  initialize(iframeJQueryElement) {
    this.iframeJQueryElement = iframeJQueryElement;
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
    const deferred = this._deferredReload;
    if (deferred) {
      // delete the "state" before resolving the promise.
      delete this._deferredReload;
      deferred.resolve();
    }
  }
}
