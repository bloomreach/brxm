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

import { isMetaCollection, MetaCollectionImpl, MetaCollectionModel } from './meta-collection';
import { MetaFactory } from './meta-factory';
import { MetaType, Meta, META_POSITION_BEGIN, META_POSITION_END, TYPE_META_COMMENT } from './meta';
import { MetaCommentImpl } from './meta-comment';

let factory: jest.Mocked<MetaFactory>;

beforeEach(() => {
  factory = { create: jest.fn() } as unknown as jest.Mocked<MetaFactory>;
});

function createMetaCollection(model = {} as MetaCollectionModel) {
  return new MetaCollectionImpl(model, factory);
}

describe('MetaCollectionImpl', () => {
  describe('constructor', () => {
    it('should extend a built-in array class', () => {
      const collection = createMetaCollection();

      expect(collection).toBeInstanceOf(Array);
    });

    it('should call a factory to build meta-data', () => {
      createMetaCollection({
        beginNodeSpan: [
          { data: 'meta1', type: 'type1' as MetaType },
          { data: 'meta2', type: 'type2' as MetaType },
        ],
      });

      expect(factory.create).toBeCalledWith({ data: 'meta1', type: 'type1' }, expect.anything());
      expect(factory.create).toBeCalledWith({ data: 'meta2', type: 'type2' }, expect.anything());
    });

    it('should pass a position of the meta', () => {
      createMetaCollection({
        beginNodeSpan: [{ data: 'meta1', type: 'type1' as MetaType }],
        endNodeSpan: [{ data: 'meta2', type: 'type2' as MetaType }],
      });

      expect(factory.create).toBeCalledWith({ data: 'meta1', type: 'type1' }, META_POSITION_BEGIN);
      expect(factory.create).toBeCalledWith({ data: 'meta2', type: 'type2' }, META_POSITION_END);
    });

    it('should hold meta-data items', () => {
      const meta1 = { data: 'meta1' } as unknown as Meta;
      const meta2 = { data: 'meta2' } as unknown as Meta;

      factory.create.mockReturnValueOnce(meta1);
      factory.create.mockReturnValueOnce(meta2);

      const collection = createMetaCollection({
        beginNodeSpan: [{ data: 'meta1', type: 'type1' as MetaType }],
        endNodeSpan: [{ data: 'meta2', type: 'type2' as MetaType }],
      });

      expect(collection).toMatchSnapshot();
    });
  });

  describe('clear', () => {
    let a: HTMLElement;
    let collection: MetaCollectionImpl;

    beforeEach(() => {
      collection = new MetaCollectionImpl(
        {
          beginNodeSpan: [{ data: 'meta1', type: TYPE_META_COMMENT }],
          endNodeSpan: [{ data: 'meta2', type: TYPE_META_COMMENT }],
        },
        new MetaFactory()
          .register(TYPE_META_COMMENT, (model, position) => new MetaCommentImpl(model, position))
      );

      a = document.createElement('a');

      document.body.append(a);
    });

    afterEach(() => {
      document.body.innerHTML = '';
    });

    it('should remove rendered comments', () => {
      collection.render(a, a);
      collection.clear();

      expect(document.body).toMatchSnapshot();
    });
  });

  describe('render', () => {
    let a: HTMLElement;
    let b: HTMLElement;
    let collection: MetaCollectionImpl;

    beforeEach(() => {
      collection = new MetaCollectionImpl(
        {
          beginNodeSpan: [
            { data: 'meta1', type: TYPE_META_COMMENT },
            { data: 'meta2', type: TYPE_META_COMMENT },
          ],
          endNodeSpan: [
            { data: 'meta3', type: TYPE_META_COMMENT },
            { data: 'meta4', type: TYPE_META_COMMENT },
          ],
        },
        new MetaFactory()
          .register(TYPE_META_COMMENT, (model, position) => new MetaCommentImpl(model, position)),
      );

      a = document.createElement('a');
      b = document.createElement('b');

      document.body.append(a, b);
    });

    afterEach(() => {
      document.body.innerHTML = '';
    });

    it('should not render on detached element', () => {
      document.body.removeChild(a);

      collection.render(a, a);

      expect(document.body).toMatchSnapshot();
    });

    it('should not render on document node', () => {
      const node = document.body.parentNode?.parentNode as Node;

      collection.render(node, node);

      expect(document.body).toMatchSnapshot();
    });

    it('should surround an element with comments', () => {
      collection.render(a, a);

      expect(document.body).toMatchSnapshot();
    });

    it('should surround multiple elements with comments', () => {
      collection.render(a, b);

      expect(document.body).toMatchSnapshot();
    });

    it('should return a callback clearing rendered comments', () => {
      const clear = collection.render(a, a);
      clear();

      expect(clear).toBeInstanceOf(Function);
      expect(document.body).toMatchSnapshot();
    });

    it('should return a callback clearing only rendered comments during the call', () => {
      const clear = collection.render(a, a);
      collection.render(b, b);
      clear();

      expect(document.body).toMatchSnapshot();
    });
  });
});

describe('isMetaCollection', () => {
  it('should return true', () => {
    const meta = createMetaCollection();

    expect(isMetaCollection(meta)).toBe(true);
  });

  it('should return false', () => {
    expect(isMetaCollection(undefined)).toBe(false);
    expect(isMetaCollection([])).toBe(false);
    expect(isMetaCollection({})).toBe(false);
  });
});
