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
import { Component, TYPE_COMPONENT } from './component';
import { ContentModel, Content } from './content';
import { Events } from '../events';
import { Factory } from './factory';
import { LinkRewriter } from './link-rewriter';
import { Link, TYPE_LINK_INTERNAL } from './link';
import { MetaCollectionModel, Meta } from './meta';
import { PageImpl, PageModel, Page, isPage } from './page';

let content: Content;
let contentFactory: jest.Mocked<Factory<[ContentModel], Content>>;
let eventBus: Typed<Events>;
let linkFactory: jest.Mocked<Factory<[Link], string>>;
let linkRewriter: jest.Mocked<LinkRewriter>;
let metaFactory: jest.Mocked<Factory<[MetaCollectionModel], Meta[]>>;
let root: Component;

function createPage(model: PageModel) {
  return new PageImpl(model, root, contentFactory, eventBus, linkFactory, linkRewriter, metaFactory);
}

beforeEach(() => {
  content = {} as jest.Mocked<Content>;
  contentFactory = { create: jest.fn() };
  eventBus = new Typed<Events>();
  linkFactory = { create: jest.fn() };
  linkRewriter = { rewrite: jest.fn() } as unknown as jest.Mocked<LinkRewriter>;
  metaFactory = { create: jest.fn() };
  root = { getComponent: jest.fn() } as unknown as jest.Mocked<Component>;

  contentFactory.create.mockReturnValue(content);
});

describe('PageImpl', () => {
  describe('getComponent', () => {
    it('should forward a call to the root component', () => {
      const page = createPage({ page: { id: 'id', type: TYPE_COMPONENT } });
      page.getComponent('a', 'b');

      expect(root.getComponent).toBeCalledWith('a', 'b');
    });
  });

  describe('getContent', () => {
    let page: Page;

    beforeEach(() => {
      page = createPage({
        content: {
          content1: { id: 'id1', name: 'content1' },
          content2: { id: 'id2', name: 'content2' },
        },
        page: { id: 'id', type: TYPE_COMPONENT },
      });
    });

    it('should create content instances', () => {
      expect(contentFactory.create).toBeCalledTimes(2);
      expect(contentFactory.create).nthCalledWith(1, { id: 'id1', name: 'content1' });
      expect(contentFactory.create).nthCalledWith(2, { id: 'id2', name: 'content2' });
    });

    it('should return a content item', () => {
      expect(page.getContent('content1')).toBe(content);
    });
  });

  describe('getMeta', () => {
    it('should delegate to the MetaFactory to create new meta', () => {
      const metaFactoryCreateSpy = jest.spyOn(metaFactory, 'create');
      const page = createPage({ page: { id: 'id', type: TYPE_COMPONENT } });

      const metaCollectionModel = {} as MetaCollectionModel;
      page.getMeta(metaCollectionModel);

      expect(metaFactoryCreateSpy).toHaveBeenCalledWith(metaCollectionModel);
    });
  });

  describe('getTitle', () => {
    it('should return a page title', () => {
      const page = createPage({
        page: {
          id: 'id',
          type: TYPE_COMPONENT,
          _meta: { pageTitle: 'something' },
        },
      });

      expect(page.getTitle()).toBe('something');
    });

    it('should return an undefined value', () => {
      const page1 = createPage({ page: { id: 'id', type: TYPE_COMPONENT, _meta: {} } });
      const page2 = createPage({ page: { id: 'id', type: TYPE_COMPONENT } });

      expect(page1.getTitle()).toBeUndefined();
      expect(page2.getTitle()).toBeUndefined();
    });
  });

  describe('getUrl', () => {
    beforeEach(() => {
      linkFactory.create.mockReturnValueOnce('url');
    });

    it('should pass a link to the link factory', () => {
      const link = { href: '' };
      const page = createPage({ page: { id: 'id', type: TYPE_COMPONENT } });

      expect(page.getUrl(link)).toBe('url');
      expect(linkFactory.create).toBeCalledWith(link);
    });

    it('should pass the current page link', () => {
      const page = createPage({
        page: { id: 'id', type: TYPE_COMPONENT },
        _links: {
          site: { href: 'site-url' },
          self: { href: 'self-url' },
        },
      });

      expect(page.getUrl()).toBe('url');
      expect(linkFactory.create).toBeCalledWith({ href: 'site-url', type: TYPE_LINK_INTERNAL });
    });

    it('should pass empty string', () => {
      const page = createPage({ page: { id: 'id', type: TYPE_COMPONENT } });

      expect(page.getUrl()).toBe('url');
      expect(linkFactory.create).toBeCalledWith('');
    });
  });

  describe('getVisitor', () => {
    it('should return a visitor', () => {
      const page = createPage({
        page: { id: 'id', type: TYPE_COMPONENT },
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
      const page1 = createPage({ page: { id: 'id', type: TYPE_COMPONENT, _meta: {} } });
      const page2 = createPage({ page: { id: 'id', type: TYPE_COMPONENT } });

      expect(page1.getVisitor()).toBeUndefined();
      expect(page2.getVisitor()).toBeUndefined();
    });
  });

  describe('getVisit', () => {
    it('should return a visit', () => {
      const page = createPage({
        page: { id: 'id', type: TYPE_COMPONENT },
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
      const page1 = createPage({ page: { id: 'id', type: TYPE_COMPONENT, _meta: {} } });
      const page2 = createPage({ page: { id: 'id', type: TYPE_COMPONENT } });

      expect(page1.getVisit()).toBeUndefined();
      expect(page2.getVisit()).toBeUndefined();
    });
  });

  describe('isPreview', () => {
    it('should return true', () => {
      const page = createPage({
        page: { id: 'id', type: TYPE_COMPONENT },
        _meta: { preview: true },
      });

      expect(page.isPreview()).toBe(true);
    });

    it('should return false', () => {
      const page1 = createPage({ page: { id: 'id', type: TYPE_COMPONENT, _meta: {} } });
      const page2 = createPage({ page: { id: 'id', type: TYPE_COMPONENT } });

      expect(page1.isPreview()).toBe(false);
      expect(page2.isPreview()).toBe(false);
    });
  });

  describe('onPageUpdate', () => {
    it('should update content on page.update event', async () => {
      const page = createPage({ page: { id: 'id', type: TYPE_COMPONENT } });

      expect(page.getContent('content')).toBeUndefined();

      await eventBus.emitSerial('page.update', { page: {
        content: {
          content: { id: 'id', name: 'content' },
        },
        page: { id: 'id', type: TYPE_COMPONENT },
      } });

      expect(contentFactory.create).toBeCalledWith({ id: 'id', name: 'content' });
      expect(page.getContent('content')).toBe(content);
    });
  });

  describe('rewriteLinks', () => {
    it('should pass a call to the link rewriter', () => {
      linkRewriter.rewrite.mockReturnValueOnce('rewritten');

      const page = createPage({ page: { id: 'id', type: TYPE_COMPONENT } });

      expect(page.rewriteLinks('something', 'text/html')).toBe('rewritten');
      expect(linkRewriter.rewrite).toBeCalledWith('something', 'text/html');
    });
  });

  describe('sync', () => {
    it('should emit page.ready event', () => {
      spyOn(eventBus, 'emit');

      const page = createPage({ page: { id: 'id', type: TYPE_COMPONENT } });
      page.sync();

      expect(eventBus.emit).toBeCalledWith('page.ready', {});
    });
  });

  describe('toJSON', () => {
    it('should return a page model', () => {
      const model = { page: { id: 'id', type: TYPE_COMPONENT } } as PageModel;
      const page = createPage(model);

      expect(page.toJSON()).toBe(model);
    });
  });
});

describe('isPage', () => {
  it('should return true', () => {
    const page = createPage({ page: { id: 'id', type: TYPE_COMPONENT } });

    expect(isPage(page)).toBe(true);
  });

  it('should return false', () => {
    expect(isPage(undefined)).toBe(false);
    expect(isPage({})).toBe(false);
  });
});
