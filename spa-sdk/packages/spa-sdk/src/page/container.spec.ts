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

import { ComponentImpl } from './component';
import { ContainerImpl, isContainer, TYPE_COMPONENT_CONTAINER, TYPE_CONTAINER_BOX } from './container';

describe('ContainerImpl', () => {
  describe('getChildren', () => {
    it('should return children', () => {
      const container = new ContainerImpl({ id: 'id', type: TYPE_COMPONENT_CONTAINER, xtype: TYPE_CONTAINER_BOX }, []);

      expect(container.getChildren()).toEqual([]);
    });
  });

  describe('getType', () => {
    it('should return a type', () => {
      const container = new ContainerImpl({ id: 'id', type: TYPE_COMPONENT_CONTAINER, xtype: TYPE_CONTAINER_BOX });

      expect(container.getType()).toBe(TYPE_CONTAINER_BOX);
    });

    it('should return a type in lower case', () => {
      const container = new ContainerImpl({
        id: 'id',
        type: TYPE_COMPONENT_CONTAINER,
        xtype: TYPE_CONTAINER_BOX.toUpperCase() as typeof TYPE_CONTAINER_BOX,
      });

      expect(container.getType()).toBe(TYPE_CONTAINER_BOX);
    });

    it('should return undefined where there is no xtype specified', () => {
      const container = new ContainerImpl({ id: 'id', type: TYPE_COMPONENT_CONTAINER });

      expect(container.getType()).toBeUndefined();
    });
  });
});

describe('isContainer', () => {
  it('should return true', () => {
    const container = new ContainerImpl({ id: 'id', type: TYPE_COMPONENT_CONTAINER, xtype: TYPE_CONTAINER_BOX });

    expect(isContainer(container)).toBe(true);
  });

  it('should return false', () => {
    const component = new ComponentImpl({ id: 'id', type: TYPE_COMPONENT_CONTAINER });

    expect(isContainer(undefined)).toBe(false);
    expect(isContainer(component)).toBe(false);
  });
});
