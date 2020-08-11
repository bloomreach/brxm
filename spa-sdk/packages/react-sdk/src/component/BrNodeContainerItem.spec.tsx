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

import React from 'react';
import { mount, shallow } from 'enzyme';
import { ContainerItem, Page } from '@bloomreach/spa-sdk';
import { BrContainerItemUndefined } from '../cms';
import { BrNodeContainerItem } from './BrNodeContainerItem';

describe('BrNodeContainerItem', () => {
  const props = {
    component: {
      getType: jest.fn(),
      on: jest.fn(),
      off: jest.fn(),
    } as unknown as jest.Mocked<ContainerItem>,
    page: { sync: jest.fn() } as unknown as jest.Mocked<Page>,
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('componentDidMount', () => {
    it('should subscribe for update event on mount', () => {
      mount(<BrNodeContainerItem {...props} />);

      expect(props.component.on).toBeCalledWith('update', expect.any(Function));
    });
  });

  describe('componentDidUpdate', () => {
    it('should resubscribe on component update', () => {
      const wrapper = shallow(<BrNodeContainerItem {...props} />);

      jest.clearAllMocks();
      wrapper.setProps({ component: { ...props.component } });

      expect(props.component.off).toBeCalledWith('update', expect.any(Function));
      expect(props.component.on).toBeCalledWith('update', expect.any(Function));
    });
  });

  describe('componentWillUnmount', () => {
    it('should unsubscribe from update event on unmount', () => {
      const wrapper = mount(<BrNodeContainerItem {...props} />);
      wrapper.unmount();

      expect(props.component.off).toBeCalledWith('update', props.component.on.mock.calls[0][1]);
    });
  });

  describe('getMapping', () => {
    it('should use container item type for mapping', () => {
      shallow(<BrNodeContainerItem {...props} />);

      expect(props.component.getType).toBeCalled();
    });
  });

  describe('fallback', () => {
    it('should render undefined container item', () => {
      const wrapper = shallow(<BrNodeContainerItem {...props}><a/></BrNodeContainerItem>);

      expect(wrapper.equals(<BrContainerItemUndefined {...props}><a/></BrContainerItemUndefined>)).toBe(true);
    });
  });

  describe('onUpdate', () => {
    it('should trigger sync on update event', () => {
      mount(<BrNodeContainerItem {...props} />);
      props.component.on.mock.calls[0][1]({});

      expect(props.page.sync).toBeCalled();
    });
  });
});
