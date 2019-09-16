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

import { Typed } from 'emittery';
import { Events } from './events';
import { ComponentFactory, Component, Page, ContentFactory, Content, TYPE_COMPONENT } from './page';
import { Spa } from './spa';
import { PageModelUrlBuilder } from './url';

const model = {
  _meta: {},
  content: {
    someContent: { id: 'content-id', name: 'content-name' },
  },
  page: {
    id: 'page-id',
    type: TYPE_COMPONENT,
    _links: {
      componentRendering: { href: 'some-url' },
    },
  },
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
  let eventBus: Typed<Events>;
  let pageModelUrlBuilder: PageModelUrlBuilder;
  let spa: Spa;

  beforeEach(() => {
    componentFactory = new ComponentFactory();
    contentFactory = new ContentFactory(jest.fn());
    eventBus = new Typed<Events>();
    pageModelUrlBuilder = jest.fn(() => 'http://example.com');

    componentFactory.create = jest.fn(model => new Component(model));
    spyOn(contentFactory, 'create');

    spa = new Spa(pageModelUrlBuilder, componentFactory, contentFactory, eventBus);
  });

  describe('initialize', () => {
    let page: Page;

    beforeEach(async () => {
      config.httpClient.mockClear();
      spyOn(eventBus, 'on');
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

    it('should subscribe for cms.update event', () => {
      expect(eventBus.on).toBeCalledWith('cms.update', expect.any(Function));
    });

    it('should reject a promise when fetching the page model fails', () => {
      const error = new Error('Failed to fetch page model data');
      config.httpClient.mockImplementationOnce(() => { throw error; });
      const promise = spa.initialize(config);

      expect.assertions(1);
      expect(promise).rejects.toBe(error);
    });
  });

  describe('onCmsUpdate', () => {
    beforeEach(async () => {
      spyOn(eventBus, 'emit');
      await spa.initialize(config);

      jest.clearAllMocks();
    });

    it('should not proceed if a component does not exist', async () => {
      await eventBus.emitSerial('cms.update', { id: 'some-component', properties: {} });

      expect(config.httpClient).not.toBeCalled();
      expect(eventBus.emit).not.toBeCalled();
    });

    describe('on component update', () => {
      beforeEach(() => eventBus.emitSerial('cms.update', { id: 'page-id', properties: { a: 'b' } }));

      it('should request a component model', () => {
        expect(config.httpClient).toBeCalledWith({
          url: 'some-url',
          method: 'post',
          data: { a: 'b' },
        });
      });

      it('should create a component', () => {
        expect(componentFactory.create).toBeCalledWith(model.page);
      });

      it('should create a content instance', () => {
        expect(contentFactory.create).toBeCalledWith(model.content.someContent);
      });

      it('should emit page.update event', () => {
        expect(eventBus.emit).toBeCalledWith('page.update', { page: expect.any(Page) });
      });
    });
  });
});
