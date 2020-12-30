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
import { isConfigurationWithProxy } from '../configuration';
import { PageModel } from '../page';
import { UrlBuilder } from '../url';
import { ApiImpl } from './api';

jest.mock('../configuration');
jest.mock('../url');

const model = {} as PageModel;
const config = {
  httpClient: jest.fn(async () => ({ data: model })),
  request: {
    connection: {
      remoteAddress: '127.0.0.1',
    },
    headers: {
      cookie: 'JSESSIONID=1234',
      host: 'example.com',
      referer: 'http://example.com',
      'user-agent': 'Google Bot',
    },
    path: '/',
    visitor: {
      id: 'visitor-id',
      header: 'visitor-header',
    },
  },
};

describe('ApiImpl', () => {
  let api: ApiImpl;
  let urlBuilder: jest.Mocked<UrlBuilder>;

  beforeEach(async () => {
    urlBuilder = {
      initialize: jest.fn(),
      getApiUrl: jest.fn((path: string) => `http://example.com${path}`),
    } as unknown as jest.Mocked<UrlBuilder>;

    api = new ApiImpl(urlBuilder, config);
  });

  describe('getPage', () => {
    beforeEach(async () => await api.getPage(config.request.path));

    it('should generate a URL', () => {
      expect(urlBuilder.getApiUrl).toBeCalledWith(config.request.path);
    });

    it('should request a page model', () => {
      expect(config.httpClient).toBeCalledWith({
        url: 'http://example.com/',
        method: 'GET',
        headers: {
          referer: 'http://example.com',
          'user-agent': 'Google Bot',
          'visitor-header': 'visitor-id',
          'x-forwarded-for': '127.0.0.1',
        },
      });
    });

    it('should return a page model', async () => {
      expect(await api.getPage(config.request.path)).toBe(model);
    });

    it('should forward cookie header if the setup is using a reverse proxy', async () => {
      mocked(isConfigurationWithProxy).mockReturnValueOnce(true);

      const api = new ApiImpl(urlBuilder, config);
      await api.getPage(config.request.path);

      expect(config.httpClient).toBeCalledWith(expect.objectContaining({
        headers: expect.objectContaining({
          cookie: 'JSESSIONID=1234',
        }),
      }));
    });

    it('should not include x-forwarded-for header when the remote address could not be determined', async () => {
      const api = new ApiImpl(urlBuilder, { httpClient: config.httpClient, request: { path: config.request.path } });
      await api.getPage(config.request.path);

      expect(config.httpClient).toBeCalledWith(expect.not.objectContaining({
        headers: {
          'x-forwarded-for': expect.anything(),
        },
      }));
    });

    it('should not include visitor header when visitor configuration is not defined', async () => {
      const api = new ApiImpl(urlBuilder, { httpClient: config.httpClient, request: { path: config.request.path } });
      await api.getPage(config.request.path);

      expect(config.httpClient).toBeCalledWith(expect.not.objectContaining({
        headers: {
          'visitor-header': expect.anything(),
        },
      }));
    });

    it('should prefer visitor from the common config', async () => {
      const api = new ApiImpl(
        urlBuilder,
        { ...config, visitor: { header: 'custom-visitor-header', id: 'custom-visitor' } },
      );
      await api.getPage(config.request.path);

      expect(config.httpClient).toBeCalledWith(expect.not.objectContaining({
        headers: {
          'custom-visitor-header': 'custom-visitor',
        },
      }));
    });

    it('should not include API version header when the API version is not set', async () => {
      const api = new ApiImpl(urlBuilder, { httpClient: config.httpClient, request: { path: config.request.path } });
      await api.getPage(config.request.path);

      expect(config.httpClient).toBeCalledWith(expect.not.objectContaining({
        headers: {
          'Accept-Version': expect.anything(),
        },
      }));
    });

    it('should include a custom API version header', async () => {
      const api = new ApiImpl(urlBuilder, {
        apiVersionHeader: 'X-Version',
        apiVersion: 'version',
        httpClient: config.httpClient,
        request: { path: config.request.path },
      });
      await api.getPage(config.request.path);

      expect(config.httpClient).toBeCalledWith(expect.objectContaining({
        headers: {
          'X-Version': 'version',
        },
      }));
    });

    it('should fall back to the default API version header', async () => {
      const api = new ApiImpl(urlBuilder, {
        apiVersion: 'version',
        httpClient: config.httpClient,
        request: { path: config.request.path },
      });
      await api.getPage(config.request.path);

      expect(config.httpClient).toBeCalledWith(expect.objectContaining({
        headers: {
          'Accept-Version': 'version',
        },
      }));
    });

    it('should include a custom authorization header', async () => {
      const api = new ApiImpl(urlBuilder, {
        authorizationHeader: 'X-Auth',
        authorizationToken: 'token',
        httpClient: config.httpClient,
        request: { path: config.request.path },
      });
      await api.getPage(config.request.path);

      expect(config.httpClient).toBeCalledWith(expect.objectContaining({
        headers: {
          'X-Auth': 'Bearer token',
        },
      }));
    });

    it('should fall back to the default authorization header', async () => {
      const api = new ApiImpl(urlBuilder, {
        authorizationToken: 'token',
        httpClient: config.httpClient,
        request: { path: config.request.path },
      });
      await api.getPage(config.request.path);

      expect(config.httpClient).toBeCalledWith(expect.objectContaining({
        headers: {
          Authorization: 'Bearer token',
        },
      }));
    });

    it('should include a custom server-id header', async () => {
      const api = new ApiImpl(urlBuilder, {
        serverIdHeader: 'X-Custom-Server-Id',
        serverId: 'some',
        httpClient: config.httpClient,
        request: { path: config.request.path },
      });
      await api.getPage(config.request.path);

      expect(config.httpClient).toBeCalledWith(expect.objectContaining({
        headers: {
          'X-Custom-Server-Id': 'some',
        },
      }));
    });

    it('should fall back to the default server-id header', async () => {
      const api = new ApiImpl(urlBuilder, {
        serverId: 'some',
        httpClient: config.httpClient,
        request: { path: config.request.path },
      });
      await api.getPage(config.request.path);

      expect(config.httpClient).toBeCalledWith(expect.objectContaining({
        headers: {
          'Server-Id': 'some',
        },
      }));
    });
  });

  describe('getComponent', () => {
    beforeEach(async () => await api.getComponent('http://example.com/component', { a: 'b' }));

    it('should request a component model', () => {
      expect(config.httpClient).toBeCalledWith(expect.objectContaining({
        url: 'http://example.com/component',
        method: 'POST',
        data: 'a=b',
        headers: expect.objectContaining({
          'Content-Type': 'application/x-www-form-urlencoded',
        }),
      }));
    });

    it('should return a component model', async () => {
      expect(await api.getComponent('/', {})).toBe(model);
    });
  });
});
