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

import { ContentFactory } from './content-factory';
import { ContentModel } from './content';

describe('ContentFactory', () => {
  describe('create', () => {
    it('should call a registered builder', () => {
      const builder1 = jest.fn(() => 'content1');
      const builder2 = jest.fn(() => 'content2');
      const factory = new ContentFactory()
        .register('type1', builder1)
        .register('type2', builder2);

      factory.create({ id: 'id1', type: 'type1' } as ContentModel);
      factory.create({ id: 'id2', type: 'type2' } as ContentModel);

      expect(builder1).toBeCalledWith({ id: 'id1', type: 'type1' });
      expect(builder2).toBeCalledWith({ id: 'id2', type: 'type2' });
    });

    it('should return model as-is on unknown component type', () => {
      const factory = new ContentFactory();

      expect(factory.create({ id: 'id1', type: 'type' } as ContentModel)).toEqual({ id: 'id1', type: 'type' });
    });
  });
});
