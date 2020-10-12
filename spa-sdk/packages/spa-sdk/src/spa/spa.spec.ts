/*
 * Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
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
import { Component, EventBus, PageFactory, PageModel, Page, TYPE_COMPONENT } from '../page';
import { EventBus as CmsEventBus } from '../cms';
import { Api } from './api';
import { Spa } from './spa';

jest.mock('../url');

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
  httpClient: jest.fn(),
  request: { path: '/' },
};

describe('Spa', () => {
  let api: jest.Mocked<Api>;
  let cmsEventBus: CmsEventBus;
  let eventBus: EventBus;
  let page: jest.Mocked<Page>;
  let pageFactory: jest.MockedFunction<PageFactory>;
  let spa: Spa;

  beforeEach(() => {
    eventBus = new Typed();
    cmsEventBus = new Typed();
    api = {
      initialize: jest.fn(),
      getPage: jest.fn(async () => model),
      getComponent: jest.fn(() => model),
    } as unknown as jest.Mocked<Api>;
    page = {
      getComponent: jest.fn(),
      isPreview: jest.fn(),
    } as unknown as jest.Mocked<Page>;
    pageFactory = jest.fn();

    spyOn(cmsEventBus, 'on').and.callThrough();
    pageFactory.mockReturnValue(page);
    spa = new Spa(eventBus, api, pageFactory, cmsEventBus);
  });

  describe('initialize', () => {
    beforeEach(async () => await spa.initialize(config.request.path));

    it('should get page through an API', () => {
      expect(api.getPage).toBeCalledWith(config.request.path);
    });

    it('should use a page model from the arguments', () => {
      jest.clearAllMocks();
      const model = {} as PageModel;

      spa.initialize(model);
      expect(api.getPage).not.toBeCalled();
      expect(pageFactory).toBeCalledWith(model);
    });

    it('should create a page instance', () => {
      expect(pageFactory).toBeCalledWith(model);
    });

    it('should subscribe for cms.update event', async () => {
      page.isPreview.mockReturnValue(true);
      await spa.initialize(config.request.path);
      expect(cmsEventBus.on).toBeCalledWith('cms.update', expect.any(Function));
    });

    it('should reject a promise when fetching the page model fails', () => {
      const error = new Error('Failed to fetch page model data');
      api.getPage.mockImplementationOnce(async () => { throw error; });
      const promise = spa.initialize(config.request.path);

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
      page.isPreview.mockReturnValue(true);
      spyOn(cmsEventBus, 'emit');
      spyOn(eventBus, 'emit');
      await spa.initialize(config.request.path);

      jest.clearAllMocks();
    });

    it('should not proceed if a component does not exist', async () => {
      await cmsEventBus.emitSerial('cms.update', { id: 'some-component', properties: {} });

      expect(root.getComponentById).toBeCalledWith('some-component');
      expect(api.getComponent).not.toBeCalled();
      expect(eventBus.emit).not.toBeCalledWith('page.update', expect.anything());
    });

    describe('on component update', () => {
      beforeEach(async () => {
        root.getComponentById.mockReturnValue(component);
        component.getUrl.mockReturnValue('some-url');
        await cmsEventBus.emitSerial('cms.update', { id: 'page-id', properties: { a: 'b' } });
      });

      it('should request a component model', () => {
        expect(component.getUrl).toBeCalled();
        expect(api.getComponent).toBeCalledWith('some-url', { a: 'b' });
      });

      it('should emit page.update event', () => {
        expect(eventBus.emit).toBeCalledWith('page.update', { page: model });
      });
    });
  });

  describe('destroy', () => {
    beforeEach(() => spa.initialize(config.request.path));

    it('should unsubscribe from cms.update event', async () => {
      spyOn(cmsEventBus, 'off');

      spa.destroy();

      expect(cmsEventBus.off).toBeCalledWith('cms.update', expect.any(Function));
    });

    it('should remove all page events', async () => {
      spyOn(eventBus, 'clearListeners');

      spa.destroy();

      expect(eventBus.clearListeners).toBeCalled();
    });
  });
});
