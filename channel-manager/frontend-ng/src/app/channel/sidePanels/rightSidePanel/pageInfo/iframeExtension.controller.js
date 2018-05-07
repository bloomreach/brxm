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
  constructor($element, $uiRouterGlobals, ExtensionService) {
    'ngInject';

    this.$element = $element;
    this.$uiRouterGlobals = $uiRouterGlobals;
    this.ExtensionService = ExtensionService;
  }

  $onInit() {
    const extensionId = this.$uiRouterGlobals.params.extensionId;
    this.extension = this.ExtensionService.getExtension(extensionId);
    this.url = this.extension.urlPath;
    this.pageUrl = this.$uiRouterGlobals.params.pageUrl;

    this.extensionIframe = this.$element.children('.iframe-extension')[0];
    $(this.extensionIframe).on('load', () => this._onIframeLoaded());
  }

  _onIframeLoaded() {
  }
}

export default IframeExtensionCtrl;
