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
import { Meta, META_POSITION_BEGIN } from './meta';

describe('Component', () => {
  describe('getMeta', () => {
    it('should return a meta-data array', () => {
      const meta = new Meta({ data: '', type: 'comment' }, META_POSITION_BEGIN);
      const component = new Component({ id: 'id', type: TYPE_COMPONENT }, [], [meta]);

      expect(component.getMeta()).toEqual([meta]);
    });

    it('should return an empty array', () => {
      const component = new Component({ id: 'id', type: TYPE_COMPONENT });

      expect(component.getMeta()).toEqual([]);
    });
  });

  describe('getModels', () => {
    it('should return models object', () => {
      const component = new Component({ id: 'id', type: TYPE_COMPONENT, models: { a: 1, b: 2 } });

      expect(component.getModels()).toEqual({ a: 1, b: 2 });
    });

    it('should return empty object', () => {
      const component = new Component({ id: 'id', type: TYPE_COMPONENT });

      expect(component.getModels()).toEqual({});
    });
  });

  describe('getModelUrl', () => {
    it('should return a model url', () => {
      const component = new Component({
        id: 'id',
        type: TYPE_COMPONENT,
        _links: { componentRendering: { href: 'url' } },
      });

      expect(component.getModelUrl()).toBe('url');
    });

    it('should return undefined when a model url is missing', () => {
      const component1 = new Component({ id: 'id', type: TYPE_COMPONENT });
      const component2 = new Component({ id: 'id', type: TYPE_COMPONENT, _links: {} });

      expect(component1.getModelUrl()).toBeUndefined();
      expect(component2.getModelUrl()).toBeUndefined();
    });
  });

  describe('getName', () => {
    it('should return a name', () => {
      const component = new Component({ id: 'id', type: TYPE_COMPONENT, name: 'something' });

      expect(component.getName()).toBe('something');
    });

    it('should return an empty string', () => {
      const component = new Component({ id: 'id', type: TYPE_COMPONENT });

      expect(component.getName()).toBe('');
    });
  });

  describe('getParameters', () => {
    it('should return parameters', () => {
      const component = new Component({
        id: 'id',
        type: TYPE_COMPONENT,
        _meta: {
          params: { a: '1', b: '2' },
        },
      });

      expect(component.getParameters()).toEqual({ a: '1', b: '2' });
    });

    it('should return an empty object', () => {
      const component1 = new Component({ id: 'id', type: TYPE_COMPONENT });
      const component2 = new Component({ id: 'id', type: TYPE_COMPONENT, _meta: {} });

      expect(component1.getParameters()).toEqual({});
      expect(component2.getParameters()).toEqual({});
    });
  });

  describe('getComponent', () => {
    it('should return a reference to itself', () => {
      const component = new Component({ id: 'id', type: TYPE_COMPONENT });

      expect(component.getComponent()).toBe(component);
    });

    it('should find a child component', () => {
      const root = new Component({ id: 'root-component', type: TYPE_COMPONENT }, [
        new Component({ id: 'a-component', type: TYPE_COMPONENT, name: 'a' }),
        new Component({ id: 'b-component', type: TYPE_COMPONENT, name: 'b' }, [
          new Component({ id: 'c-component', type: TYPE_COMPONENT, name: 'c' }),
        ]),
      ]);

      expect(root.getComponent('a')).toBeDefined();
      expect(root.getComponent('a')!.getName()).toBe('a');

      expect(root.getComponent('b', 'c')).toBeDefined();
      expect(root.getComponent('b', 'c')!.getName()).toBe('c');
    });

    it('should not find a child component', () => {
      const component = new Component({ id: 'id', type: TYPE_COMPONENT });

      expect(component.getComponent('a', 'b')).toBeUndefined();
    });
  });

  describe('getComponentById', () => {
    it('should find a component by id', () => {
      const root = new Component({ id: 'root-component', type: TYPE_COMPONENT }, [
        new Component({ id: 'a-component', type: TYPE_COMPONENT, name: 'a' }),
        new Component({ id: 'b-component', type: TYPE_COMPONENT, name: 'b' }, [
          new Component({ id: 'c-component', type: TYPE_COMPONENT, name: 'c' }),
        ]),
      ]);

      expect(root.getComponentById('a-component')).toBeDefined();
      expect(root.getComponentById('a-component')!.getName()).toBe('a');

      expect(root.getComponentById('c-component')).toBeDefined();
      expect(root.getComponentById('c-component')!.getName()).toBe('c');
    });

    it('should return undefined when a component does not exist', () => {
      const component = new Component({ id: 'id', type: TYPE_COMPONENT });

      expect(component.getComponentById('a')).toBeUndefined();
    });
  });
});
