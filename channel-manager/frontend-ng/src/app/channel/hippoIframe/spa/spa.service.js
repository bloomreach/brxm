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

const PROPERTY_URL = 'url';
const PROPERTY_SPA_URL = 'org.hippoecm.hst.configuration.channel.PreviewURLChannelInfo_url';

export default class SpaService {
  constructor($log, $rootScope, ChannelService, DomService, OverlayService, RenderingService, RpcService) {
    'ngInject';

    this.$log = $log;
    this.$rootScope = $rootScope;
    this.ChannelService = ChannelService;
    this.DomService = DomService;
    this.OverlayService = OverlayService;
    this.RenderingService = RenderingService;
    this.RpcService = RpcService;

    this._onSdkReady = this._onSdkReady.bind(this);
    this._onUnload = this._onUnload.bind(this);
    this._sync = this._sync.bind(this);
  }

  init(iframeJQueryElement) {
    this.iframeJQueryElement = iframeJQueryElement;

    this.RpcService.initialize({
      origin: this.getOrigin(),
      target: iframeJQueryElement[0].contentWindow,
    });

    if (this._offSdkUnload) {
      this._offSdkUnload();
    }
    this._offSdkUnload = this.$rootScope.$on('iframe:unload', this._onUnload);

    if (this._offSdkReady) {
      this._offSdkReady();
    }
    this._offSdkReady = this.$rootScope.$on('spa:ready', this._onSdkReady);

    this.RpcService.register('sync', this._sync);
  }

  destroy() {
    if (this._offSdkReady) {
      this._offSdkReady();
      delete this._offSdkReady;
    }
    if (this._offSdkUnload) {
      this._offSdkUnload();
      delete this._offSdkUnload;
    }

    this.RpcService.destroy();

    this._isSpa = false;
    delete this._legacyHandle;
  }

  getOrigin() {
    const properties = this.ChannelService.getProperties();
    const channel = this.ChannelService.getChannel();
    const url = (properties && properties[PROPERTY_SPA_URL]) || (channel && channel[PROPERTY_URL]);

    if (!url) {
      return;
    }

    try {
      const { origin } = new URL(url);

      // eslint-disable-next-line consistent-return
      return origin;
    } catch (error) {} // eslint-disable-line no-empty
  }

  _onSdkReady() {
    this._isSpa = true;
  }

  _onUnload() {
    this._isSpa = false;
    delete this._legacyHandle;
  }

  isSpa() {
    return !!(this._isSpa || this._legacyHandle);
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

  inject(resource) {
    return this.RpcService.call('inject', resource);
  }

  renderComponent(component, properties = {}) {
    if (!component || !this.isSpa()) {
      return false;
    }

    if (!this._legacyHandle) {
      this.RpcService.trigger('update', { properties, id: component.getReferenceNamespace() });
      return true;
    }

    if (!angular.isFunction(this._legacyHandle.renderComponent)) {
      return false;
    }

    try {
      return this._legacyHandle.renderComponent(component.getReferenceNamespace(), properties) !== false;
    } catch (error) {
      this.$log.error(error);
      return true;
    }
  }

  _sync() {
    return this.RenderingService.createOverlay();
  }
}
