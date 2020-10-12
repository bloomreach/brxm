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

import { Typed } from 'emittery';
import { Component } from './component';
import { ComponentFactory } from './component-factory09';
import { ContentFactory } from './content-factory09';
import { ContentModel, Content } from './content09';
import { EventBus as CmsEventBus } from '../cms';
import { EventBus } from './events09';
import { LinkFactory } from './link-factory';
import { LinkRewriter } from './link-rewriter';
import { TYPE_COMPONENT } from './component09';
import { TYPE_LINK_INTERNAL } from './link';
import { MetaCollectionFactory } from './meta-collection-factory';
import { PageImpl, PageModel, isPage } from './page09';
import { Page } from './page';

let componentFactory: jest.Mocked<ComponentFactory>;
let content: Content;
let contentFactory: jest.MockedFunction<ContentFactory>;
let cmsEventBus: CmsEventBus;
let eventBus: EventBus;
let linkFactory: jest.Mocked<LinkFactory>;
let linkRewriter: jest.Mocked<LinkRewriter>;
let metaFactory: jest.MockedFunction<MetaCollectionFactory>;
let root: Component;

const model = {
  _links: {
    self: { href: 'self-url' },
    site: { href: 'site-url' },
  },
  _meta: {},
  page: { _meta: {}, id: 'id', type: TYPE_COMPONENT },
} as PageModel;

function createPage(pageModel = model) {
  return new PageImpl(
    pageModel,
    componentFactory,
    contentFactory,
    cmsEventBus,
    eventBus,
    linkFactory,
    linkRewriter,
    metaFactory,
  );
}

beforeEach(() => {
  componentFactory = { create: jest.fn(() => root) } as unknown as typeof componentFactory;
  content = {} as jest.Mocked<Content>;
  contentFactory = jest.fn(() => content) as unknown as typeof contentFactory;
  cmsEventBus = new Typed();
  eventBus = new Typed();
  linkFactory = { create: jest.fn() } as unknown as typeof linkFactory;
  linkRewriter = { rewrite: jest.fn() } as unknown as jest.Mocked<LinkRewriter>;
  metaFactory = jest.fn();
  root = { getComponent: jest.fn() } as unknown as jest.Mocked<Component>;
});

describe('PageImpl', () => {
  describe('getComponent', () => {
    it('should forward a call to the root component', () => {
      const page = createPage();
      page.getComponent('a', 'b');

      expect(root.getComponent).toBeCalledWith('a', 'b');
    });
  });

  describe('getContent', () => {
    let page: Page;

    beforeEach(() => {
      page = createPage({
        ...model,
        content: {
          content1: { id: 'id1', name: 'content1' } as ContentModel,
          content2: { id: 'id2', name: 'content2' } as ContentModel,
        },
      });
    });

    it('should create content instances', () => {
      expect(contentFactory).toBeCalledTimes(2);
      expect(contentFactory).nthCalledWith(1, { id: 'id1', name: 'content1' });
      expect(contentFactory).nthCalledWith(2, { id: 'id2', name: 'content2' });
    });

    it('should return a content item', () => {
      expect(page.getContent('content1')).toBe(content);
    });
  });

  describe('getDocument', () => {
    it('should throw an error', () => {
      const page = createPage();

      expect(() => page.getDocument()).toThrowError();
    });
  });

  describe('getMeta', () => {
    it('should delegate to the MetaFactory to create new meta', () => {
      const page = createPage();
      const model = {};

      page.getMeta(model);

      expect(metaFactory).toHaveBeenCalledWith(model);
    });
  });

  describe('getTitle', () => {
    it('should return a page title', () => {
      const page = createPage({
        ...model,
        page: {
          ...model.page,
          _meta: { pageTitle: 'something' },
        },
      });

      expect(page.getTitle()).toBe('something');
    });

    it('should return an undefined value', () => {
      const page = createPage();

      expect(page.getTitle()).toBeUndefined();
    });
  });

  describe('getUrl', () => {
    beforeEach(() => {
      linkFactory.create.mockReturnValueOnce('url');
    });

    it('should pass a link to the link factory', () => {
      const link = { href: '' };
      const page = createPage();

      expect(page.getUrl(link)).toBe('url');
      expect(linkFactory.create).toBeCalledWith(link);
    });

    it('should pass the current page link', () => {
      const page = createPage();

      expect(page.getUrl()).toBe('url');
      expect(linkFactory.create).toBeCalledWith({ href: 'site-url', type: TYPE_LINK_INTERNAL });
    });
  });

  describe('getVersion', () => {
    it('should return a version', () => {
      const page = createPage({
        ...model,
        _meta: { version: '0.9' },
      });

      expect(page.getVersion()).toEqual('0.9');
    });

    it('should return an undefined value', () => {
      const page = createPage();

      expect(page.getVersion()).toBeUndefined();
    });
  });

  describe('getVisitor', () => {
    it('should return a visitor', () => {
      const page = createPage({
        ...model,
        _meta: {
          visitor: {
            id: 'some-id',
            header: 'some-header',
            new: false,
          },
        },
      });

      expect(page.getVisitor()).toEqual({
        id: 'some-id',
        header: 'some-header',
        new: false,
      });
    });

    it('should return an undefined value', () => {
      const page = createPage();

      expect(page.getVisitor()).toBeUndefined();
    });
  });

  describe('getVisit', () => {
    it('should return a visit', () => {
      const page = createPage({
        ...model,
        _meta: {
          visit: {
            id: 'some-id',
            new: false,
          },
        },
      });

      expect(page.getVisit()).toEqual({
        id: 'some-id',
        new: false,
      });
    });

    it('should return an undefined value', () => {
      const page = createPage();

      expect(page.getVisit()).toBeUndefined();
    });
  });

  describe('isPreview', () => {
    it('should return true', () => {
      const page = createPage({ ...model, _meta: { preview: true } });

      expect(page.isPreview()).toBe(true);
    });

    it('should return false', () => {
      const page = createPage();

      expect(page.isPreview()).toBe(false);
    });
  });

  describe('onPageUpdate', () => {
    it('should update content on page.update event', async () => {
      const page = createPage();

      expect(page.getContent('content')).toBeUndefined();

      await eventBus.emitSerial('page.update', { page: {
        ...model,
        content: {
          content: { id: 'id', name: 'content' } as ContentModel,
        },
      } });

      expect(contentFactory).toBeCalledWith({ id: 'id', name: 'content' });
      expect(page.getContent('content')).toBe(content);
    });
  });

  describe('rewriteLinks', () => {
    it('should pass a call to the link rewriter', () => {
      linkRewriter.rewrite.mockReturnValueOnce('rewritten');

      const page = createPage();

      expect(page.rewriteLinks('something', 'text/html')).toBe('rewritten');
      expect(linkRewriter.rewrite).toBeCalledWith('something', 'text/html');
    });
  });

  describe('sync', () => {
    it('should emit page.ready event', () => {
      spyOn(cmsEventBus, 'emit');

      const page = createPage();
      page.sync();

      expect(cmsEventBus.emit).toBeCalledWith('page.ready', {});
    });
  });

  describe('toJSON', () => {
    it('should return a page model', () => {
      const page = createPage();

      expect(page.toJSON()).toBe(model);
    });
  });
});

describe('isPage', () => {
  it('should return true', () => {
    const page = createPage();

    expect(isPage(page)).toBe(true);
  });

  it('should return false', () => {
    expect(isPage(undefined)).toBe(false);
    expect(isPage({})).toBe(false);
  });
});
