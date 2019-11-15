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
import { ComponentImpl, TYPE_COMPONENT_CONTAINER_ITEM } from './component';
import { ContainerItemImpl, ContainerItemModel, ContainerItem, isContainerItem } from './container-item';
import { Events } from '../events';
import { Factory } from './factory';
import { Link } from './link';
import { MetaCollectionModel, Meta } from './meta';

let eventBus: Typed<Events>;
let linkFactory: jest.Mocked<Factory<[Link], string>>;
let metaFactory: jest.Mocked<Factory<[MetaCollectionModel], Meta[]>>;

function createContainerItem(model: ContainerItemModel) {
  return new ContainerItemImpl(model, eventBus, linkFactory, metaFactory);
}

beforeEach(() => {
  eventBus = new Typed<Events>();
  linkFactory = { create: jest.fn() };
  metaFactory = { create: jest.fn() };
});

describe('ContainerItemImpl', () => {
  describe('getType', () => {
    it('should return a type', () => {
      const containerItem = createContainerItem({ id: 'id', type: TYPE_COMPONENT_CONTAINER_ITEM, label: 'Banner' });

      expect(containerItem.getType()).toBe('Banner');
    });
  });

  describe('isHidden', () => {
    it('should be hidden', () => {
      const containerItem = createContainerItem({
        id: 'id',
        type: TYPE_COMPONENT_CONTAINER_ITEM,
        _meta: {
          params: { 'com.onehippo.cms7.targeting.TargetingParameterUtil.hide': 'on' },
        },
      });

      expect(containerItem.isHidden()).toBe(true);
    });

    it('should not be hidden', () => {
      const containerItem1 = createContainerItem({
        id: 'id',
        type: TYPE_COMPONENT_CONTAINER_ITEM,
        _meta: {
          params: { 'com.onehippo.cms7.targeting.TargetingParameterUtil.hide': 'off' },
        },
      });

      const containerItem2 = createContainerItem({
        id: 'id',
        type: TYPE_COMPONENT_CONTAINER_ITEM,
        _meta: { params: {} },
      });
      const containerItem3 = createContainerItem({ id: 'id', type: TYPE_COMPONENT_CONTAINER_ITEM, _meta: { } });
      const containerItem4 = createContainerItem({ id: 'id', type: TYPE_COMPONENT_CONTAINER_ITEM });

      expect(containerItem1.isHidden()).toBe(false);
      expect(containerItem2.isHidden()).toBe(false);
      expect(containerItem3.isHidden()).toBe(false);
      expect(containerItem4.isHidden()).toBe(false);
    });
  });

  describe('getParameters', () => {
    it('should return parameters', () => {
      const containerItem = createContainerItem({
        id: 'id',
        type: TYPE_COMPONENT_CONTAINER_ITEM,
        _meta: {
          paramsInfo: { a: '1', b: '2' },
        },
      });

      expect(containerItem.getParameters()).toEqual({ a: '1', b: '2' });
    });

    it('should return an empty object', () => {
      const containerItem1 = createContainerItem({ id: 'id', type: TYPE_COMPONENT_CONTAINER_ITEM, _meta: {} });
      const containerItem2 = createContainerItem({ id: 'id', type: TYPE_COMPONENT_CONTAINER_ITEM });

      expect(containerItem1.getParameters()).toEqual({});
      expect(containerItem2.getParameters()).toEqual({});
    });
  });

  describe('onPageUpdate', () => {
    let containerItem: ContainerItem;

    beforeEach(() => {
      containerItem = createContainerItem({ id: 'id1', type: TYPE_COMPONENT_CONTAINER_ITEM, label: 'a' });
      metaFactory.create.mockClear();
    });

    it('should not update a container item if it is not the same container item', async () => {
      await eventBus.emitSerial(
        'page.update',
        { page: {
          page: { id: 'id2', type: TYPE_COMPONENT_CONTAINER_ITEM, label: 'b' },
        } },
      );

      expect(metaFactory.create).not.toBeCalled();
      expect(containerItem.getType()).toBe('a');
    });

    it('should update a meta-data on page.update event', async () => {
      const metaModel = {};
      const meta = [] as Meta[];
      metaFactory.create.mockReturnValueOnce(meta);
      await eventBus.emitSerial(
        'page.update',
        { page: {
          page: { id: 'id1', type: TYPE_COMPONENT_CONTAINER_ITEM, _meta: metaModel },
        } },
      );

      expect(metaFactory.create).toBeCalledWith(metaModel);
      expect(containerItem.getMeta()).toBe(meta);
    });

    it('should update a model on page.update event', async () => {
      await eventBus.emitSerial(
        'page.update',
        { page: {
          page: { id: 'id1', type: TYPE_COMPONENT_CONTAINER_ITEM, label: 'b' },
        } },
      );

      expect(containerItem.getType()).toBe('b');
    });

    it('should emit an update event', async () => {
      const listener = jest.fn();
      containerItem.on('update', listener);

      await eventBus.emitSerial(
        'page.update',
        { page: {
          page: { id: 'id1', type: TYPE_COMPONENT_CONTAINER_ITEM },
        } },
      );
      await new Promise(process.nextTick);

      expect(listener).toBeCalledWith({});
    });
  });
});

describe('isContainerItem', () => {
  it('should return true', () => {
    const containerItem = createContainerItem({ id: 'id', type: TYPE_COMPONENT_CONTAINER_ITEM });

    expect(isContainerItem(containerItem)).toBe(true);
  });

  it('should return false', () => {
    const component = new ComponentImpl(
      { id: 'id', type: TYPE_COMPONENT_CONTAINER_ITEM },
      [],
      linkFactory,
      metaFactory,
    );

    expect(isContainerItem(undefined)).toBe(false);
    expect(isContainerItem(component)).toBe(false);
  });
});
