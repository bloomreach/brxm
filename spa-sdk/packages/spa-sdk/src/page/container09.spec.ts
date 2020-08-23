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

import { ComponentImpl, TYPE_COMPONENT_CONTAINER } from './component09';
import { ContainerImpl, ContainerModel, isContainer } from './container09';
import { ContainerItem } from './container-item';
import { MetaCollectionFactory } from './meta-collection-factory';
import { TYPE_CONTAINER_BOX } from './container';
import { UrlBuilder } from '../url';

let metaFactory: jest.MockedFunction<MetaCollectionFactory>;
let urlBuilder: jest.Mocked<UrlBuilder>;

const model = {
  _meta: {},
  id: 'id',
  type: TYPE_COMPONENT_CONTAINER,
} as ContainerModel;

function createContainer(containerModel = model, children: ContainerItem[] = []) {
  return new ContainerImpl(containerModel, children, metaFactory, urlBuilder);
}

beforeEach(() => {
  metaFactory = jest.fn();
  urlBuilder = {} as unknown as typeof urlBuilder;
});

describe('ContainerImpl', () => {
  describe('getChildren', () => {
    it('should return children', () => {
      const container = createContainer();

      expect(container.getChildren()).toEqual([]);
    });
  });

  describe('getType', () => {
    it('should return a type', () => {
      const container = createContainer({ ...model, xtype: TYPE_CONTAINER_BOX });

      expect(container.getType()).toBe(TYPE_CONTAINER_BOX);
    });

    it('should return a type in lower case', () => {
      const container = createContainer({
        ...model,
        xtype: TYPE_CONTAINER_BOX.toUpperCase() as typeof TYPE_CONTAINER_BOX,
      });

      expect(container.getType()).toBe(TYPE_CONTAINER_BOX);
    });

    it('should return undefined where there is no xtype specified', () => {
      const container = createContainer();

      expect(container.getType()).toBeUndefined();
    });
  });
});

describe('isContainer', () => {
  it('should return true', () => {
    const container = createContainer();

    expect(isContainer(container)).toBe(true);
  });

  it('should return false', () => {
    const component = new ComponentImpl(model, [], metaFactory, urlBuilder);

    expect(isContainer(undefined)).toBe(false);
    expect(isContainer(component)).toBe(false);
  });
});
