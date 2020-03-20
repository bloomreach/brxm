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

describe('RpcService', () => {
  let $rootScope;
  let $window;
  let ChannelService;
  let RpcService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    ChannelService = jasmine.createSpyObj('ChannelService', ['getOrigin']);

    angular.mock.module(($provide) => {
      $provide.value('ChannelService', ChannelService);
    });

    inject((
      _$rootScope_,
      _$window_,
      _RpcService_,
    ) => {
      $rootScope = _$rootScope_;
      $window = _$window_;
      RpcService = _RpcService_;

      spyOn($window, 'postMessage').and.callThrough();
      RpcService.initialize($window);
    });

    ChannelService.getOrigin.and.returnValue('http://localhost:3000');
  });

  describe('call', () => {
    it('should send a request', () => {
      RpcService.call('something', 'param1', 'param2');

      expect($window.postMessage).toHaveBeenCalledWith(
        jasmine.objectContaining({
          type: 'brxm:request',
          id: jasmine.any(String),
          command: 'something',
          payload: ['param1', 'param2'],
        }),
        'http://localhost:3000',
      );
    });
  });

  describe('trigger', () => {
    it('should send an event', () => {
      RpcService.trigger('something', { a: 'b' });

      expect($window.postMessage).toHaveBeenCalledWith(
        jasmine.objectContaining({
          type: 'brxm:event',
          event: 'something',
          payload: { a: 'b' },
        }),
        'http://localhost:3000',
      );
    });
  });

  describe('initialize', () => {
    it('should emit an event', () => {
      const listener = jasmine.createSpy();
      $rootScope.$on('spa:something', listener);

      $window.dispatchEvent(new MessageEvent('message', {
        data: { type: 'brxm:event', event: 'something', payload: { a: 'b' } },
        origin: 'http://localhost:3000',
      }));
      $rootScope.$digest();

      expect(listener).toHaveBeenCalledWith(jasmine.anything(), { a: 'b' });
    });

    it('should resolve a call', (done) => {
      let id;
      $window.postMessage.and.callFake(({ id: messageId }) => { id = messageId; });

      const promise = RpcService.call('command');
      promise.then((value) => {
        expect(value).toBe('b');
        done();
      });

      $window.dispatchEvent(new MessageEvent('message', {
        data: {
          id: 'id',
          type: 'brxm:response',
          state: 'rejected',
          result: 'a',
        },
        origin: 'http://localhost:3000',
      }));
      $window.dispatchEvent(new MessageEvent('message', {
        data: {
          id,
          type: 'brxm:response',
          state: 'fulfilled',
          result: 'b',
        },
        origin: 'http://localhost:3000',
      }));

      $rootScope.$digest();
    });

    it('should reject a call', (done) => {
      let id;
      $window.postMessage.and.callFake(({ id: messageId }) => { id = messageId; });

      const promise = RpcService.call('command');
      promise.catch((value) => {
        expect(value).toBe('b');
        done();
      });

      $window.dispatchEvent(new MessageEvent('message', {
        data: {
          id: 'id',
          type: 'brxm:response',
          state: 'rejected',
          result: 'a',
        },
        origin: 'http://localhost:3000',
      }));
      $window.dispatchEvent(new MessageEvent('message', {
        data: {
          id,
          type: 'brxm:response',
          state: 'rejected',
          result: 'b',
        },
        origin: 'http://localhost:3000',
      }));

      $rootScope.$digest();
    });
  });

  describe('register', () => {
    it('should call a registered callback', () => {
      const callback = jasmine.createSpy();
      RpcService.register('command', callback);

      $window.dispatchEvent(new MessageEvent('message', {
        data: {
          id: 'id1',
          type: 'brxm:request',
          command: 'something',
          payload: ['a', 'b'],
        },
        origin: 'http://localhost:3000',
      }));
      $window.dispatchEvent(new MessageEvent('message', {
        data: {
          id: 'id2',
          type: 'brxm:request',
          command: 'command',
          payload: ['c', 'd'],
        },
        origin: 'http://localhost:3000',
      }));
      $rootScope.$digest();

      expect(callback).toHaveBeenCalledWith('c', 'd');
    });

    it('should send a fulfilled response', () => {
      const callback = () => 'something';
      RpcService.register('command', callback);

      $window.dispatchEvent(new MessageEvent('message', {
        data: {
          id: 'id1',
          type: 'brxm:request',
          command: 'command',
          payload: ['a', 'b'],
        },
        origin: 'http://localhost:3000',
      }));
      $rootScope.$digest();

      expect($window.postMessage).toHaveBeenCalledWith(
        jasmine.objectContaining({
          type: 'brxm:response',
          id: 'id1',
          state: 'fulfilled',
          result: 'something',
        }),
        'http://localhost:3000',
      );
    });

    it('should send a rejected response', () => {
      // eslint-disable-next-line no-throw-literal
      const callback = async () => { throw 'something'; };
      RpcService.register('command', callback);

      $window.dispatchEvent(new MessageEvent('message', {
        data: {
          id: 'id1',
          type: 'brxm:request',
          command: 'command',
          payload: ['a', 'b'],
        },
        origin: 'http://localhost:3000',
      }));
      $rootScope.$digest();

      expect($window.postMessage).toHaveBeenCalledWith(
        jasmine.objectContaining({
          type: 'brxm:response',
          id: 'id1',
          state: 'rejected',
          result: 'something',
        }),
        'http://localhost:3000',
      );
    });
  });

  describe('destroy', () => {
    it('should stop reacting on events', () => {
      const listener = jasmine.createSpy();
      $rootScope.$on('spa:something', listener);

      $window.dispatchEvent(new MessageEvent('message', {
        data: { type: 'brxm:event', event: 'something', payload: { a: 'b' } },
        origin: 'http://localhost:3000',
      }));
      $rootScope.$digest();

      expect(listener).not.toHaveBeenCalledWith();
    });
  });
});
