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
  }

  async connect() {
    this._connection = this.Penpal.connectToParent({
      methods: {
        parseElements: this._parseElements.bind(this),
        updateComponent: this._updateComponent.bind(this),
        updateContainer: this._updateContainer.bind(this),
        emit: this._emit.bind(this),
      },
    });

    this._parent = await this._connection.promise;
  }

  _emit(event, data) {
    this.$rootScope.$emit(`cm:${event}`, data);
  }

  _parseElements(...args) {
    return this.$injector.get('PageStructureService').parseElements(...args);
  }

  _updateComponent(...args) {
    return this.$injector.get('PageStructureService').updateComponent(...args);
  }

  _updateContainer(...args) {
    return this.$injector.get('PageStructureService').updateContainer(...args);
  }

  emit(event, data) {
    if (!this._parent) {
      return;
    }

    this._parent.emit(event, data);
  }
}
