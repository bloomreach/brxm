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
  constructor($element, $log, $uiRouterGlobals, DomService, ExtensionService) {
    'ngInject';

    this.$element = $element;
    this.$log = $log;
    this.$uiRouterGlobals = $uiRouterGlobals;
    this.DomService = DomService;
    this.ExtensionService = ExtensionService;
  }

  $onInit() {
    const extensionId = this.$uiRouterGlobals.params.extensionId;
    this.extension = this.ExtensionService.getExtension(extensionId);

    this.pageUrl = this.$uiRouterGlobals.params.pageUrl;

    const extensionIframe = this.$element.children('.iframe-extension');
    this.iframeWindow = this.DomService.getIframeWindow(extensionIframe);

    extensionIframe.on('load', () => this._onIframeLoaded());
  }

  _onIframeLoaded() {
    if (this._isApiValid()) {
      this._setPageContext();
    }
  }

  _isApiValid() {
    if (!angular.isObject(this.iframeWindow.BR_EXTENSION)) {
      this._warnPageExtension('does not define a window.BR_EXTENSION object, cannot provide page context');
      return false;
    } else if (!angular.isFunction(this.iframeWindow.BR_EXTENSION.onContextChanged)) {
      this._warnPageExtension('does not define a window.BR_EXTENSION.onContextChanged function, cannot provide page context');
      return false;
    }
    return true;
  }

  _setPageContext() {
    try {
      this.iframeWindow.BR_EXTENSION.onContextChanged({
        context: 'page',
        data: {
          pageUrl: this.pageUrl,
        },
      });
    } catch (e) {
      this._warnPageExtension('threw an error in window.BR_EXTENSION.onContextChanged()', e);
    }
  }

  _warnPageExtension(message, error) {
    const warning = `Page info extension '${this.extension.displayName}' ${message}`;
    if (error) {
      this.$log.warn(warning, error);
    } else {
      this.$log.warn(warning);
    }
  }
}

export default IframeExtensionCtrl;
