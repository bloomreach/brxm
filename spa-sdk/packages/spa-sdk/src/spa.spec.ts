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

import { Spa } from './spa';
import { ComponentFactory, Component, Page, ContentFactory, ContentMap, MetaFactory } from './page';
import { PageModelUrlBuilder } from './url';

const model = {
  _meta: {},
  content: {
    someContent: { id: 'content-id', name: 'content-name' },
  },
  page: {},
};
const config = {
  httpClient: jest.fn(async () => model),
  options: {
    live: {
      pageModelBaseUrl: 'http://localhost:8080/site/my-spa',
    },
    preview: {
      pageModelBaseUrl: 'http://localhost:8080/site/_cmsinternal/my-spa',
    },
  },
  request: {
    path: '/',
    headers: { Cookie: 'JSESSIONID=1234' },
  },
};

describe('Spa', () => {
  let componentFactory: ComponentFactory;
  let contentFactory: ContentFactory;
  let content: ContentMap;
  let metaFactory: MetaFactory;
  let pageModelUrlBuilder: PageModelUrlBuilder;
  let spa: Spa;

  beforeEach(() => {
    componentFactory = new ComponentFactory();
    contentFactory = new ContentFactory(jest.fn());
    content = new Map();
    metaFactory = new MetaFactory();
    pageModelUrlBuilder = jest.fn(() => 'http://example.com');

    componentFactory.create = jest.fn(model => new Component(model));
    metaFactory.create = jest.fn(() => []);
    spyOn(contentFactory, 'create');

    spa = new Spa(pageModelUrlBuilder, componentFactory, contentFactory, metaFactory, content);
  });

  describe('initialize', () => {
    let page: Page;

    beforeEach(async () => {
      config.httpClient.mockClear();
      page = await spa.initialize(config);
    });

    it('should generate a URL', () => {
      expect(pageModelUrlBuilder).toBeCalledWith(config.request, config.options);
    });

    it('should request a page model', () => {
      expect(config.httpClient).toBeCalledWith({
        url: 'http://example.com',
        method: 'get',
        headers: config.request.headers,
      });
    });

    it('should create a root component', () => {
      expect(componentFactory.create).toBeCalledWith(model.page);
    });

    it('should return a page instance', () => {
      expect(page).toBeInstanceOf(Page);
    });

    it('should create a content instance', () => {
      expect(contentFactory.create).toBeCalledWith(model.content.someContent);
    });

    it('should create a meta collection', () => {
      expect(metaFactory.create).toBeCalledWith(model._meta);
    });

    it('should reject a promise when fetching the page model fails', () => {
      const error = new Error('Failed to fetch page model data');
      config.httpClient.mockImplementation(() => { throw error; });
      const promise = spa.initialize(config);

      expect.assertions(1);
      expect(promise).rejects.toBe(error);
    });
  });
});
