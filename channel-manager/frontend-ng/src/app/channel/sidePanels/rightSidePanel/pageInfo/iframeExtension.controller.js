/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
class IframeExtensionCtrl {
  constructor(
    $element,
    $log,
    $sce,
    $window,
    ChannelService,
    ConfigService,
    DomService,
    ExtensionService,
    HippoIframeService,
    PathService,
    Penpal,
  ) {
    'ngInject';

    this.$element = $element;
    this.$log = $log;
    this.$sce = $sce;
    this.$window = $window;
    this.ChannelService = ChannelService;
    this.ConfigService = ConfigService;
    this.DomService = DomService;
    this.ExtensionService = ExtensionService;
    this.HippoIframeService = HippoIframeService;
    this.PathService = PathService;
    this.Penpal = Penpal;
  }

  $onInit() {
    this._initExtension();
  }

  _initExtension() {
    this.extension = this.ExtensionService.getExtension(this.extensionId);

    this.connection = this.Penpal.connectToChild({
      url: this._getExtensionUrl(),
      appendTo: this.$element[0],
      methods: {
        getProperties: () => this._getProperties(),
        getPage: () => this.context,
        refreshChannel: () => this.ChannelService.reload(),
        refreshPage: () => this.HippoIframeService.reload(),
      },
    });

    // Don't allow an extension to change the URL of the top-level window: sandbox the iframe and DON'T include:
    // - allow-top-navigation
    // - allow-top-navigation-by-user-activation
    $(this.connection.iframe).attr('sandbox', 'allow-forms allow-popups allow-popups-to-escape-sandbox allow-same-origin allow-scripts');

    this.connection.promise
      .then((child) => {
        this.child = child;
      })
      .catch((error) => {
        this.$log.warn(`Extension '${this.extension.displayName}' failed to connect with the client library.`, error);
      });
  }

  _getExtensionUrl() {
    if (this._isAbsoluteUrl(this.extension.url)) {
      return this._getTrustedAbsoluteUrl(this.extension.url);
    }
    return this._getUrlRelativeToCmsLocation(this.extension.url);
  }

  _isAbsoluteUrl(url) {
    return url.startsWith('http://') || url.startsWith('https://');
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

  _addQueryParameters(url) {
    url.searchParams.append('br.antiCache', this.ConfigService.antiCache);
    url.searchParams.append('br.parentOrigin', this.$window.location.origin);
  }

  _getProperties() {
    return {
      baseUrl: this.ConfigService.getCmsOrigin() + this.ConfigService.getCmsContextPath(),
      extension: {
        config: this.extension.config,
      },
      locale: this.ConfigService.locale,
      timeZone: this.ConfigService.timeZone,
      user: this.ConfigService.cmsUser,
      version: this.ConfigService.cmsVersion,
    };
  }

  $onChanges(params) {
    const changedContext = params.context;

    if (changedContext) {
      // copy the context so any changes to it won't affect the parent version
      this.context = angular.copy(changedContext.currentValue);

      if (this.child) {
        this.child.emitPageEvent('load', this.context);
      }
    }
  }
}

export default IframeExtensionCtrl;
