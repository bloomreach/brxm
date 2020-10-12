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

import { Typed } from 'emittery';
import { ComponentImpl, TYPE_COMPONENT_CONTAINER_ITEM } from './component09';
import { ContainerItemImpl, ContainerItemModel, isContainerItem } from './container-item09';
import { ContainerItem } from './container-item';
import { EventBus, Events } from './events09';
import { MetaCollectionFactory } from './meta-collection-factory';
import { MetaCollection } from './meta-collection';
import { PageModel } from './page09';
import { UrlBuilder } from '../url';

let eventBus: EventBus;
let metaFactory: jest.MockedFunction<MetaCollectionFactory>;
let urlBuilder: jest.Mocked<UrlBuilder>;

const model = {
  _meta: {},
  id: 'id',
  type: TYPE_COMPONENT_CONTAINER_ITEM,
} as ContainerItemModel;

function createContainerItem(containerItemModel = model) {
  return new ContainerItemImpl(containerItemModel, metaFactory, urlBuilder, eventBus);
}

beforeEach(() => {
  eventBus = new Typed<Events>();
  metaFactory = jest.fn();
  urlBuilder = {} as unknown as typeof urlBuilder;
});

describe('ContainerItemImpl', () => {
  describe('getLabel', () => {
    it('should return a label', () => {
      const containerItem = createContainerItem({ ...model, ctype: 'NewsList', label: 'News List' });

      expect(containerItem.getLabel()).toBe('News List');
    });
  });

  describe('getType', () => {
    it('should return a component type', () => {
      const containerItem = createContainerItem({ ...model, ctype: 'NewsList', label: 'News List' });

      expect(containerItem.getType()).toBe('NewsList');
    });

    it('should fall back to the label', () => {
      const containerItem = createContainerItem({ ...model, label: 'Banner' });

      expect(containerItem.getType()).toBe('Banner');
    });
  });

  describe('isHidden', () => {
    it('should be hidden', () => {
      const containerItem = createContainerItem({
        ...model,
        _meta: {
          params: { 'com.onehippo.cms7.targeting.TargetingParameterUtil.hide': 'on' },
        },
      });

      expect(containerItem.isHidden()).toBe(true);
    });

    it('should not be hidden', () => {
      const containerItem1 = createContainerItem({
        ...model,
        _meta: {
          params: { 'com.onehippo.cms7.targeting.TargetingParameterUtil.hide': 'off' },
        },
      });

      const containerItem2 = createContainerItem({
        ...model,
        _meta: { params: {} },
      });
      const containerItem3 = createContainerItem();

      expect(containerItem1.isHidden()).toBe(false);
      expect(containerItem2.isHidden()).toBe(false);
      expect(containerItem3.isHidden()).toBe(false);
    });
  });

  describe('getParameters', () => {
    it('should return parameters', () => {
      const containerItem = createContainerItem({
        ...model,
        _meta: {
          paramsInfo: { a: '1', b: '2' },
        },
      });

      expect(containerItem.getParameters()).toEqual({ a: '1', b: '2' });
    });

    it('should return an empty object', () => {
      const containerItem = createContainerItem();

      expect(containerItem.getParameters()).toEqual({});
    });
  });

  describe('onPageUpdate', () => {
    let containerItem: ContainerItem;

    beforeEach(() => {
      containerItem = createContainerItem({ ...model, id: 'id1', label: 'a' });
      metaFactory.mockClear();
    });

    it('should not update a container item if it is not the same container item', async () => {
      await eventBus.emitSerial(
        'page.update',
        { page: {
          page: { ...model, id: 'id2', label: 'b' },
        } as PageModel },
      );

      expect(metaFactory).not.toBeCalled();
      expect(containerItem.getType()).toBe('a');
    });

    it('should update a meta-data on page.update event', async () => {
      const metaModel = {};
      const meta = {} as MetaCollection;
      metaFactory.mockReturnValueOnce(meta);
      await eventBus.emitSerial(
        'page.update',
        { page: {
          page: { ...model, id: 'id1', _meta: metaModel },
        } as PageModel },
      );

      expect(metaFactory).toBeCalledWith(metaModel);
      expect(containerItem.getMeta()).toBe(meta);
    });

    it('should update a model on page.update event', async () => {
      await eventBus.emitSerial(
        'page.update',
        { page: {
          page: { ...model, id: 'id1', label: 'b' },
        } as PageModel },
      );

      expect(containerItem.getType()).toBe('b');
    });

    it('should emit an update event', async () => {
      const listener = jest.fn();
      containerItem.on('update', listener);

      await eventBus.emitSerial(
        'page.update',
        { page: {
          page: { ...model, id: 'id1' },
        } as PageModel },
      );
      await new Promise(process.nextTick);

      expect(listener).toBeCalledWith({});
    });
  });
});

describe('isContainerItem', () => {
  it('should return true', () => {
    const containerItem = createContainerItem();

    expect(isContainerItem(containerItem)).toBe(true);
  });

  it('should return false', () => {
    const component = new ComponentImpl(model, [], metaFactory, urlBuilder);

    expect(isContainerItem(undefined)).toBe(false);
    expect(isContainerItem(component)).toBe(false);
  });
});
