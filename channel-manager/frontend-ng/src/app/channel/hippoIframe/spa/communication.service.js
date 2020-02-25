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
  constructor($rootScope, Penpal) {
    'ngInject';

    this.$rootScope = $rootScope;
    this.Penpal = Penpal;
  }

  async connect({ target, origin }) {
    this._connection = this.Penpal.connectToChild({
      childOrigin: origin,
      iframe: target,
      methods: {
        emit: this._emit.bind(this),
      },
    });

    this._child = await this._connection.promise;
  }

  disconnect() {
    if (!this._connection) {
      return;
    }

    this._connection.destroy();
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

  emit(event, data) {
    if (!this._child) {
      return;
    }

    this._child.emit(event, data);
  }

  parseElements(...args) {
    return this._call('parseElements', ...args);
  }

  updateComponent(...args) {
    return this._call('updateComponent', ...args);
  }

  updateContainer(...args) {
    return this._call('updateContainer', ...args);
  }
}
