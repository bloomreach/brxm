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

import { MetaFactory } from './meta-factory';
import { Meta, META_POSITION_BEGIN, META_POSITION_END } from './meta';

describe('MetaFactory', () => {
  describe('register', () => {
    it('should provide a fluent interface', () => {
      const factory = new MetaFactory();

      expect(factory.register(
        'something',
        () => new Meta({ data: 'something', type: 'something' }, META_POSITION_BEGIN),
      )).toBe(factory);
    });
  });

  describe('create', () => {
    const builder1 = jest.fn((model, position) => new Meta(model, position));
    const builder2 = jest.fn((model, position) => new Meta(model, position));
    const factory = new MetaFactory()
      .register('type1', builder1)
      .register('type2', builder2);

    beforeEach(() => {
      builder1.mockClear();
      builder2.mockClear();
    });

    it('should return an empty array on no meta-data', () => {
      expect(factory.create({})).toEqual([]);
    });

    it('should call a registered builder', () => {
      factory.create({
        beginNodeSpan: [
          { data: 'meta1', type: 'type1' },
          { data: 'meta2', type: 'type2' },
        ],
      });

      expect(builder1).toBeCalledWith({ data: 'meta1', type: 'type1' }, META_POSITION_BEGIN);
      expect(builder2).toBeCalledWith({ data: 'meta2', type: 'type2' }, META_POSITION_BEGIN);
    });

    it('should throw an exception on unknown component type', () => {
      expect(() => factory.create({
        beginNodeSpan: [{ data: 'data0', type: 'type0' }],
      })).toThrowError();
    });

    it('should pass a position of the meta', () => {
      factory.create({
        beginNodeSpan: [{ data: 'meta1', type: 'type1' }],
        endNodeSpan: [{ data: 'meta2', type: 'type2' }],
      });

      expect(builder1).toBeCalledWith({ data: 'meta1', type: 'type1' }, META_POSITION_BEGIN);
      expect(builder2).toBeCalledWith({ data: 'meta2', type: 'type2' }, META_POSITION_END);
    });
  });
});
