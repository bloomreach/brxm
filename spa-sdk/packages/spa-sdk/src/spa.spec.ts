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
import { mocked } from 'ts-jest/utils';
import { Cms } from './cms';
import { Component, Factory, PageModel, Page, TYPE_COMPONENT } from './page';
import { Configuration, Spa } from './spa';
import { Events } from './events';
import { UrlBuilder, isMatched } from './url';

jest.mock('./url');

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
  httpClient: jest.fn(async () => ({ data: model })),
  options: {
    live: {
      cmsBaseUrl: 'http://localhost:8080/site/my-spa',
    },
    preview: {
      cmsBaseUrl: 'http://localhost:8080/site/_cmsinternal/my-spa',
    },
  },
  request: {
    connection: {
      remoteAddress: '127.0.0.1',
    },
    headers: {
      cookie: 'JSESSIONID=1234',
      host: 'example.com',
    },
    path: '/',
  },
};

describe('Spa', () => {
  let cms: jest.Mocked<Cms>;
  let eventBus: Typed<Events>;
  let page: jest.Mocked<Page>;
  let pageFactory: jest.Mocked<Factory<[PageModel], Page>>;
  let spa: Spa;
  let urlBuilder: jest.Mocked<UrlBuilder>;

  beforeEach(() => {
    eventBus = new Typed<Events>();
    cms = { initialize: jest.fn() } as unknown as jest.Mocked<Cms>;
    page = { getComponent: jest.fn() } as unknown as jest.Mocked<Page>;
    pageFactory = { create: jest.fn() };
    urlBuilder = {
      initialize: jest.fn(),
      getApiUrl: jest.fn(() => 'http://example.com'),
    } as unknown as jest.Mocked<UrlBuilder>;

    spyOn(eventBus, 'on').and.callThrough();
    pageFactory.create.mockReturnValue(page);
    spa = new Spa(config as Configuration, cms, eventBus, pageFactory, urlBuilder);
  });

  describe('initialize', () => {
    beforeEach(async () => await spa.initialize());

    it('should initialize a CMS integration', () => {
      expect(cms.initialize).toBeCalled();
    });

    it('should use a preview configuration', async () => {
      mocked(isMatched).mockReturnValueOnce(true);
      await spa.initialize();

      expect(urlBuilder.initialize).toBeCalledTimes(2);
      expect(urlBuilder.initialize).nthCalledWith(1, config.options.live);
      expect(urlBuilder.initialize).nthCalledWith(2, config.options.preview);
    });

    it('should generate a URL', () => {
      expect(urlBuilder.getApiUrl).toBeCalledWith(config.request.path);
    });

    it('should request a page model', () => {
      expect(config.httpClient).toBeCalledWith({
        url: 'http://example.com',
        method: 'GET',
        headers: {
          cookie: 'JSESSIONID=1234',
          'x-forwarded-for': '127.0.0.1',
        },
      });
    });

    it('should use a page model from the arguments', async () => {
      jest.clearAllMocks();
      const model = {} as PageModel;

      await spa.initialize(model);
      expect(config.httpClient).not.toBeCalled();
      expect(pageFactory.create).toBeCalledWith(model);
    });

    it('should create a page instance', () => {
      expect(pageFactory.create).toBeCalledWith(model);
    });

    it('should subscribe for cms.update event', () => {
      expect(eventBus.on).toBeCalledWith('cms.update', expect.any(Function));
    });

    it('should reject a promise when fetching the page model fails', () => {
      const error = new Error('Failed to fetch page model data');
      config.httpClient.mockImplementationOnce(() => { throw error; });
      const promise = spa.initialize();

      expect.assertions(1);
      expect(promise).rejects.toBe(error);
    });
  });

  describe('onCmsUpdate', () => {
    let component: jest.Mocked<Component>;
    let root: jest.Mocked<Component>;

    beforeEach(async () => {
      root = { getComponentById: jest.fn() } as unknown as jest.Mocked<Component>;
      component = { getUrl: jest.fn() } as unknown as jest.Mocked<Component>;

      page.getComponent.mockReturnValue(root);
      spyOn(eventBus, 'emit');
      await spa.initialize();

      jest.clearAllMocks();
    });

    it('should not proceed if a component does not exist', async () => {
      await eventBus.emitSerial('cms.update', { id: 'some-component', properties: {} });

      expect(root.getComponentById).toBeCalledWith('some-component');
      expect(config.httpClient).not.toBeCalled();
      expect(eventBus.emit).not.toBeCalled();
    });

    describe('on component update', () => {
      beforeEach(async () => {
        root.getComponentById.mockReturnValue(component);
        component.getUrl.mockReturnValue('some-url');
        await eventBus.emitSerial('cms.update', { id: 'page-id', properties: { a: 'b' } });
      });

      it('should request a component model', () => {
        expect(component.getUrl).toBeCalled();
        expect(config.httpClient).toBeCalledWith({
          url: 'some-url',
          method: 'POST',
          data: 'a=b',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
          },
        });
      });

      it('should emit page.update event', () => {
        expect(eventBus.emit).toBeCalledWith('page.update', { page: model });
      });
    });
  });

  describe('destroy', () => {
    it('should unsubscribe from cms.update event', async () => {
      spyOn(eventBus, 'off');

      await spa.initialize();
      spa.destroy();

      expect(eventBus.off).toBeCalledWith('cms.update', expect.any(Function));
    });
  });
});
