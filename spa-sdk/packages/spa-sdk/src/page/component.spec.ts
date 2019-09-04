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

import { Component, TYPE_COMPONENT } from './component';

describe('Component', () => {
  describe('getName', () => {
    it('should return a name', () => {
      const component = new Component({ type: TYPE_COMPONENT, name: 'something' });

      expect(component.getName()).toBe('something');
    });

    it('should return an empty string', () => {
      const component = new Component({ type: TYPE_COMPONENT });

      expect(component.getName()).toBe('');
    });
  });

  describe('getParameters', () => {
    it('should return parameters', () => {
      const component = new Component({
        type: TYPE_COMPONENT,
        _meta: {
          params: { a: '1', b: '2' },
        },
      });

      expect(component.getParameters()).toEqual({ a: '1', b: '2' });
    });

    it('should return an empty object', () => {
      const component1 = new Component({ type: TYPE_COMPONENT });
      const component2 = new Component({ type: TYPE_COMPONENT, _meta: {} });

      expect(component1.getParameters()).toEqual({});
      expect(component2.getParameters()).toEqual({});
    });
  });

  describe('getComponent', () => {
    it('should return a reference to itself', () => {
      const component = new Component({ type: TYPE_COMPONENT });

      expect(component.getComponent()).toBe(component);
    });

    it('should find a child component', () => {
      const root = new Component({ type: TYPE_COMPONENT }, [
        new Component({ type: TYPE_COMPONENT, name: 'a' }),
        new Component({ type: TYPE_COMPONENT, name: 'b' }, [
          new Component({ type: TYPE_COMPONENT, name: 'c' }),
        ]),
      ]);

      expect(root.getComponent('a')).not.toBeNull();
      expect(root.getComponent('a')!.getName()).toBe('a');

      expect(root.getComponent('b', 'c')).not.toBeNull();
      expect(root.getComponent('b', 'c')!.getName()).toBe('c');
    });
  });
});
