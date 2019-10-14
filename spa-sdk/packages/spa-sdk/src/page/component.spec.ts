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

import { ComponentImpl, ComponentModel, Component, TYPE_COMPONENT, isComponent } from './component';
import { Factory } from './factory';
import { Link } from './link';
import { MetaCollectionModel, MetaImpl, Meta, META_POSITION_BEGIN } from './meta';

let linkFactory: jest.Mocked<Factory<[Link], string>>;
let metaFactory: jest.Mocked<Factory<[MetaCollectionModel], Meta[]>>;

function createComponent(model: ComponentModel, children: Component[] = []) {
  return new ComponentImpl(model, children, linkFactory, metaFactory);
}

beforeEach(() => {
  linkFactory = { create: jest.fn() };
  metaFactory = { create: jest.fn() };
});

describe('ComponentImpl', () => {
  describe('getId', () => {
    it('should return a component id', () => {
      const component = createComponent({ id: 'id', type: TYPE_COMPONENT });

      expect(component.getId()).toBe('id');
    });
  });

  describe('getMeta', () => {
    it('should return a meta-data array', () => {
      const metaModel = {};
      const meta = new MetaImpl({ data: '', type: 'comment' }, META_POSITION_BEGIN);

      metaFactory.create.mockReturnValueOnce([meta]);
      const component = createComponent({ id: 'id', type: TYPE_COMPONENT, _meta: metaModel });

      expect(metaFactory.create).toBeCalledWith(metaModel);
      expect(component.getMeta()).toEqual([meta]);
    });

    it('should pass an empty object if there is no meta-data', () => {
      createComponent({ id: 'id', type: TYPE_COMPONENT });

      expect(metaFactory.create).toBeCalledWith({});
    });
  });

  describe('getModels', () => {
    it('should return models object', () => {
      const component = createComponent({ id: 'id', type: TYPE_COMPONENT, models: { a: 1, b: 2 } });

      expect(component.getModels()).toEqual({ a: 1, b: 2 });
    });

    it('should return empty object', () => {
      const component = createComponent({ id: 'id', type: TYPE_COMPONENT });

      expect(component.getModels()).toEqual({});
    });
  });

  describe('getUrl', () => {
    it('should return a model url', () => {
      const component = createComponent(
        { id: 'id', type: TYPE_COMPONENT, _links: { componentRendering: { href: 'url' } } },
      );

      linkFactory.create.mockReturnValueOnce('url');

      expect(component.getUrl()).toBe('url');
      expect(linkFactory.create).toBeCalledWith({ href: 'url' });
    });

    it('should return undefined when component links are missing', () => {
      const component = createComponent({ id: 'id', type: TYPE_COMPONENT });

      expect(component.getUrl()).toBeUndefined();
    });
  });

  describe('getName', () => {
    it('should return a name', () => {
      const component = createComponent({ id: 'id', type: TYPE_COMPONENT, name: 'something' });

      expect(component.getName()).toBe('something');
    });

    it('should return an empty string', () => {
      const component = createComponent({ id: 'id', type: TYPE_COMPONENT });

      expect(component.getName()).toBe('');
    });
  });

  describe('getParameters', () => {
    it('should return parameters', () => {
      const component = createComponent({ id: 'id', type: TYPE_COMPONENT, _meta: { params: { a: '1', b: '2' } } });

      expect(component.getParameters()).toEqual({ a: '1', b: '2' });
    });

    it('should return an empty object', () => {
      const component1 = createComponent({ id: 'id', type: TYPE_COMPONENT });
      const component2 = createComponent({ id: 'id', type: TYPE_COMPONENT, _meta: {} });

      expect(component1.getParameters()).toEqual({});
      expect(component2.getParameters()).toEqual({});
    });
  });

  describe('getComponent', () => {
    it('should return a reference to itself', () => {
      const component = createComponent({ id: 'id', type: TYPE_COMPONENT });

      expect(component.getComponent()).toBe(component);
    });

    it('should find a child component', () => {
      const root = createComponent({ id: 'root-component', type: TYPE_COMPONENT }, [
        createComponent({ id: 'a-component', type: TYPE_COMPONENT, name: 'a' }),
        createComponent({ id: 'b-component', type: TYPE_COMPONENT, name: 'b' }, [
          createComponent({ id: 'c-component', type: TYPE_COMPONENT, name: 'c' }),
        ]),
      ]);

      expect(root.getComponent('a')).toBeDefined();
      expect(root.getComponent('a')!.getName()).toBe('a');

      expect(root.getComponent('b', 'c')).toBeDefined();
      expect(root.getComponent('b', 'c')!.getName()).toBe('c');
    });

    it('should not find a child component', () => {
      const component = createComponent({ id: 'id', type: TYPE_COMPONENT });

      expect(component.getComponent('a', 'b')).toBeUndefined();
    });
  });

  describe('getComponentById', () => {
    it('should find a component by id', () => {
      const root = createComponent({ id: 'root-component', type: TYPE_COMPONENT }, [
        createComponent({ id: 'a-component', type: TYPE_COMPONENT, name: 'a' }),
        createComponent({ id: 'b-component', type: TYPE_COMPONENT, name: 'b' }, [
          createComponent({ id: 'c-component', type: TYPE_COMPONENT, name: 'c' }),
        ]),
      ]);

      expect(root.getComponentById('a-component')).toBeDefined();
      expect(root.getComponentById('a-component')!.getName()).toBe('a');

      expect(root.getComponentById('c-component')).toBeDefined();
      expect(root.getComponentById('c-component')!.getName()).toBe('c');
    });

    it('should return undefined when a component does not exist', () => {
      const component = createComponent({ id: 'id', type: TYPE_COMPONENT });

      expect(component.getComponentById('a')).toBeUndefined();
    });
  });
});

describe('isComponent', () => {
  it('should return true', () => {
    const containerItem = createComponent({ id: 'id', type: TYPE_COMPONENT });

    expect(isComponent(containerItem)).toBe(true);
  });

  it('should return false', () => {
    expect(isComponent(undefined)).toBe(false);
    expect(isComponent({})).toBe(false);
  });
});
