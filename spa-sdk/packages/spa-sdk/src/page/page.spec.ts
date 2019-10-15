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
import { ComponentImpl, Component, TYPE_COMPONENT } from './component';
import { ContentMap } from './content-map';
import { ContentImpl } from './content';
import { Events } from '../events';
import { MetaCollectionModel } from './meta';
import { MetaFactory } from './meta-factory';
import { PageImpl, Page } from './page';

describe('PageImpl', () => {
  let content: ContentMap;
  let eventBus: Typed<Events>;
  let metaFactory: MetaFactory;
  let root: Component;

  beforeEach(() => {
    content = new Map();
    eventBus = new Typed<Events>();
    metaFactory = new MetaFactory();
    root = new ComponentImpl({ id: 'id', type: TYPE_COMPONENT });

    jest.spyOn(root, 'getComponent');
    jest.spyOn(content, 'get');
  });

  describe('getComponent', () => {
    it('should forward a call to the root component', () => {
      const page = new PageImpl({ page: { id: 'id', type: TYPE_COMPONENT } }, root, content, eventBus, metaFactory);
      page.getComponent('a', 'b');

      expect(root.getComponent).toBeCalledWith('a', 'b');
    });
  });

  describe('getContent', () => {
    let page: Page;

    beforeEach(() => {
      page = new PageImpl({ page: { id: 'id', type: TYPE_COMPONENT } }, root, content, eventBus, metaFactory);
    });

    it('should resolve a reference', () => {
      page.getContent({ $ref: '/content/some-content' });

      expect(content.get).toBeCalledWith('some-content');
    });

    it('should pass a string reference directly', () => {
      page.getContent('content-reference');

      expect(content.get).toBeCalledWith('content-reference');
    });

    it('should return a content item', () => {
      const someContent = new ContentImpl({ id: 'some-id', name: 'some-name' });
      content.set('some-content', someContent);

      expect(page.getContent('some-content')).toBe(someContent);
    });
  });

  describe('getMeta', () => {
    it('should delegate to the MetaFactory to create new meta', () => {
      const metaFactoryCreateSpy = jest.spyOn(metaFactory, 'create');
      const page = new PageImpl({ page: { id: 'id', type: TYPE_COMPONENT } }, root, content, eventBus, metaFactory);

      const metaCollectionModel = {} as MetaCollectionModel;
      page.getMeta(metaCollectionModel);

      expect(metaFactoryCreateSpy).toHaveBeenCalledWith(metaCollectionModel);
    });
  });

  describe('getTitle', () => {
    it('should return a page title', () => {
      const page = new PageImpl(
        {
          page: {
            id: 'id',
            type: TYPE_COMPONENT,
            _meta: { pageTitle: 'something' },
          },
        },
        root,
        content,
        eventBus,
        metaFactory,
      );

      expect(page.getTitle()).toBe('something');
    });

    it('should return an undefined value', () => {
      const page1 = new PageImpl(
        { page: { id: 'id', type: TYPE_COMPONENT, _meta: {} } },
        root,
        content,
        eventBus,
        metaFactory,
      );
      const page2 = new PageImpl({ page: { id: 'id', type: TYPE_COMPONENT } }, root, content, eventBus, metaFactory);

      expect(page1.getTitle()).toBeUndefined();
      expect(page2.getTitle()).toBeUndefined();
    });
  });

  describe('isPreview', () => {
    it('should return true', () => {
      const page = new PageImpl(
        {
          page: { id: 'id', type: TYPE_COMPONENT },
          _meta: { preview: true },
        },
        root,
        content,
        eventBus,
        metaFactory,
      );

      expect(page.isPreview()).toBe(true);
    });

    it('should return false', () => {
      const page1 = new PageImpl(
        {
          page: {
            id: 'id',
            type: TYPE_COMPONENT,
            _meta: {},
          },
        },
        root,
        content,
        eventBus,
        metaFactory,
      );
      const page2 = new PageImpl({ page: { id: 'id', type: TYPE_COMPONENT } }, root, content, eventBus, metaFactory);

      expect(page1.isPreview()).toBe(false);
      expect(page2.isPreview()).toBe(false);
    });
  });

  describe('onPageUpdate', () => {
    it('should update content on page.update event', async () => {
      const model = { page: { id: 'id', type: TYPE_COMPONENT } };
      const page = new PageImpl(model, root, content, eventBus, metaFactory);
      const someContent = new ContentImpl({ id: 'some-id', name: 'some-name' });

      expect(page.getContent('some-content')).toBeUndefined();
      await eventBus.emitSerial('page.update', {
        page: new PageImpl(model, root, new Map([['some-content', someContent]]), eventBus, metaFactory),
      });
      expect(page.getContent('some-content')).toBe(someContent);
    });
  });

  describe('sync', () => {
    it('should emit page.ready event', () => {
      spyOn(eventBus, 'emit');

      const page = new PageImpl({ page: { id: 'id', type: TYPE_COMPONENT } }, root, content, eventBus, metaFactory);
      page.sync();

      expect(eventBus.emit).toBeCalledWith('page.ready', {});
    });
  });
});
