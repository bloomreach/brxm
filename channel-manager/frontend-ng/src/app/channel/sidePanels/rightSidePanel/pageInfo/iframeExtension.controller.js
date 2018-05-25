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
  constructor($element, $log, DomService, ExtensionService) {
    'ngInject';

    this.$element = $element;
    this.$log = $log;
    this.DomService = DomService;
    this.ExtensionService = ExtensionService;
  }

  $onInit() {
    this.extension = this.ExtensionService.getExtension(this.extensionId);

    const extensionIframe = this.$element.children('.iframe-extension');
    this.iframeWindow = this.DomService.getIframeWindow(extensionIframe);

    extensionIframe.on('load', () => {
      this.iframeLoaded = true;
      this._setIframeContext();
    });
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
    if (!angular.isObject(this.iframeWindow.BR_EXTENSION)) {
      this._warnExtension('does not define a window.BR_EXTENSION object, cannot provide context');
      return;
    }

    if (!angular.isFunction(this.iframeWindow.BR_EXTENSION.onContextChanged)) {
      this._warnExtension('does not define a window.BR_EXTENSION.onContextChanged function, cannot provide context');
      return;
    }

    try {
      const contextName = this.extension.context;
      const contextData = this.context;
      this.iframeWindow.BR_EXTENSION.onContextChanged({
        context: contextName,
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
