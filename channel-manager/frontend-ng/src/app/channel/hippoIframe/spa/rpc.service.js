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

const TYPE_EVENT = 'brxm:event';
const TYPE_RESPONSE = 'brxm:response';
const TYPE_REQUEST = 'brxm:request';
const STATE_FULFILLED = 'fulfilled';
const STATE_REJECTED = 'rejected';

export default class RpcService {
  constructor($q, $rootScope, $window, ChannelService) {
    'ngInject';

    this.$q = $q;
    this.$rootScope = $rootScope;
    this.$window = $window;
    this.ChannelService = ChannelService;

    this._calls = new Map();
    this._callbacks = new Map();
    this._onMessage = this._onMessage.bind(this);
  }

  initialize(target) {
    this._target = target;
    this.$window.addEventListener('message', this._onMessage);
  }

  destroy() {
    this.$window.removeEventListener('message', this._onMessage);
    delete this._target;
  }

  call(command, ...payload) {
    return this.$q((resolve, reject) => {
      const id = this._generateId();

      this._calls.set(id, [resolve, reject]);
      this._send({
        id,
        command,
        payload,
        type: TYPE_REQUEST,
      });
    });
  }

  register(command, callback) {
    this._callbacks.set(command, callback);
  }

  trigger(event, payload) {
    this._send({ event, payload, type: TYPE_EVENT });
  }

  _generateId() {
    let id;
    do {
      id = `${Math.random()}`.slice(2);
    } while (this._calls.has(id));

    return id;
  }

  _send(message) {
    const origin = this.ChannelService.getOrigin();
    if (!this._target || !origin) {
      return;
    }

    this._target.postMessage(message, origin);
  }

  _onMessage(event) {
    if (!event.data || event.origin !== this.ChannelService.getOrigin()) {
      return;
    }

    this._process(event.data);
  }

  _process(message) {
    // eslint-disable-next-line default-case
    switch (message && message.type) {
      case TYPE_EVENT:
        this._processEvent(message);
        break;
      case TYPE_RESPONSE:
        this._processResponse(message);
        break;
      case TYPE_REQUEST:
        this._processRequest(message);
        break;
    }
  }

  _processEvent(event) {
    this.$rootScope.$emit(`spa:${event.event}`, event.payload);
  }

  _processResponse(response) {
    if (!this._calls.has(response.id)) {
      return;
    }

    const [resolve, reject] = this._calls.get(response.id);
    this._calls.delete(response.id);

    if (response.state === STATE_REJECTED) {
      reject(response.result);
      return;
    }

    resolve(response.result);
  }

  async _processRequest(request) {
    const callback = this._callbacks.get(request.command);

    if (!callback) {
      return;
    }

    try {
      this._send({
        type: TYPE_RESPONSE,
        id: request.id,
        state: STATE_FULFILLED,
        result: await callback(...request.payload),
      });
    } catch (result) {
      this._send({
        result,
        type: TYPE_RESPONSE,
        id: request.id,
        state: STATE_REJECTED,
      });
    }
  }
}
