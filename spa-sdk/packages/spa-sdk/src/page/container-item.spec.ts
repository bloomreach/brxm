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

import { ContainerItem, TYPE_COMPONENT_CONTAINER_ITEM } from './container-item';

describe('ContainerItem', () => {
  describe('getType', () => {
    it('should return a type', () => {
      const containerItem = new ContainerItem({ type: TYPE_COMPONENT_CONTAINER_ITEM, label: 'Banner' });

      expect(containerItem.getType()).toBe('Banner');
    });
  });

  describe('isHidden', () => {
    it('should be hidden', () => {
      const containerItem = new ContainerItem({
        type: TYPE_COMPONENT_CONTAINER_ITEM,
        _meta: {
          params: { 'com.onehippo.cms7.targeting.TargetingParameterUtil.hide': 'on' },
        },
      });

      expect(containerItem.isHidden()).toBe(true);
    });

    it('should not be hidden', () => {
      const containerItem1 = new ContainerItem({
        type: TYPE_COMPONENT_CONTAINER_ITEM,
        _meta: {
          params: { 'com.onehippo.cms7.targeting.TargetingParameterUtil.hide': 'off' },
        },
      });

      const containerItem2 = new ContainerItem({ type: TYPE_COMPONENT_CONTAINER_ITEM, _meta: { params: {} } });
      const containerItem3 = new ContainerItem({ type: TYPE_COMPONENT_CONTAINER_ITEM, _meta: { } });
      const containerItem4 = new ContainerItem({ type: TYPE_COMPONENT_CONTAINER_ITEM });

      expect(containerItem1.isHidden()).toBe(false);
      expect(containerItem2.isHidden()).toBe(false);
      expect(containerItem3.isHidden()).toBe(false);
      expect(containerItem4.isHidden()).toBe(false);
    });
  });

  describe('getParameters', () => {
    it('should return parameters', () => {
      const containerItem = new ContainerItem({
        type: TYPE_COMPONENT_CONTAINER_ITEM,
        _meta: {
          paramsInfo: { a: '1', b: '2' },
        },
      });

      expect(containerItem.getParameters()).toEqual({ a: '1', b: '2' });
    });

    it('should return an empty object', () => {
      const containerItem1 = new ContainerItem({ type: TYPE_COMPONENT_CONTAINER_ITEM, _meta: {} });
      const containerItem2 = new ContainerItem({ type: TYPE_COMPONENT_CONTAINER_ITEM });

      expect(containerItem1.getParameters()).toEqual({});
      expect(containerItem2.getParameters()).toEqual({});
    });
  });
});
