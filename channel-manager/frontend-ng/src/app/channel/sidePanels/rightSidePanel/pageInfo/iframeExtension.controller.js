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
import Penpal from 'penpal';

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
    this.$window = $window;
    this.ChannelService = ChannelService;
    this.ConfigService = ConfigService;
    this.DomService = DomService;
    this.ExtensionService = ExtensionService;
    this.HippoIframeService = HippoIframeService;
    this.PathService = PathService;
  }

  $onInit() {
    this._initExtension();
  }

  _initExtension() {
    this.extension = this.ExtensionService.getExtension(this.extensionId);

    this.connection = Penpal.connectToChild({
      url: this._getExtensionUrl(),
      appendTo: this.$element[0],
      methods: {
        getCmsProperties: () => ({ user: this.ConfigService.cmsUser }),
      },
    });

    this.connection.promise.then((child) => {
      this.child = child;
      this.iframeLoaded = true;
      this._setIframeContext();
    });
    // old _initExtension code:
    //
    // try {
    //   const publicApi = {
    //     refreshChannel: () => {
    //       this.ChannelService.reload();
    //     },
    //     refreshPage: () => {
    //       this.HippoIframeService.reload();
    //     },
    //     config: this.extension.config,
    //   };
    //   this.iframeWindow.BR_EXTENSION.onInit(publicApi);
    // } catch (e) {
    //   this._warnExtension('threw an error in window.BR_EXTENSION.onInit()', e);
    // }
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
    url.searchParams.append('antiCache', this.ConfigService.antiCache);
    return this.$sce.trustAsResourceUrl(url.href);
  }

  _getUrlRelativeToCmsLocation(extensionUrl) {
    const path = this.PathService.concatPaths(this.ConfigService.getCmsContextPath(), extensionUrl);
    // The current location should be the default value for the second parameter of the URL() constructor,
    // but Chrome needs it explicitly otherwise it will throw an error.
    const url = new URL(path, this.$window.location.origin);
    url.searchParams.append('antiCache', this.ConfigService.antiCache);
    return url.pathname + url.search;
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

  _setIframeContext() {
    this._warnExtension('should update the page context: TODO');
    // if (!angular.isObject(this.iframeWindow.BR_EXTENSION)) {
    //   this._warnExtension('does not define a window.BR_EXTENSION object, cannot provide context');
    //   return;
    // }
    //
    // if (!angular.isFunction(this.iframeWindow.BR_EXTENSION.onContextChanged)) {
    //   this._warnExtension('does not define a window.BR_EXTENSION.onContextChanged function, cannot provide context');
    //   return;
    // }
    //
    // try {
    //   const extensionPoint = this.extension.extensionPoint;
    //   const contextData = this.context;
    //   this.iframeWindow.BR_EXTENSION.onContextChanged({
    //     extensionPoint,
    //     data: contextData,
    //   });
    // } catch (e) {
    //   this._warnExtension('threw an error in window.BR_EXTENSION.onContextChanged()', e);
    // }
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
