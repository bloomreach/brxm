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
import  { PageModel } from './page';

const model = (page: Record<string, unknown>) => ({
  page,
  root: { $ref: '/page/root' },
} as unknown as PageModel);

describe('ComponentFactory', () => {
  describe('create', () => {
    it('should call a registered builder', () => {
      const builder1 = jest.fn();
      const builder2 = jest.fn();
      const factory = new ComponentFactory()
        .register(TYPE_COMPONENT, builder1)
        .register(TYPE_COMPONENT_CONTAINER, builder2);

      factory.create(model({ root: { id: 'id1', name: 'Component 1', type: TYPE_COMPONENT } }));
      factory.create(model({ root: { id: 'id2', name: 'Component 2', type: TYPE_COMPONENT_CONTAINER } }));

      expect(builder1).toBeCalledWith(expect.objectContaining({ id: 'id1', name: 'Component 1' }), []);
      expect(builder2).toBeCalledWith(
        expect.objectContaining({
          id: 'id2',
          type: TYPE_COMPONENT_CONTAINER,
          name: 'Component 2',
        }),
        [],
      );
    });

    it('should throw an exception on unknown component type', () => {
      const factory = new ComponentFactory()
        .register(TYPE_COMPONENT_CONTAINER_ITEM, jest.fn());

      expect(() => factory.create(model({
        root: {
          id: 'id2',
          name: 'Component 2',
          type: TYPE_COMPONENT_CONTAINER,
        },
      }))).toThrowError();
    });

    it('should produce a tree structure', () => {
      const builder = jest.fn(model => model.id);
      const factory = new ComponentFactory()
        .register(TYPE_COMPONENT, builder);

      const root = factory.create(model({
        root: {
          id: 'root',
          children: [{ $ref: '/page/a' }, { $ref: '/page/b' }],
          type: TYPE_COMPONENT,
        },
        a: { id: 'a', type: TYPE_COMPONENT },
        b: {
          id: 'b',
          children: [{ $ref: '/page/c' }, { $ref: '/page/d' }],
          type: TYPE_COMPONENT,
        },
        c: { id: 'c', type: TYPE_COMPONENT },
        d: { id: 'd', type: TYPE_COMPONENT },
      }));

      expect(builder).toBeCalledTimes(5);
      expect(builder).nthCalledWith(1, expect.objectContaining({ id: 'd' }), []);
      expect(builder).nthCalledWith(2, expect.objectContaining({ id: 'c' }), []);
      expect(builder).nthCalledWith(3, expect.objectContaining({ id: 'b' }), ['c', 'd']);
      expect(builder).nthCalledWith(4, expect.objectContaining({ id: 'a' }), []);
      expect(builder).nthCalledWith(5, expect.objectContaining({ id: 'root' }), ['a', 'b']);
      expect(root).toBe('root');
    });
  });
});
