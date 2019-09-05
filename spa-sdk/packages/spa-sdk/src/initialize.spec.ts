/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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

import { HttpClient } from './api';
import { initialize } from './initialize';
import { ComponentFactory, Component, Page } from './page';
import { PageModelUrlBuilder } from './url';

const model = {
  page: {},
};
const options = {
  live: {
    pageModelBaseUrl: 'http://localhost:8080/site/my-spa',
  },
  preview: {
    pageModelBaseUrl: 'http://localhost:8080/site/_cmsinternal/my-spa',
  },
};
const request = {
  path: '/',
  headers: { Cookie: 'JSESSIONID=1234' },
};

describe('initialize', () => {
  let factory: ComponentFactory;
  let httpClient: HttpClient;
  let modelUrlBuilder: PageModelUrlBuilder;

  beforeEach(() => {
    factory = new ComponentFactory();
    httpClient = jest.fn(async () => model);
    modelUrlBuilder = jest.fn(() => 'http://example.com');

    factory.create = jest.fn(model => new Component(model));
  });

  it('should generate a URL', async () => {
    await initialize(modelUrlBuilder, factory, { httpClient, options, request });

    expect(modelUrlBuilder).toBeCalledWith(request, options);
  });

  it('should request a page model', async () => {
    await initialize(modelUrlBuilder, factory, { httpClient, options, request });

    expect(httpClient).toBeCalledWith({
      url: 'http://example.com',
      method: 'get',
      headers: request.headers,
    });
  });

  it('should create a root component', async () => {
    await initialize(modelUrlBuilder, factory, { httpClient, options, request });

    expect(factory.create).toBeCalledWith(model.page);
  });

  it('should return a page instance', async () => {
    const page = await initialize(modelUrlBuilder, factory, { httpClient, options, request });

    expect(page).toBeInstanceOf(Page);
  });

  it('should reject a promise when fetching the page model fails', () => {
    const error = new Error('Failed to fetch page model data');
    const httpClient = () => { throw error; };
    const promise = initialize(modelUrlBuilder, factory, { httpClient, request, options });

    expect.assertions(1);
    expect(promise).rejects.toBe(error);
  });
});
