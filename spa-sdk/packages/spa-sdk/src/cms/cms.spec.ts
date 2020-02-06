/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

import { mocked } from 'ts-jest/utils';
import { Typed } from 'emittery';
import { Events } from '../events';
import { CmsImpl } from './cms';
import { RpcClient, RpcServer } from './rpc';

describe('CmsImpl', () => {
  let cms: CmsImpl;
  let eventBus: Typed<Events>;
  let rpcClient: RpcClient<any, any>;
  let rpcServer: RpcServer<any, any>;

  beforeEach(() => {
    eventBus = new Typed<Events>();
    rpcClient = {
      call: jest.fn(),
      on: jest.fn(),
      off: jest.fn(),
    };
    rpcServer = {
      register: jest.fn(),
      trigger: jest.fn(),
    };
    cms = new CmsImpl(eventBus, rpcClient, rpcServer);
  });

  describe('initialize', () => {
    const getReadyState = jest.fn();

    beforeEach(() => {
      Object.defineProperty(document, 'readyState', {
        get: getReadyState,
      });
    });

    it('should trigger ready event right away', () => {
      getReadyState.mockReturnValueOnce('interactive');
      cms.initialize({ window });

      expect(rpcServer.trigger).toHaveBeenCalledWith('ready', undefined);
    });

    it('should not trigger ready event on state change if the state is still loading', async () => {
      getReadyState.mockReturnValueOnce('loading');
      getReadyState.mockReturnValueOnce('loading');
      cms.initialize({ window });

      document.dispatchEvent(new ProgressEvent('readystatechange'));
      await new Promise(resolve => setTimeout(resolve, 0));

      expect(rpcServer.trigger).not.toHaveBeenCalled();
    });

    it('should trigger ready event on state change', async () => {
      getReadyState.mockReturnValueOnce('loading');
      getReadyState.mockReturnValueOnce('interactive');
      cms.initialize({ window });

      document.dispatchEvent(new ProgressEvent('readystatechange'));
      await new Promise(resolve => setTimeout(resolve, 0));

      expect(rpcServer.trigger).toHaveBeenCalledWith('ready', undefined);
    });
  });

  describe('onPageReady', () => {
    it('should process postponed events on initialization', async () => {
      cms.initialize({ window });
      await eventBus.emit('page.ready', {});

      expect(rpcClient.call).toHaveBeenCalledWith('sync');
    });
  });

  describe('onUpdate', () => {
    it('should process postponed events on initialization', async () => {
      cms.initialize({ window });

      expect(rpcClient.on).toHaveBeenCalledWith('update', expect.any(Function));

      const [, onUpdate] = mocked(rpcClient.on).mock.calls.pop()!;
      const event = { id: 'id', properties: { a: 'b' } };
      const emitSpy = spyOn(eventBus, 'emit');
      onUpdate(event);

      expect(emitSpy).toHaveBeenCalledWith('cms.update', event);
    });
  });
});
