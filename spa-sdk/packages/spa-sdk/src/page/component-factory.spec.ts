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

import { ComponentFactory } from './component-factory';
import { ComponentModel, Component } from './component';

describe('ComponentFactory', () => {
  describe('register', () => {
    it('should provide a fluent interface', () => {
      const factory = new ComponentFactory();

      expect(factory.register('something', () => new Component({ type: 'something' }))).toBe(factory);
    });
  });

  describe('create', () => {
    it('should call a registered builder', () => {
      const builder1 = jest.fn(() => new Component({ type: '1' }));
      const builder2 = jest.fn(() => new Component({ type: '2' }));
      const factory = new ComponentFactory()
        .register('type1', builder1)
        .register('type2', builder2);

      factory.create({ type: 'type1', name: 'Component 1' });
      factory.create({ type: 'type2', name: 'Component 2' });

      expect(builder1).toBeCalledWith({ type: 'type1', name: 'Component 1' }, []);
      expect(builder2).toBeCalledWith({ type: 'type2', name: 'Component 2' }, []);
    });

    it('should throw an exception on unknown component type', () => {
      const factory = new ComponentFactory()
        .register('type0', model => new Component(model));

      expect(() => factory.create({ type: 'type1', name: 'Component 1' })).toThrowError();
    });

    it('should produce a tree structure', () => {
      const builder = jest.fn((model: ComponentModel, children: Component[]) => new Component(model, children));
      const factory = new ComponentFactory()
        .register('type', builder);

      const root = factory.create({
        type: 'type',
        components: [
          { type: 'type', name: 'a' },
          { type: 'type',
            name: 'b',
            components: [
              { type: 'type', name: 'c' },
              { type: 'type', name: 'd' },
            ] },
        ],
      });

      expect(root.getComponent('a', 'b')).toBeUndefined();

      const c = root.getComponent('b', 'c');
      expect(c).not.toBeUndefined();
      expect(c!.getName()).toBe('c');
    });
  });
});
