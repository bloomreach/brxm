/*
 * Copyright 2019-2021 Hippo B.V. (http://www.onehippo.com)
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
import { Component, Page } from '@bloomreach/spa-sdk';
import { BrNodeComponent } from './BrNodeComponent';

describe('BrNodeComponent', () => {
  const context = {
    test: ({ children }: React.PropsWithChildren<typeof props>) => <a>{children}</a>,
  };
  const props = {
    component: { getName: jest.fn() } as unknown as jest.Mocked<Component>,
    page: {} as jest.Mocked<Page>,
  };

  beforeEach(() => {
    jest.resetAllMocks();
  });

  describe('getMapping', () => {
    it('should use component name for mapping', () => {
      shallow(<BrNodeComponent {...props} />);

      expect(props.component.getName).toBeCalled();
    });
  });

  describe('render', () => {
    beforeEach(() => {
      // @see https://github.com/airbnb/enzyme/issues/1553
      /// @ts-ignore
      BrNodeComponent.contextTypes = { test: () => null };
      delete (BrNodeComponent as Partial<typeof BrNodeComponent>).contextType;
    });

    it('should fallback when there is no mapping', () => {
      props.component.getName.mockReturnValue('something');
      const wrapper = shallow(<BrNodeComponent {...props}><b/></BrNodeComponent>, { context });

      expect(wrapper.equals(<b/>)).toBe(true);
    });

    it('should render a mapped component', () => {
      props.component.getName.mockReturnValue('test');
      const wrapper = mount(<BrNodeComponent {...props}><b/></BrNodeComponent>, { context });

      expect(wrapper.html()).toBe('<a><b></b></a>');
    });

    it('should render children on a fallback', () => {
      const wrapper = shallow(<BrNodeComponent {...props}><b/></BrNodeComponent>);

      expect(wrapper.equals(<b/>)).toBe(true);
    });
  });
});
