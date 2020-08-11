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
import {
  Container,
  Page,
  TYPE_CONTAINER_BOX,
  TYPE_CONTAINER_INLINE,
  TYPE_CONTAINER_NO_MARKUP,
  TYPE_CONTAINER_ORDERED_LIST,
  TYPE_CONTAINER_UNORDERED_LIST,
} from '@bloomreach/spa-sdk';
import { BrContainerBox, BrContainerInline, BrContainerNoMarkup, BrContainerOrderedList, BrContainerUnorderedList } from '../cms';
import { BrNodeContainer } from './BrNodeContainer';

describe('BrNodeContainer', () => {
  const props = {
    component: { getType: jest.fn() } as unknown as jest.Mocked<Container>,
    page: {} as jest.Mocked<Page>,
  };

  beforeEach(() => {
    jest.resetAllMocks();
  });

  describe('getMapping', () => {
    it('should use container type for mapping', () => {
      shallow(<BrNodeContainer {...props} />);

      expect(props.component.getType).toBeCalled();
    });
  });

  describe('fallback', () => {
    it('should render inline container', () => {
      props.component.getType.mockReturnValue(TYPE_CONTAINER_INLINE);
      const wrapper = shallow(<BrNodeContainer {...props}><a/></BrNodeContainer>);

      expect(wrapper.equals(<BrContainerInline {...props}><a/></BrContainerInline>)).toBe(true);
    });

    it('should render no markup container', () => {
      props.component.getType.mockReturnValue(TYPE_CONTAINER_NO_MARKUP);
      const wrapper = shallow(<BrNodeContainer {...props}><a/></BrNodeContainer>);

      expect(wrapper.equals(<BrContainerNoMarkup {...props}><a/></BrContainerNoMarkup>)).toBe(true);
    });

    it('should render ordered list container', () => {
      props.component.getType.mockReturnValue(TYPE_CONTAINER_ORDERED_LIST);
      const wrapper = shallow(<BrNodeContainer {...props}><a/></BrNodeContainer>);

      expect(wrapper.equals(<BrContainerOrderedList {...props}><a/></BrContainerOrderedList>)).toBe(true);
    });

    it('should render unordered list container', () => {
      props.component.getType.mockReturnValue(TYPE_CONTAINER_UNORDERED_LIST);
      const wrapper = shallow(<BrNodeContainer {...props}><a/></BrNodeContainer>);

      expect(wrapper.equals(<BrContainerUnorderedList {...props}><a/></BrContainerUnorderedList>)).toBe(true);
    });

    it('should render box container', () => {
      props.component.getType.mockReturnValue(TYPE_CONTAINER_BOX);
      const wrapper = shallow(<BrNodeContainer {...props}><a/></BrNodeContainer>);

      expect(wrapper.equals(<BrContainerBox {...props}><a/></BrContainerBox>)).toBe(true);
    });

    it('should render box container on an unknown type', () => {
      const wrapper = shallow(<BrNodeContainer {...props} />);

      expect(wrapper.equals(<BrContainerBox {...props} />)).toBe(true);
    });
  });
});
