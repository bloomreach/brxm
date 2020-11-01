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
import { shallow } from 'enzyme';
import { Component } from '@bloomreach/spa-sdk';
import { BrComponent } from './BrComponent';
import { BrNode } from './BrNode';

jest.mock('@bloomreach/spa-sdk');

describe('BrComponent', () => {
  const context = {
    getChildren: jest.fn(() => []),
    getComponent: jest.fn(),
  } as unknown as jest.Mocked<Component>;

  beforeEach(() => {
    jest.restoreAllMocks();

    // @see https://github.com/airbnb/enzyme/issues/1553
    /// @ts-ignore
    BrComponent.contextTypes = {
      getChildren: () => null,
      getComponent: () => null,
    };
    delete (BrComponent as Partial<typeof BrComponent>).contextType;
  });

  it('should render children if path is not set', () => {
    const component1 = {} as Component;
    const component2 = {} as Component;
    context.getChildren.mockReturnValueOnce([component1, component2]);
    const wrapper = shallow(<BrComponent/>, { context });

    expect(context.getChildren).toBeCalled();
    expect(wrapper.contains(<BrNode component={component1} />)).toBe(true);
    expect(wrapper.contains(<BrNode component={component2} />)).toBe(true);
  });

  it('should split path by slashes', () => {
    shallow(<BrComponent path="a/b" />, { context });

    expect(context.getComponent).toBeCalledWith('a', 'b');
  });

  it('should render nothing if no component found', () => {
    const wrapper = shallow(<BrComponent path="a/b" />, { context });

    expect(wrapper.equals(<></>)).toBe(true);
  });

  it('should render found component', () => {
    const component = {} as Component;
    context.getComponent.mockReturnValueOnce(component);
    const wrapper = shallow(<BrComponent path="a/b" />, { context });

    expect(wrapper.contains(<BrNode component={component} />)).toBe(true);
  });

  it('should pass children down', () => {
    const component = {} as Component;
    context.getComponent.mockReturnValueOnce(component);
    const wrapper = shallow(<BrComponent path="a/b"><a/></BrComponent>, { context });

    expect(wrapper.contains(<BrNode component={component}><a/></BrNode>)).toBe(true);
  });
});
