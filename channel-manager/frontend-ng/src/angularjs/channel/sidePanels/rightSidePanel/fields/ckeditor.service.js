/*
 * Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
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

class CKEditorService {

  constructor($log, $q, $timeout, $window, ConfigService, DomService, PathService) {
    'ngInject';

    this.$log = $log;
    this.$q = $q;
    this.$timeout = $timeout;
    this.$window = $window;
    this.ConfigService = ConfigService;
    this.DomService = DomService;
    this.PathService = PathService;
  }

  loadCKEditor() {
    if (this.ckeditor) {
      return this.ckeditor;
    }

    this.ckeditor = this.DomService.addScript(this.$window, this._getCKEditorUrl())
      .then(() => {
        this._setCKEditorTimestamp();
        return this._ckeditorLoaded();
      });

    return this.ckeditor;
  }

  _getCKEditorUrl() {
    return this.PathService.concatPaths(this.ConfigService.getCmsContextPath(), this.ConfigService.ckeditorUrl);
  }

  _setCKEditorTimestamp() {
    this.$window.CKEDITOR.timestamp = this.ConfigService.ckeditorTimestamp;
  }

  _ckeditorLoaded() {
    let pollTimeoutMillis = 2;
    const ready = this.$q.defer();

    const checkCKEditorLoaded = () => {
      if (typeof this.$window.CKEDITOR.on === 'function') {
        if (this.$window.CKEDITOR.status === 'loaded') {
          ready.resolve(this.$window.CKEDITOR);
        } else {
          this.$window.CKEDITOR.on('loaded', () => {
            ready.resolve(this.$window.CKEDITOR);
          });
        }
      } else {
        // try again using exponential backoff
        pollTimeoutMillis *= 2;
        this.$log.info(`Waiting ${pollTimeoutMillis} ms for CKEditor's event mechanism to load...`);
        this.$timeout(checkCKEditorLoaded, pollTimeoutMillis);
      }
    };

    checkCKEditorLoaded();

    return ready.promise;
  }
}

export default CKEditorService;
