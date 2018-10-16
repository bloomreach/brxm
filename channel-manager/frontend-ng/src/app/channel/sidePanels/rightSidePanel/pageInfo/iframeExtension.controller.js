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
    $scope,
    $window,
    ChannelService,
    ConfigService,
    DomService,
    ExtensionService,
    HippoIframeService,
    PathService,
  ) {
    'ngInject';

    this.$element = $element;
    this.$log = $log;
    this.$sce = $sce;
    this.$scope = $scope;
    this.$window = $window;
    this.ChannelService = ChannelService;
    this.ConfigService = ConfigService;
    this.DomService = DomService;
    this.ExtensionService = ExtensionService;
    this.HippoIframeService = HippoIframeService;
    this.PathService = PathService;

    this.$scope.trustedSource = resource => this.$sce.trustAsResourceUrl(resource);
  }

  $onInit() {
    this.extension = this.ExtensionService.getExtension(this.extensionId);

    const extensionIframe = this.$element.children('.iframe-extension');
    this.iframeWindow = this.DomService.getIframeWindow(extensionIframe);

    extensionIframe.on('load', () => {
      this.iframeLoaded = true;
      this._initExtension();
      this._setIframeContext();
    });
  }

  getExtensionUrl() {
    if (this.extension.url.startsWith('/')) {
      const path = this.PathService.concatPaths(this.ConfigService.getCmsContextPath(), this.extension.url);
      // The current location should be the default value for the second parameter of the URL() constructor,
      // but Chrome needs it explicitly otherwise it will throw an error.
      const url = new URL(path, this.$window.location.origin);
      url.searchParams.append('antiCache', this.ConfigService.antiCache);
      return url.pathname + url.search;
    }

    const url = new URL(this.extension.url);
    url.searchParams.append('antiCache', this.ConfigService.antiCache);
    return url.href;
  }

  $onChanges(params) {
    const changedContext = params.context;

    if (changedContext) {
      // copy the context so any changes to it won't affect the parent version
      this.context = angular.copy(changedContext.currentValue);

      // the iframe's onload handler sets the initial context, so only subsequent context changes
      // need to be passed to the iframe.
      if (!changedContext.isFirstChange() && this.iframeLoaded) {
        this._setIframeContext();
      }
    }
  }

  _initExtension() {
    if (!angular.isObject(this.iframeWindow.BR_EXTENSION)) {
      this._warnExtension('does not define a window.BR_EXTENSION object, cannot initialize');
      return;
    }

    if (!angular.isFunction(this.iframeWindow.BR_EXTENSION.onInit)) {
      this._warnExtension('does not define a window.BR_EXTENSION.onInit function, cannot initialize');
      return;
    }

    try {
      const publicApi = {
        refreshChannel: () => {
          this.ChannelService.reload();
        },
        refreshPage: () => {
          this.HippoIframeService.reload();
        },
        config: this.extension.config,
      };
      this.iframeWindow.BR_EXTENSION.onInit(publicApi);
    } catch (e) {
      this._warnExtension('threw an error in window.BR_EXTENSION.onInit()', e);
    }
  }

  _setIframeContext() {
    if (!angular.isObject(this.iframeWindow.BR_EXTENSION)) {
      this._warnExtension('does not define a window.BR_EXTENSION object, cannot provide context');
      return;
    }

    if (!angular.isFunction(this.iframeWindow.BR_EXTENSION.onContextChanged)) {
      this._warnExtension('does not define a window.BR_EXTENSION.onContextChanged function, cannot provide context');
      return;
    }

    try {
      const extensionPoint = this.extension.extensionPoint;
      const contextData = this.context;
      this.iframeWindow.BR_EXTENSION.onContextChanged({
        extensionPoint,
        data: contextData,
      });
    } catch (e) {
      this._warnExtension('threw an error in window.BR_EXTENSION.onContextChanged()', e);
    }
  }

  _warnExtension(message, error) {
    const warning = `Extension '${this.extension.displayName}' ${message}`;
    if (error) {
      this.$log.warn(warning, error);
    } else {
      this.$log.warn(warning);
    }
  }
}

export default IframeExtensionCtrl;
