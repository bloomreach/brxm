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

import { Typed } from 'emittery';
import { EventBus, Events } from './events';
import { CmsImpl } from './cms';
import { RpcClient, RpcServer } from './rpc';

describe('CmsImpl', () => {
  let cms: CmsImpl;
  let eventBus: EventBus;
  let rpcClient: jest.Mocked<RpcClient<any, any>>;
  let rpcServer: jest.Mocked<RpcServer<any, any>>;

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

    it('should trigger ready event only once per window', () => {
      getReadyState.mockReturnValueOnce('interactive');
      cms.initialize({ window });
      cms.initialize({ window });

      expect(rpcServer.trigger).toHaveBeenCalledTimes(1);
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

    it('should register inject procedure', async () => {
      cms.initialize({ window });

      expect(rpcServer.register).toHaveBeenCalledWith('inject', expect.any(Function));
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

      const [, onUpdate] = rpcClient.on.mock.calls.pop()!;
      const event = { id: 'id', properties: { a: 'b' } };
      const emitSpy = spyOn(eventBus, 'emit');
      onUpdate(event);

      expect(emitSpy).toHaveBeenCalledWith('cms.update', event);
    });
  });

  describe('inject', () => {
    let inject: (string: any) => Promise<void>;
    const sourceDocument = document;
    const getDocument = jest.fn<Document | undefined, []>(() => sourceDocument);

    beforeEach(() => {
      ([[, inject]] = rpcServer.register.mock.calls);
      cms.initialize({ window });
      Object.defineProperty(window, 'document', { get: getDocument });
    });

    it('should not proceed if there is no window', async () => {
      getDocument.mockReturnValueOnce(undefined);

      await expect(inject('something')).rejects.toThrow('SPA document is not ready.');
    });

    it('should put a script element', async () => {
      inject('url1');

      await new Promise(process.nextTick);

      const script = window.document.querySelector('script[src="url1"]');

      expect(script).not.toBeNull();
      expect(script).toMatchSnapshot();
    });

    it('should resolve a promise on load event', async () => {
      const promise = inject('url2');

      await new Promise(process.nextTick);
      const element = window.document.querySelector('script[src="url2"]');
      element?.dispatchEvent(new Event('load'));

      await expect(promise).resolves.toBeUndefined();
    });

    it('should reject a promise on error event', async () => {
      const promise = inject('url3');

      await new Promise(process.nextTick);
      const element = window.document.querySelector('script[src="url3"]');
      element?.dispatchEvent(new Event('error'));

      await expect(promise).rejects.toThrow("Failed to load resource 'url3'.");
    });
  });
});
