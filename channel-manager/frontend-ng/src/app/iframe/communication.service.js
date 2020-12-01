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

    this.emit = this._call.bind(this, 'emit');
    this.getAsset = this._call.bind(this, 'getAsset');
    this.getAssetUrl = this._call.bind(this, 'getAssetUrl');
    this.getLocale = this._call.bind(this, 'getLocale');
    this.getScroll = this._call.bind(this, 'getScroll');
    this.getTranslations = this._call.bind(this, 'getTranslations');
    this.isEditable = this._call.bind(this, 'isEditable');
    this.isEditSharedContainers = this._call.bind(this, 'isEditSharedContainers');
    this.ready = this._call.bind(this, 'ready');
  }

  async connect() {
    this._connection = this.Penpal.connectToParent({
      methods: {
        disableScroll: this._disableScroll.bind(this),
        enableScroll: this._enableScroll.bind(this),
        getScroll: this._getScroll.bind(this),
        parseElements: this._parseElements.bind(this),
        reload: this._reload.bind(this),
        selectComponent: this._selectComponent.bind(this),
        setScroll: this._setScroll.bind(this),
        stopScroll: this._stopScroll.bind(this),
        toggleAddMode: this._toggleAddMode.bind(this),
        toggleComponentsOverlay: this._toggleComponentsOverlay.bind(this),
        toggleContentsOverlay: this._toggleContentsOverlay.bind(this),
        updateComponent: this._updateComponent.bind(this),
        updateContainer: this._updateContainer.bind(this),
        emit: this._emit.bind(this),
      },
    });

    this._parent = await this._connection.promise;
  }

  _call(command, ...args) {
    if (!this._parent) {
      return;
    }

    // eslint-disable-next-line consistent-return
    return this._parent[command](...args);
  }

  _emit(event, data) {
    this.$rootScope.$emit(`cm:${event}`, data);
  }

  _disableScroll(...args) {
    return this.$injector.get('ScrollService').disable(...args);
  }

  _enableScroll(...args) {
    return this.$injector.get('ScrollService').enable(...args);
  }

  _getScroll(...args) {
    return this.$injector.get('ScrollService').getScroll(...args);
  }

  _parseElements(...args) {
    return this.$injector.get('PageStructureService').parseElements(...args);
  }

  _reload() {
    return this.$injector.get('$window').location.reload();
  }

  _selectComponent(...args) {
    return this.$injector.get('OverlayService').selectComponent(...args);
  }

  _setScroll(...args) {
    return this.$injector.get('ScrollService').setScroll(...args);
  }

  _stopScroll(...args) {
    return this.$injector.get('ScrollService').stopScroll(...args);
  }

  _toggleAddMode(...args) {
    return this.$injector.get('OverlayService').toggleAddMode(...args);
  }

  _toggleComponentsOverlay(...args) {
    return this.$injector.get('OverlayService').toggleComponentsOverlay(...args);
  }

  _toggleContentsOverlay(...args) {
    return this.$injector.get('OverlayService').toggleContentsOverlay(...args);
  }

  _updateComponent(...args) {
    return this.$injector.get('PageStructureService').updateComponent(...args);
  }

  _updateContainer(...args) {
    return this.$injector.get('PageStructureService').updateContainer(...args);
  }
}
