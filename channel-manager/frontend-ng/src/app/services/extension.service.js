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

export default class ExtensionService {
  constructor($window, ConfigService, PathService) {
    'ngInject';

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
    const baseUrl = `${this.$window.location.origin}${this.ConfigService.getCmsContextPath() || ''}`;
    const url = new URL(extension.url, baseUrl);
    this._addQueryParameters(url);

    return url.href;
  }

  getExtensionRelativeUrl(extension, relativeUrl) {
    const extensionUrl = this.getExtensionUrl(extension);
    const newUrl = new URL(relativeUrl, extensionUrl);
    this._addQueryParameters(newUrl);
    return newUrl.href;
  }

  _addQueryParameters(url) {
    url.searchParams.append('br.antiCache', this.ConfigService.antiCache);
    url.searchParams.append('br.parentOrigin', this.$window.location.origin);
  }
}
