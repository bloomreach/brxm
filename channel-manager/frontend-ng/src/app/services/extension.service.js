/*
 * Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

function isAbsoluteUrl(url) {
  return url.startsWith('http://') || url.startsWith('https://');
}

export default class ExtensionService {
  constructor($sce, $window, ConfigService, PathService) {
    'ngInject';

    this.$sce = $sce;
    this.$window = $window;
    this.ConfigService = ConfigService;
    this.PathService = PathService;
  }

  hasExtensions(extensionPoint) {
    if (this.ConfigService.extensions) {
      return this.ConfigService.extensions.some(extension => extension.extensionPoint === extensionPoint);
    }
    return false;
  }

  getExtensions(extensionPoint) {
    if (this.ConfigService.extensions) {
      return this.ConfigService.extensions.filter(extension => extension.extensionPoint === extensionPoint);
    }
    return [];
  }

  getExtension(id) {
    if (this.ConfigService.extensions) {
      return this.ConfigService.extensions.find(extension => extension.id === id);
    }
    return undefined;
  }

  getExtensionUrl(extension) {
    return isAbsoluteUrl(extension.url)
      ? this._getTrustedAbsoluteUrl(extension.url)
      : this._getUrlRelativeToCmsLocation(extension.url);
  }

  _addQueryParameters(url) {
    url.searchParams.append('br.antiCache', this.ConfigService.antiCache);
    url.searchParams.append('br.parentOrigin', this.$window.location.origin);
  }

  _getTrustedAbsoluteUrl(extensionUrl) {
    const url = new URL(extensionUrl);
    this._addQueryParameters(url);

    return this.$sce.trustAsResourceUrl(url.href);
  }

  _getUrlRelativeToCmsLocation(extensionUrl) {
    const path = this.PathService.concatPaths(this.ConfigService.getCmsContextPath(), extensionUrl);
    // The current location should be the default value for the second parameter of the URL() constructor,
    // but Chrome needs it explicitly otherwise it will throw an error.
    const url = new URL(path, this.$window.location.origin);
    this._addQueryParameters(url);

    return url.pathname + url.search;
  }
}
