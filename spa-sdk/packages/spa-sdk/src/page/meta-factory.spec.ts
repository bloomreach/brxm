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

import { MetaFactory } from './meta-factory';
import { MetaImpl, MetaType, META_POSITION_BEGIN, META_POSITION_END, TYPE_META_COMMENT } from './meta';

describe('MetaFactory', () => {
  describe('create', () => {
    const builder1 = jest.fn((model, position) => new MetaImpl(model, position));
    const builder2 = jest.fn((model, position) => new MetaImpl(model, position));
    const factory = new MetaFactory()
      .register('type1' as MetaType, builder1)
      .register('type2' as MetaType, builder2);

    beforeEach(() => {
      builder1.mockClear();
      builder2.mockClear();
    });

    it('should call a registered builder', () => {
      factory.create({ data: 'meta1', type: 'type1' as MetaType }, META_POSITION_BEGIN);
      factory.create({ data: 'meta2', type: 'type2' as MetaType }, META_POSITION_END);

      expect(builder1).toBeCalledWith({ data: 'meta1', type: 'type1' }, META_POSITION_BEGIN);
      expect(builder2).toBeCalledWith({ data: 'meta2', type: 'type2' }, META_POSITION_END);
    });

    it('should throw an exception on unknown component type', () => {
      expect(() => factory.create({ data: 'data0', type: 'type0' as MetaType }, META_POSITION_BEGIN)).toThrowError();
    });
  });
});
