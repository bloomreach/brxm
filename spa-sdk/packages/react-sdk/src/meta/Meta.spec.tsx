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

jest.mock('@bloomreach/spa-sdk');

import { shallow } from 'enzyme';
import React from 'react';
import { META_POSITION_BEGIN } from '@bloomreach/spa-sdk';

import { Meta } from './Meta';
import { MetaComment } from './MetaComment';
import { mockMeta, mockNoCommentMeta } from '../../__mocks__/@bloomreach/spa-sdk';

describe('Meta', () => {
  it('should render MetaComment if meta is a comment', () => {
    const meta = mockMeta('comment-data', META_POSITION_BEGIN);
    const wrapper = shallow(<Meta meta={meta}/>);
    expect(wrapper.contains(<MetaComment meta={meta}/>)).toBe(true);
  });

  it('should not render MetaComment if meta is not a comment', () => {
    const meta = mockNoCommentMeta();
    const wrapper = shallow(<Meta meta={meta}/>);
    expect(wrapper.contains(<MetaComment meta={meta}/>)).toBe(false);
  });
});
