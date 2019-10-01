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
import { Container, Page } from '@bloomreach/spa-sdk';
import { BrContainerOrderedList } from './BrContainerOrderedList';

describe('BrContainerOrderedList', () => {
  const props = {
    component: {} as jest.Mocked<Container>,
    page: { isPreview: jest.fn() } as unknown as jest.Mocked<Page>,
  };

  beforeEach(() => {
    jest.resetAllMocks();
  });

  it('should render itself as ol element', () => {
    const wrapper = shallow(<BrContainerOrderedList {...props} />);

    expect(wrapper.equals(<ol/>)).toBe(true);
  });

  it('should render children as li elements', () => {
    const wrapper = shallow(
      <BrContainerOrderedList {...props}>
        <a/>
        <b/>
      </BrContainerOrderedList>
    );

    expect(wrapper.equals(
      <ol>
        <li><a/></li>
        <li><b/></li>
      </ol>
    )).toBe(true);
  });

  it('should render preview classes', () => {
    props.page.isPreview.mockReturnValue(true);

    const wrapper = shallow(
      <BrContainerOrderedList {...props}>
        <a/>
        <b/>
      </BrContainerOrderedList>
    );

    expect(wrapper.equals(
      <ol className="hst-container">
        <li className="hst-container-item"><a/></li>
        <li className="hst-container-item"><b/></li>
      </ol>
    )).toBe(true);
  });
});
