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
import { shallow, ShallowWrapper } from 'enzyme';
import { MetaComment, META_POSITION_BEGIN, META_POSITION_END } from '@bloomreach/spa-sdk';
import { BrMeta } from './BrMeta';
import { BrMetaWrapper } from './BrMetaWrapper';

describe('BrMetaWrapper', () => {
  const children = <div/>;
  const metaBegin = new class implements MetaComment {
    getData = jest.fn();
    getPosition = jest.fn();
  };
  const metaEnd = new class implements MetaComment {
    getData = jest.fn();
    getPosition = jest.fn();
  };

  let wrapper: ShallowWrapper<React.ComponentProps<typeof BrMetaWrapper>> ;

  beforeEach(() => {
    jest.clearAllMocks();
    metaBegin.getPosition.mockReturnValue(META_POSITION_BEGIN);
    metaEnd.getPosition.mockReturnValue(META_POSITION_END);

    wrapper = shallow(<BrMetaWrapper meta={[metaEnd, metaBegin]}>{children}</BrMetaWrapper>);
  });

  it('should render meta data', () => {
    expect(wrapper.contains(<BrMeta meta={metaBegin} />)).toBe(true);
    expect(wrapper.contains(<BrMeta meta={metaEnd} />)).toBe(true);
  });

  it('should render meta in the right order', () => {
    const brMeta = wrapper.find(BrMeta);

    expect(brMeta.at(0).prop('meta')).toBe(metaBegin);
    expect(brMeta.at(1).prop('meta')).toBe(metaEnd);
  });

  it('should render children', () => {
    expect(wrapper.contains(children)).toBe(true);
  });
});
