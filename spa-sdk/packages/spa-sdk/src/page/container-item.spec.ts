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

import { Typed } from 'emittery';
import { ContainerItem, TYPE_COMPONENT_CONTAINER_ITEM } from './container-item';
import { Events } from '../events';
import { Meta } from './meta';
import { Page } from './page';

describe('ContainerItem', () => {
  let eventBus: Typed<Events>;

  beforeEach(() => {
    eventBus = new Typed<Events>();
  });

  describe('getType', () => {
    it('should return a type', () => {
      const containerItem = new ContainerItem(
        { id: 'id', type: TYPE_COMPONENT_CONTAINER_ITEM, label: 'Banner' },
        eventBus,
      );

      expect(containerItem.getType()).toBe('Banner');
    });
  });

  describe('isHidden', () => {
    it('should be hidden', () => {
      const containerItem = new ContainerItem(
        {
          id: 'id',
          type: TYPE_COMPONENT_CONTAINER_ITEM,
          _meta: {
            params: { 'com.onehippo.cms7.targeting.TargetingParameterUtil.hide': 'on' },
          },
        },
        eventBus,
      );

      expect(containerItem.isHidden()).toBe(true);
    });

    it('should not be hidden', () => {
      const containerItem1 = new ContainerItem(
        {
          id: 'id',
          type: TYPE_COMPONENT_CONTAINER_ITEM,
          _meta: {
            params: { 'com.onehippo.cms7.targeting.TargetingParameterUtil.hide': 'off' },
          },
        },
        eventBus,
      );

      const containerItem2 = new ContainerItem(
        {
          id: 'id',
          type: TYPE_COMPONENT_CONTAINER_ITEM,
          _meta: { params: {} },
        },
        eventBus,
      );
      const containerItem3 = new ContainerItem({ id: 'id', type: TYPE_COMPONENT_CONTAINER_ITEM, _meta: { } }, eventBus);
      const containerItem4 = new ContainerItem({ id: 'id', type: TYPE_COMPONENT_CONTAINER_ITEM }, eventBus);

      expect(containerItem1.isHidden()).toBe(false);
      expect(containerItem2.isHidden()).toBe(false);
      expect(containerItem3.isHidden()).toBe(false);
      expect(containerItem4.isHidden()).toBe(false);
    });
  });

  describe('getParameters', () => {
    it('should return parameters', () => {
      const containerItem = new ContainerItem(
        {
          id: 'id',
          type: TYPE_COMPONENT_CONTAINER_ITEM,
          _meta: {
            paramsInfo: { a: '1', b: '2' },
          },
        },
        eventBus,
      );

      expect(containerItem.getParameters()).toEqual({ a: '1', b: '2' });
    });

    it('should return an empty object', () => {
      const containerItem1 = new ContainerItem({ id: 'id', type: TYPE_COMPONENT_CONTAINER_ITEM, _meta: {} }, eventBus);
      const containerItem2 = new ContainerItem({ id: 'id', type: TYPE_COMPONENT_CONTAINER_ITEM }, eventBus);

      expect(containerItem1.getParameters()).toEqual({});
      expect(containerItem2.getParameters()).toEqual({});
    });
  });

  describe('onPageUpdate', () => {
    const meta1 = [] as Meta[];
    const meta2 = [] as Meta[];
    let containerItem1: ContainerItem;
    let containerItem2: ContainerItem;

    beforeEach(() => {
      containerItem1 = new ContainerItem({ id: 'id1', type: TYPE_COMPONENT_CONTAINER_ITEM }, eventBus, meta1);
      containerItem2 = new ContainerItem({ id: 'id2', type: TYPE_COMPONENT_CONTAINER_ITEM }, eventBus, meta2);
    });

    it('should not update a meta-data if it is not the same container item', async () => {
      await eventBus.emitSerial('page.update', {
        page: new Page(
          { page: { id: 'id2', type: TYPE_COMPONENT_CONTAINER_ITEM } },
          containerItem2,
          new Map(),
          eventBus,
        ),
      });

      expect(containerItem1.getMeta()).toBe(meta1);
    });

    it('should update a meta-data on page.update event', async () => {
      await eventBus.emitSerial('page.update', {
        page: new Page(
          { page: { id: 'id1', type: TYPE_COMPONENT_CONTAINER_ITEM } },
          new ContainerItem({ id: 'id1', type: TYPE_COMPONENT_CONTAINER_ITEM }, eventBus, meta2),
          new Map(),
          eventBus,
        ),
      });

      expect(containerItem1.getMeta()).toBe(meta2);
    });

    it('should emit an update event', async () => {
      const listener = jest.fn();
      containerItem1.on('update', listener);

      await eventBus.emitSerial('page.update', {
        page: new Page(
          { page: { id: 'id1', type: TYPE_COMPONENT_CONTAINER_ITEM } },
          new ContainerItem({ id: 'id1', type: TYPE_COMPONENT_CONTAINER_ITEM }, eventBus, meta2),
          new Map(),
          eventBus,
        ),
      });

      expect(listener).toBeCalledWith({});
    });
  });
});
