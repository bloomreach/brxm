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
import { isMatched } from '../url';
import { PostMessage } from './post-message';

jest.mock('../url');

describe('PostMessage', () => {
  let postMessage: PostMessage;
  let processSpy: jasmine.Spy;

  beforeAll(() => {
    postMessage = new PostMessage();
    processSpy = spyOn(postMessage, 'process');
  });

  afterEach(() => {
    processSpy.calls.reset();
  });

  describe('initialize', () => {
    it('should not process a message if not initialized', async () => {
      window.postMessage({ some: 'data' }, '*');

      await new Promise(resolve => setTimeout(resolve, 0));

      expect(processSpy).not.toHaveBeenCalled();
    });

    it('should process a message', async () => {
      postMessage.initialize({ window, origin: 'http://example.com' });
      mocked(isMatched).mockReturnValueOnce(true);
      window.postMessage({ some: 'data' }, '*');

      await new Promise(resolve => setTimeout(resolve, 0));

      expect(processSpy).toHaveBeenCalledWith({ some: 'data' });
    });

    it('should process a message in case of wildcard origin', async () => {
      postMessage.initialize({ window, origin: '*' });
      mocked(isMatched).mockReturnValueOnce(true);
      window.postMessage({ some: 'data' }, '*');

      await new Promise(resolve => setTimeout(resolve, 0));

      expect(isMatched).toHaveBeenCalledWith(expect.anything(), '');
      expect(processSpy).toHaveBeenCalledWith({ some: 'data' });
    });

    it('should not process a message if the origin is not matching', async () => {
      postMessage.initialize({ window, origin: 'http://example.com' });
      mocked(isMatched).mockReturnValueOnce(false);
      window.postMessage({ some: 'data' }, '*');

      await new Promise(resolve => setTimeout(resolve, 0));

      expect(processSpy).not.toHaveBeenCalledWith({ some: 'data' });
    });

    it('should not process a message without data', async () => {
      postMessage.initialize({ window, origin: 'http://example.com' });
      mocked(isMatched).mockReturnValueOnce(true);
      window.postMessage(undefined, '*');

      await new Promise(resolve => setTimeout(resolve, 0));

      expect(processSpy).not.toHaveBeenCalled();
    });
  });

  describe('send', () => {
    it('should send a message to a parent frame', () => {
      const postMessageSpy = spyOn(window.parent, 'postMessage');
      postMessage.initialize({ window, origin: 'http://example.com' });
      postMessage.call('command');

      expect(postMessageSpy).toHaveBeenCalledWith(
        expect.objectContaining({ command: 'command' }),
        'http://example.com',
      );
    });
  });
});
