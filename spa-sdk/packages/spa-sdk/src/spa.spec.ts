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
import { Events } from './events';
import { Cms } from './cms';
import {
  ComponentFactory,
  ComponentImpl,
  ContentFactory,
  ContentFactoryImpl,
  MetaFactory,
  PageImpl,
  Page,
  TYPE_COMPONENT,
} from './page';
import { Spa } from './spa';
import * as Url from './url';

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
    path: '/',
    headers: { Cookie: 'JSESSIONID=1234' },
  },
};

describe('Spa', () => {
  let cms: Cms;
  let componentFactory: ComponentFactory;
  let contentFactory: ContentFactory;
  let eventBus: Typed<Events>;
  let metaFactory: MetaFactory;
  let spa: Spa;
  let urlBuilder: Url.UrlBuilder;
  let urlBuilderImpl: jasmine.Spy;

  beforeEach(() => {
    eventBus = new Typed<Events>();
    cms = new Cms(eventBus);
    componentFactory = new ComponentFactory();
    contentFactory = new ContentFactoryImpl(jest.fn());
    metaFactory = new MetaFactory();
    urlBuilder = { getApiUrl: jest.fn(() => 'http://example.com') } as unknown as Url.UrlBuilder;
    urlBuilderImpl = spyOn(Url, 'UrlBuilderImpl').and.returnValue(urlBuilder);

    cms.initialize = jest.fn();
    componentFactory.create = jest.fn(model => new ComponentImpl(model));
    spyOn(contentFactory, 'create');

    spa = new Spa(componentFactory, contentFactory, metaFactory, eventBus, cms);
  });

  describe('initialize', () => {
    let page: Page;
    let on: jasmine.Spy;

    beforeEach(async () => {
      config.httpClient.mockClear();
      on = spyOn(eventBus, 'on');
      page = await spa.initialize(config);
    });

    it('should initialize a CMS integration', () => {
      expect(cms.initialize).toBeCalled();
    });

    it('should use a preview configuration', async () => {
      mocked(Url.isPreview).mockReturnValueOnce(true);
      await spa.initialize(config);

      expect(urlBuilderImpl).toBeCalledTimes(2);
      expect(urlBuilderImpl).nthCalledWith(1, config.options.live);
      expect(urlBuilderImpl).nthCalledWith(2, config.options.preview);
    });

    it('should generate a URL', () => {
      expect(urlBuilder.getApiUrl).toBeCalledWith(config.request.path);
    });

    it('should request a page model', () => {
      expect(config.httpClient).toBeCalledWith({
        url: 'http://example.com',
        method: 'GET',
        headers: config.request.headers,
      });
    });

    it('should create a root component', () => {
      expect(componentFactory.create).toBeCalledWith(model.page);
    });

    it('should return a page instance', () => {
      expect(page).toBeInstanceOf(PageImpl);
    });

    it('should create a content instance', () => {
      expect(contentFactory.create).toBeCalledWith(model.content.someContent);
    });

    it('should subscribe for cms.update event', () => {
      expect(eventBus.on).toBeCalledWith('cms.update', expect.any(Function));
    });

    it('should not subscribe for cms.update event twice', async () => {
      on.calls.reset();
      await spa.initialize(config);

      expect(eventBus.on).not.toBeCalledWith('cms.update', expect.anything());
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
          method: 'POST',
          data: 'a=b',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
          },
        });
      });

      it('should create a component', () => {
        expect(componentFactory.create).toBeCalledWith(model.page);
      });

      it('should create a content instance', () => {
        expect(contentFactory.create).toBeCalledWith(model.content.someContent);
      });

      it('should emit page.update event', () => {
        expect(eventBus.emit).toBeCalledWith('page.update', { page: expect.any(PageImpl) });
      });
    });
  });

  describe('destroy', () => {
    it('should not emit page.update event after destroy', async () => {
      const page = await spa.initialize(config);
      spa.destroy(page);

      spyOn(eventBus, 'emit');
      await eventBus.emitSerial('cms.update', { id: 'page-id', properties: { a: 'b' } });

      expect(eventBus.emit).not.toBeCalledWith('page.update', expect.anything());
    });

    it('should unsubscribe from cms.update event', async () => {
      spyOn(eventBus, 'off');

      const page = await spa.initialize(config);
      spa.destroy(page);

      expect(eventBus.off).toBeCalledWith('cms.update', expect.any(Function));
    });

    it('should not unsubscribe from cms.update event when there are pages left', async () => {
      spyOn(eventBus, 'off');

      const page1 = await spa.initialize(config);
      const page2 = await spa.initialize(config);
      spa.destroy(page1);

      expect(eventBus.off).not.toBeCalledWith('cms.update', expect.anything());
    });
  });
});
