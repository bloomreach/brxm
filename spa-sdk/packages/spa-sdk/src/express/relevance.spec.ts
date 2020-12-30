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
import { parse, serialize } from 'cookie';
import { IncomingMessage, OutgoingMessage } from 'http';

import { Page } from '..';
import relevance from './relevance';

jest.mock('cookie');

describe('relevance', () => {
  describe('request handling', () => {
    it('should parse current visitor from cookies', () => {
      mocked(parse).mockReturnValueOnce({ visitor: JSON.stringify({ id: 'some-id', header: 'visitor-header' }) });
      const request: Partial<IncomingMessage> = { headers: { cookie: 'something' } };
      const response = {};

      relevance.withOptions({ name: 'visitor' })(request, response);

      expect(parse).toBeCalledWith('something');
      expect(request.visitor).toEqual({ id: 'some-id', header: 'visitor-header' });
    });

    it('should not fail on invalid JSON', () => {
      mocked(parse).mockReturnValueOnce({ visitor: 'invalid' });
      const request: Partial<IncomingMessage> = {};
      const response = {};

      expect(() => relevance.withOptions({ name: 'visitor' })(request, response)).not.toThrow();
      expect(request.visitor).toBeUndefined();
    });

    it('should not fail on empty visitor', () => {
      mocked(parse).mockReturnValueOnce({});
      const request: Partial<IncomingMessage> = {};
      const response = {};

      expect(() => relevance.withOptions({ name: 'visitor' })(request, response)).not.toThrow();
      expect(request.visitor).toBeUndefined();
    });
  });

  describe('response handling', () => {
    let page: jest.Mocked<Page>;
    let request: jest.Mocked<Pick<IncomingMessage, 'once'>>;
    let response: jest.Mocked<Pick<OutgoingMessage, 'getHeader' | 'setHeader'>>;

    beforeEach(() => {
      page = { getVisitor: jest.fn() } as unknown as typeof page;
      request = {
        once: jest.fn((event, listener) => { listener(page); return request; }),
      } as unknown as typeof request;
      response = { getHeader: jest.fn(), setHeader: jest.fn() };

      mocked(parse).mockReturnValueOnce({});
    });

    it('should not proceed if there is no visitor', () => {
      relevance(request, response);

      expect(response.setHeader).not.toBeCalled();
    });

    it('should serialize returned visitor', () => {
      mocked(serialize).mockReturnValueOnce('visitor=something');
      page.getVisitor.mockReturnValueOnce({ id: 'some-id', header: 'some-header', new: true });

      relevance.withOptions({
        domain: 'example.com',
        httpOnly: true,
        maxAge: 1000,
        name: 'visitor',
      })(request, response);

      expect(serialize).toBeCalledWith(
        'visitor',
        JSON.stringify({ id: 'some-id', header: 'some-header' }),
        expect.objectContaining({
          domain: 'example.com',
          httpOnly: true,
          maxAge: 1000,
        }),
      );
      expect(response.setHeader).toBeCalledWith('Set-Cookie', ['visitor=something']);
    });

    it('should append to other Set-Cookie headers', () => {
      mocked(serialize).mockReturnValueOnce('visitor=something');
      page.getVisitor.mockReturnValueOnce({ id: 'some-id', header: 'some-header', new: true });
      response.getHeader.mockReturnValueOnce(['cookie=value']);

      relevance(request, response);

      expect(response.setHeader).toBeCalledWith('Set-Cookie', ['cookie=value', 'visitor=something']);
    });

    it('should append to another Set-Cookie header', () => {
      mocked(serialize).mockReturnValueOnce('visitor=something');
      page.getVisitor.mockReturnValueOnce({ id: 'some-id', header: 'some-header', new: true });
      response.getHeader.mockReturnValueOnce('cookie=value');

      relevance(request, response);

      expect(response.setHeader).toBeCalledWith('Set-Cookie', ['cookie=value', 'visitor=something']);
    });
  });

  it('should call the next request handler', () => {
    mocked(parse).mockReturnValueOnce({});

    const next = jest.fn();
    relevance({}, {}, next);

    expect(next).toBeCalled();
  });
});
