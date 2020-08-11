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

import { Message, Rpc } from './rpc';

describe('Rpc', () => {
  const rpc = new class extends Rpc<any, any, any, any> {
    send = jest.fn();
    process(message: Message) {
      super.process(message);
    }
  }();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('call', () => {
    it('should send a request', () => {
      rpc.call('something', 'param1', 'param2');

      expect(rpc.send).toHaveBeenCalledWith(expect.objectContaining({
        type: 'brxm:request',
        id: expect.any(String),
        command: 'something',
        payload: ['param1', 'param2'],
      }));
    });
  });

  describe('trigger', () => {
    it('should send an event', () => {
      rpc.trigger('something', { a: 'b' });

      expect(rpc.send).toHaveBeenCalledWith(expect.objectContaining({
        type: 'brxm:event',
        event: 'something',
        payload: { a: 'b' },
      }));
    });
  });

  describe('process', () => {
    it('should not fail on malformed message', () => {
      expect(() => rpc.process(undefined as unknown as Message)).not.toThrow();
      expect(() => rpc.process({} as Message)).not.toThrow();
    });

    it('should emit an event', () => {
      spyOn(rpc, 'emit');
      rpc.process({ type: 'brxm:event', event: 'something', payload: { a: 'b' } });

      expect(rpc.emit).toHaveBeenCalledWith('something', { a: 'b' });
    });

    it('should resolve a call', () => {
      let id: string;
      rpc.send.mockImplementationOnce((message) => { id = message.id; });
      const promise = rpc.call('command');

      rpc.process({ id: 'id', type: 'brxm:response', state: 'rejected', result: 'a' });
      rpc.process({ id: id!, type: 'brxm:response', state: 'fulfilled', result: 'b' });

      expect(promise).resolves.toBe('b');
    });

    it('should reject a call', () => {
      let id: string;
      rpc.send.mockImplementationOnce((message) => { id = message.id; });
      const promise = rpc.call('command');

      rpc.process({ id: 'id', type: 'brxm:response', state: 'fulfilled', result: 'a' });
      rpc.process({ id: id!, type: 'brxm:response', state: 'rejected', result: 'b' });

      expect(promise).rejects.toBe('b');
    });

    it('should call a registered callback', async () => {
      const callback = jest.fn(async () => 'something');
      rpc.register('command', callback);
      rpc.process({ type: 'brxm:request', id: 'id1', command: 'something', payload: ['a', 'b'] });
      rpc.process({ type: 'brxm:request', id: 'id2', command: 'command', payload: ['c', 'd'] });

      await new Promise(process.nextTick);
      expect(callback).toHaveBeenCalledWith('c', 'd');
    });

    it('should send a fulfilled response', async () => {
      const callback = jest.fn(async () => 'something');
      rpc.register('command', callback);
      rpc.process({ type: 'brxm:request', id: 'id1', command: 'command', payload: ['a', 'b'] });

      await new Promise(process.nextTick);
      expect(rpc.send).toHaveBeenCalledWith(expect.objectContaining({
        type: 'brxm:response',
        id: 'id1',
        state: 'fulfilled',
        result: 'something',
      }));
    });

    it('should send a rejected response', async () => {
      const callback = jest.fn(async () => { throw 'something'; });
      rpc.register('command', callback);
      rpc.process({ type: 'brxm:request', id: 'id1', command: 'command', payload: ['a', 'b'] });

      await new Promise(process.nextTick);
      expect(rpc.send).toHaveBeenCalledWith(expect.objectContaining({
        type: 'brxm:response',
        id: 'id1',
        state: 'rejected',
        result: 'something',
      }));
    });
  });
});
