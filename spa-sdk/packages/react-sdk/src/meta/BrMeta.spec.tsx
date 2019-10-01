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
import { mocked } from 'ts-jest/utils';
import { shallow } from 'enzyme';
import { isMetaComment, Meta } from '@bloomreach/spa-sdk';
import { BrMeta } from './BrMeta';
import { MetaComment } from './MetaComment';

jest.mock('@bloomreach/spa-sdk');

describe('BrMeta', () => {
  const meta = new class implements Meta {
    getData = jest.fn();
    getPosition = jest.fn();
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should render MetaComment if meta is a comment', () => {
    mocked(isMetaComment).mockReturnValueOnce(true);

    const wrapper = shallow(<BrMeta meta={meta}/>);
    expect(wrapper.contains(<MetaComment meta={meta}/>)).toBe(true);
  });

  it('should render nothing if meta is not determined', () => {
    mocked(isMetaComment).mockReturnValueOnce(false);

    const wrapper = shallow(<BrMeta meta={meta}/>);
    expect(wrapper.isEmptyRender()).toBe(true);
  });
});
