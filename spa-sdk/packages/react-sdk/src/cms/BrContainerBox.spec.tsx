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
import { BrContainerBox } from './BrContainerBox';

describe('BrContainerBox', () => {
  const props = {
    component: {} as jest.Mocked<Container>,
    page: { isPreview: jest.fn() } as unknown as jest.Mocked<Page>,
  };

  beforeEach(() => {
    jest.resetAllMocks();
  });

  it('should render itself as div element', () => {
    const wrapper = shallow(<BrContainerBox {...props} />);

    expect(wrapper.equals(<div/>)).toBe(true);
  });

  it('should render children as div elements', () => {
    const wrapper = shallow(
      <BrContainerBox {...props}>
        <a/>
        <b/>
      </BrContainerBox>
    );

    expect(wrapper.equals(
      <div>
        <div><a/></div>
        <div><b/></div>
      </div>
    )).toBe(true);
  });

  it('should render preview classes', () => {
    props.page.isPreview.mockReturnValue(true);

    const wrapper = shallow(
      <BrContainerBox {...props}>
        <a/>
        <b/>
      </BrContainerBox>
    );

    expect(wrapper.equals(
      <div className="hst-container">
        <div className="hst-container-item"><a/></div>
        <div className="hst-container-item"><b/></div>
      </div>
    )).toBe(true);
  });
});
