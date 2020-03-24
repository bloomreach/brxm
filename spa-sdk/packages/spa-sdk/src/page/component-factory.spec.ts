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

import { ComponentFactory } from './component-factory';
import {
  ComponentModel,
  TYPE_COMPONENT,
  TYPE_COMPONENT_CONTAINER_ITEM,
  TYPE_COMPONENT_CONTAINER,
} from './component';

const model = {
  id: 'id',
  type: TYPE_COMPONENT,
} as ComponentModel;

describe('ComponentFactory', () => {
  describe('create', () => {
    it('should call a registered builder', () => {
      const builder1 = jest.fn();
      const builder2 = jest.fn();
      const factory = new ComponentFactory()
        .register(TYPE_COMPONENT, builder1)
        .register(TYPE_COMPONENT_CONTAINER, builder2);

      factory.create({ ...model, id: 'id1', name: 'Component 1' });
      factory.create({ ...model, id: 'id2', type: TYPE_COMPONENT_CONTAINER, name: 'Component 2' });

      expect(builder1).toBeCalledWith({ ...model, id: 'id1', name: 'Component 1' }, []);
      expect(builder2).toBeCalledWith(
        {
          ...model,
          id: 'id2',
          type: TYPE_COMPONENT_CONTAINER,
          name: 'Component 2',
        },
        [],
      );
    });

    it('should throw an exception on unknown component type', () => {
      const factory = new ComponentFactory()
        .register(TYPE_COMPONENT_CONTAINER_ITEM, jest.fn());

      expect(() => factory.create({
        ...model,
        id: 'id1',
        type: TYPE_COMPONENT_CONTAINER,
        name: 'Component 1',
      })).toThrowError();
    });

    it('should produce a tree structure', () => {
      const builder = jest.fn(model => model.id);
      const factory = new ComponentFactory()
        .register(TYPE_COMPONENT, builder);

      const root = factory.create({
        ...model,
        id: 'root',
        components: [
          { ...model, id: 'a' },
          { ...model, id: 'b',
            components: [
              { ...model, id: 'c' },
              { ...model, id: 'd' },
            ] },
        ],
      });

      expect(builder).toBeCalledTimes(5);
      expect(builder).nthCalledWith(1, expect.objectContaining({ id: 'a' }), []);
      expect(builder).nthCalledWith(2, expect.objectContaining({ id: 'c' }), []);
      expect(builder).nthCalledWith(3, expect.objectContaining({ id: 'd' }), []);
      expect(builder).nthCalledWith(4, expect.objectContaining({ id: 'b' }), ['c', 'd']);
      expect(builder).nthCalledWith(5, expect.objectContaining({ id: 'root' }), ['a', 'b']);
      expect(root).toBe('root');
    });
  });
});
