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

import { ComponentImpl, ComponentModel, Component, TYPE_COMPONENT, isComponent } from './component';
import { MetaCollectionFactory } from './meta-collection-factory';
import { MetaCollection } from './meta-collection';
import { UrlBuilder } from '../url';

let metaFactory: jest.MockedFunction<MetaCollectionFactory>;
let urlBuilder: jest.Mocked<UrlBuilder>;

const model = {
  _links: { componentRendering: { href: 'url' } },
  _meta: {},
  id: 'id',
  type: TYPE_COMPONENT,
} as ComponentModel;

function createComponent(componentModel = model, children: Component[] = []) {
  return new ComponentImpl(componentModel, children, metaFactory, urlBuilder);
}

beforeEach(() => {
  metaFactory = jest.fn();
  urlBuilder = { getApiUrl: jest.fn() } as unknown as typeof urlBuilder;
});

describe('ComponentImpl', () => {
  describe('getId', () => {
    it('should return a component id', () => {
      const component = createComponent();

      expect(component.getId()).toBe('id');
    });
  });

  describe('getMeta', () => {
    it('should return a meta-data collection', () => {
      const meta = {} as MetaCollection;

      metaFactory.mockReturnValueOnce(meta);
      const component = createComponent();

      expect(metaFactory).toBeCalledWith(model._meta);
      expect(component.getMeta()).toEqual(meta);
    });
  });

  describe('getModels', () => {
    it('should return models object', () => {
      const component = createComponent({ ...model, models: { a: 1, b: 2 } });

      expect(component.getModels()).toEqual({ a: 1, b: 2 });
    });

    it('should return empty object', () => {
      const component = createComponent();

      expect(component.getModels()).toEqual({});
    });
  });

  describe('getUrl', () => {
    it('should return a model url', () => {
      const component = createComponent();

      urlBuilder.getApiUrl.mockReturnValueOnce('url');

      expect(component.getUrl()).toBe('url');
      expect(urlBuilder.getApiUrl).toBeCalledWith('url');
    });

    it('should return undefined when component links are missing', () => {
      const component = createComponent();

      expect(component.getUrl()).toBeUndefined();
    });
  });

  describe('getName', () => {
    it('should return a name', () => {
      const component = createComponent({ ...model, name: 'something' });

      expect(component.getName()).toBe('something');
    });

    it('should return an empty string', () => {
      const component = createComponent();

      expect(component.getName()).toBe('');
    });
  });

  describe('getParameters', () => {
    it('should return parameters', () => {
      const component = createComponent({
        ...model,
        _meta: {
          params: { a: '1', b: '2' },
        },
      });

      expect(component.getParameters()).toEqual({ a: '1', b: '2' });
    });

    it('should return an empty object', () => {
      const component = createComponent();

      expect(component.getParameters()).toEqual({});
    });
  });

  describe('getComponent', () => {
    it('should return a reference to itself', () => {
      const component = createComponent();

      expect(component.getComponent()).toBe(component);
    });

    it('should find a child component', () => {
      const root = createComponent({ ...model, id: 'root-component' }, [
        createComponent({ ...model, id: 'a-component', name: 'a' }),
        createComponent({ ...model, id: 'b-component', name: 'b' }, [
          createComponent({ ...model, id: 'c-component', name: 'c' }),
        ]),
      ]);

      expect(root.getComponent('a')).toBeDefined();
      expect(root.getComponent('a')!.getName()).toBe('a');

      expect(root.getComponent('b', 'c')).toBeDefined();
      expect(root.getComponent('b', 'c')!.getName()).toBe('c');
    });

    it('should not find a child component', () => {
      const component = createComponent();

      expect(component.getComponent('a', 'b')).toBeUndefined();
    });
  });

  describe('getComponentById', () => {
    it('should find a component by id', () => {
      const root = createComponent({ ...model, id: 'root-component' }, [
        createComponent({ ...model, id: 'a-component', name: 'a' }),
        createComponent({ ...model, id: 'b-component', name: 'b' }, [
          createComponent({ ...model, id: 'c-component', name: 'c' }),
        ]),
      ]);

      expect(root.getComponentById('a-component')).toBeDefined();
      expect(root.getComponentById('a-component')!.getName()).toBe('a');

      expect(root.getComponentById('c-component')).toBeDefined();
      expect(root.getComponentById('c-component')!.getName()).toBe('c');
    });

    it('should return undefined when a component does not exist', () => {
      const component = createComponent();

      expect(component.getComponentById('a')).toBeUndefined();
    });
  });
});

describe('isComponent', () => {
  it('should return true', () => {
    const component = createComponent();

    expect(isComponent(component)).toBe(true);
  });

  it('should return false', () => {
    expect(isComponent(undefined)).toBe(false);
    expect(isComponent({})).toBe(false);
  });
});
