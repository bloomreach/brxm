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
  constructor($log, $q, $rootScope, ChannelService, DomService, RenderingService, RpcService) {
    'ngInject';

    this.$log = $log;
    this.$q = $q;
    this.$rootScope = $rootScope;
    this.ChannelService = ChannelService;
    this.DomService = DomService;
    this.RenderingService = RenderingService;
    this.RpcService = RpcService;

    this._onSdkReady = this._onSdkReady.bind(this);
    this._onUnload = this._onUnload.bind(this);
    this._onSync = this._onSync.bind(this);

    this._renderingPool = [];
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

    this.RpcService.register('sync', this._onSync);
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
    this._onUnload();
  }

  _onSdkReady() {
    this._isSpa = true;
  }

  _onUnload() {
    this._isSpa = false;
    delete this._legacyHandle;

    this._renderingPool.splice(0)
      .forEach(deferred => deferred.reject(new Error('Could not update the component.')));
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
          this._onSync();
        },
        syncOverlay: () => {
          this._warnDeprecated();
          this._onSync();
        },
        sync: () => {
          this._warnDeprecated();
          this._onSync();
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

  async renderComponent(component, properties = {}) {
    if (!component || !this.isSpa()) {
      throw new Error('Cannot render the component in the SPA.');
    }

    if (!this._legacyHandle) {
      this.RpcService.trigger('update', { properties, id: component.getReferenceNamespace() });
    } else if (angular.isFunction(this._legacyHandle.renderComponent)) {
      this._legacyHandle.renderComponent(component.getReferenceNamespace(), properties);
    } else {
      throw new Error('The SPA does not support the component rendering.');
    }

    const deferred = this.$q.defer();
    this._renderingPool.push(deferred);

    return deferred.promise;
  }

  _onSync() {
    const initial = !this._renderingPool.length;
    this._renderingPool.splice(0)
      .forEach(deferred => deferred.resolve());

    this.RenderingService.createOverlay(initial);
  }
}
