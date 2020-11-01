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

import React from 'react';
import { mocked } from 'ts-jest/utils';
import { shallow } from 'enzyme';
import { isContainer, isContainerItem, Component, MetaCollection, Page } from '@bloomreach/spa-sdk';
import { BrMeta } from '../meta';
import { BrNode } from './BrNode';
import { BrNodeComponent } from './BrNodeComponent';
import { BrNodeContainer } from './BrNodeContainer';
import { BrNodeContainerItem } from './BrNodeContainerItem';

jest.mock('@bloomreach/spa-sdk');

describe('BrNode', () => {
  const context = { test: 'test' } as unknown as jest.Mocked<Page>;
  const props = {
    component: {
      getChildren: jest.fn(() => []),
      getMeta: jest.fn(() => []),
    } as unknown as jest.Mocked<Component>,
  };

  beforeEach(() => {
    jest.restoreAllMocks();

    // @see https://github.com/airbnb/enzyme/issues/1553
    /// @ts-ignore
    BrNode.contextTypes = { test: () => null };
    delete (BrNode as Partial<typeof BrNode>).contextType;
  });

  it('should set a component context', () => {
    const wrapper = shallow(<BrNode {...props} />);

    expect(wrapper.find('ContextProvider').first().prop('value')).toEqual(props.component);
  });

  it('should render a component meta-data', () => {
    const meta = {} as MetaCollection;
    props.component.getMeta.mockReturnValueOnce(meta);
    const wrapper = shallow(<BrNode {...props} />);

    expect(wrapper.find(BrMeta).first().prop('meta')).toBe(meta);
  });

  it('should render a component', () => {
    const wrapper = shallow(<BrNode {...props} />, { context });
    const component = wrapper.find(BrNodeComponent);

    expect(component.exists()).toBe(true);
    expect(component.prop('component')).toBe(props.component);
    expect(component.prop('page')).toEqual(context);
  });

  it('should render a container', () => {
    mocked(isContainer).mockReturnValueOnce(true);
    const wrapper = shallow(<BrNode {...props} />, { context });
    const container = wrapper.find(BrNodeContainer);

    expect(container.exists()).toBe(true);
    expect(container.prop('component')).toBe(props.component);
    expect(container.prop('page')).toEqual(context);
  });

  it('should render a container item', () => {
    mocked(isContainerItem).mockReturnValueOnce(true);
    const wrapper = shallow(<BrNode {...props} />, { context });
    const containerItem = wrapper.find(BrNodeContainerItem);

    expect(containerItem.exists()).toBe(true);
    expect(containerItem.prop('component')).toBe(props.component);
    expect(containerItem.prop('page')).toEqual(context);
  });

  it('should render children if present', () => {
    const wrapper = shallow(<BrNode {...props}><a/><b/></BrNode>);

    expect(wrapper.contains(<a/>)).toBe(true);
    expect(wrapper.contains(<b/>)).toBe(true);
  });

  it('should render model children if component children are not present', () => {
    const component1 = {} as Component;
    const component2 = {} as Component;
    props.component.getChildren.mockReturnValueOnce([component1, component2]);
    const wrapper = shallow(<BrNode {...props} />);
    const component = wrapper.find(BrNodeComponent);

    expect(component.contains(<BrNode component={component1} />)).toBe(true);
    expect(component.contains(<BrNode component={component2} />)).toBe(true);
  });
});
