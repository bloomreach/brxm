/*
 * Copyright 2018-2020 Hippo B.V. (http://www.onehippo.com)
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

export default class SpaService {
  constructor($log, DomService, OverlayService, RenderingService) {
    'ngInject';

    this.$log = $log;
    this.DomService = DomService;
    this.OverlayService = OverlayService;
    this.RenderingService = RenderingService;
  }

  init(iframeJQueryElement) {
    this.iframeJQueryElement = iframeJQueryElement;
  }

  isSpa() {
    return !!this._legacyHandle;
  }

  /**
   * @deprecated
   */
  initLegacy() {
    this._detectLegacySpa();
    if (!this._legacyHandle) {
      return false;
    }

    try {
      const publicApi = {
        createOverlay: () => {
          this._warnDeprecated();
          this.RenderingService.createOverlay();
        },
        syncOverlay: () => {
          this._warnDeprecated();
          this.OverlayService.sync();
        },
        sync: () => {
          this._warnDeprecated();
          this.RenderingService.createOverlay();
        },
      };
      this._legacyHandle.init(publicApi);
    } catch (error) {
      this.$log.error('Failed to initialize Single Page Application', error);
    }

    return true;
  }

  _detectLegacySpa() {
    this._legacyHandle = null;

    const iframeWindow = this.DomService.getIframeWindow(this.iframeJQueryElement);
    if (iframeWindow) {
      this._legacyHandle = iframeWindow.SPA || null;
    }
  }

  _warnDeprecated() {
    this.$log.warn('This version of the SPA SDK is deprecated and will not work in the next major release.');
  }

  renderComponent(component, properties = {}) {
    if (!component || !this._legacyHandle || !angular.isFunction(this._legacyHandle.renderComponent)) {
      return false;
    }

    try {
      return this._legacyHandle.renderComponent(component.getReferenceNamespace(), properties) !== false;
    } catch (error) {
      this.$log.error(error);
      return true;
    }
  }
}
