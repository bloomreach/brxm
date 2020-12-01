/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

export default class CommunicationService {
  constructor($injector, $q, $rootScope, Penpal) {
    'ngInject';

    this.$injector = $injector;
    this.$q = $q;
    this.$rootScope = $rootScope;
    this.Penpal = Penpal;

    this.disableScroll = this._call.bind(this, 'disableScroll');
    this.emit = this._call.bind(this, 'emit');
    this.enableScroll = this._call.bind(this, 'enableScroll');
    this.getScroll = this._call.bind(this, 'getScroll');
    this.parseElements = this._call.bind(this, 'parseElements');
    this.reload = this._call.bind(this, 'reload');
    this.selectComponent = this._call.bind(this, 'selectComponent');
    this.setScroll = this._call.bind(this, 'setScroll');
    this.stopScroll = this._call.bind(this, 'stopScroll');
    this.toggleAddMode = this._call.bind(this, 'toggleAddMode');
    this.toggleComponentsOverlay = this._call.bind(this, 'toggleComponentsOverlay');
    this.toggleContentsOverlay = this._call.bind(this, 'toggleContentsOverlay');
    this.updateComponent = this._call.bind(this, 'updateComponent');
    this.updateContainer = this._call.bind(this, 'updateContainer');
  }

  async connect({ target, origin }) {
    this._connection = this.Penpal.connectToChild({
      childOrigin: origin,
      iframe: target,
      methods: {
        emit: this._emit.bind(this),
        getAsset: this._getAsset.bind(this),
        getAssetUrl: this._getAssetUrl.bind(this),
        getLocale: this._getLocale.bind(this),
        getScroll: this._getScroll.bind(this),
        getTranslations: this._getTranslations.bind(this),
        isEditable: this._isEditable.bind(this),
        isEditSharedContainers: this._isEditSharedContainers.bind(this),
        ready: this._ready.bind(this),
      },
    });

    this._child = await this._connection.promise;
  }

  async ready() {
    if (!this._await) {
      this._await = this.$q.defer();
    }

    // eslint-disable-next-line consistent-return
    return this._await.promise;
  }

  disconnect() {
    if (!this._connection) {
      return;
    }

    this._connection.destroy();
    delete this._await;
    delete this._child;
    delete this._connection;
  }

  _call(command, ...args) {
    if (!this._child) {
      return;
    }

    // eslint-disable-next-line consistent-return
    return this._child[command](...args);
  }

  _emit(event, data) {
    this.$rootScope.$emit(`iframe:${event}`, data);
  }

  _getAsset(...args) {
    return this.$injector.get('HippoIframeService').getAsset(...args);
  }

  _getAssetUrl(...args) {
    return this.$injector.get('HippoIframeService').getAssetUrl(...args);
  }

  _getLocale() {
    return this.$injector.get('$translate').use();
  }

  _getScroll(...args) {
    return this.$injector.get('ScrollService').getScroll(...args);
  }

  _getTranslations(...args) {
    return this.$injector.get('$translate').getTranslationTable(...args);
  }

  _isEditable(...args) {
    return this.$injector.get('ChannelService').isEditable(...args);
  }

  _isEditSharedContainers(...args) {
    return this.$injector.get('HippoIframeService').isEditSharedContainers(...args);
  }

  _ready() {
    if (!this._await) {
      this._await = this.$q.defer();
    }

    this._await.resolve();
  }
}
