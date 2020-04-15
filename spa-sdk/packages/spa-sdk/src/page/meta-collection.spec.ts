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

import { MetaFactory } from './meta-factory';
import { MetaCollectionImpl } from './meta-collection';
import { MetaType, Meta, META_POSITION_BEGIN, META_POSITION_END } from './meta';

describe('MetaCollectionImpl', () => {
  describe('constructor', () => {
    let factory: jest.Mocked<MetaFactory>;

    beforeEach(() => {
      factory = { create: jest.fn() } as unknown as jest.Mocked<MetaFactory>;
    });

    it('should extend a built-in array class', () => {
      const collection = new MetaCollectionImpl({}, factory);

      expect(collection).toBeInstanceOf(Array);
    });

    it('should call a factory to build meta-data', () => {
      new MetaCollectionImpl(
        {
          beginNodeSpan: [
            { data: 'meta1', type: 'type1' as MetaType },
            { data: 'meta2', type: 'type2' as MetaType },
          ],
        },
        factory,
      );

      expect(factory.create).toBeCalledWith({ data: 'meta1', type: 'type1' }, expect.anything());
      expect(factory.create).toBeCalledWith({ data: 'meta2', type: 'type2' }, expect.anything());
    });

    it('should pass a position of the meta', () => {
      new MetaCollectionImpl(
        {
          beginNodeSpan: [{ data: 'meta1', type: 'type1' as MetaType }],
          endNodeSpan: [{ data: 'meta2', type: 'type2' as MetaType }],
        },
        factory,
      );

      expect(factory.create).toBeCalledWith({ data: 'meta1', type: 'type1' }, META_POSITION_BEGIN);
      expect(factory.create).toBeCalledWith({ data: 'meta2', type: 'type2' }, META_POSITION_END);
    });

    it('should hold meta-data items', () => {
      const meta1 = { data: 'meta1' } as unknown as Meta;
      const meta2 = { data: 'meta2' } as unknown as Meta;

      factory.create.mockReturnValueOnce(meta1);
      factory.create.mockReturnValueOnce(meta2);

      const collection = new MetaCollectionImpl(
        {
          beginNodeSpan: [{ data: 'meta1', type: 'type1' as MetaType }],
          endNodeSpan: [{ data: 'meta2', type: 'type2' as MetaType }],
        },
        factory,
      );

      expect(collection).toMatchSnapshot();
    });
  });
});
