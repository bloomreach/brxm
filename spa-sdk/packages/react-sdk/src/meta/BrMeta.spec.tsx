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
import { mount } from 'enzyme';
import { MetaComment, META_POSITION_BEGIN, META_POSITION_END, isMetaComment } from '@bloomreach/spa-sdk';
import { BrMeta } from './BrMeta';

describe('BrMeta', () => {
  const meta = [
    new class implements MetaComment {
      getData = jest.fn(() => 'begin comment 1');
      getPosition = jest.fn(() => META_POSITION_BEGIN as any);
    },
    new class implements MetaComment {
      getData = jest.fn(() => 'begin comment 2');
      getPosition = jest.fn(() => META_POSITION_BEGIN as any);
    },
    new class implements MetaComment {
      getData = jest.fn(() => 'end comment 1');
      getPosition = jest.fn(() => META_POSITION_END as any);
    },
    new class implements MetaComment {
      getData = jest.fn(() => 'end comment 2');
      getPosition = jest.fn(() => META_POSITION_END as any);
    },
  ];

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('componentDidMount', () => {
    it('should render comments surrounding children', () => {
      mocked(isMetaComment).mockReturnValue(true);

      const wrapper = mount((
        <div>
          <BrMeta meta={meta}>
            <a/>
            <b/>
          </BrMeta>
        </div>
      ));

      mocked(isMetaComment).mockReset();

      expect(wrapper.html()).toMatchSnapshot();
    });

    it('should render comment meta only', () => {
      mocked(isMetaComment).mockReturnValueOnce(true);

      const wrapper = mount((
        <div>
          <BrMeta meta={meta}/>
        </div>
      ));

      expect(wrapper.html()).toMatchSnapshot();
    });
  });

  describe('componentDidUpdate', () => {
    it('should rerender comments on update', () => {
      mocked(isMetaComment).mockReturnValue(true);

      const container = document.createElement('div');
      const wrapper = mount(<BrMeta meta={meta}><a/></BrMeta>, { attachTo: container });
      wrapper.setProps({ meta: [meta[0], meta[2]] });

      mocked(isMetaComment).mockReset();

      expect(container.innerHTML).toMatchSnapshot();
    });
  });

  describe('componentWillUnmount', () => {
    it('should remove the comment when the component unmounts', () => {
      mocked(isMetaComment).mockReturnValue(true);

      const container = document.createElement('div');
      const wrapper = mount(<div><BrMeta meta={meta} /></div>, { attachTo: container });
      wrapper.detach();

      expect(container.innerHTML).toBe('');
    });
  });

  describe('render', () => {
    it('should render only children if there is no meta', () => {
      const wrapper = mount((
        <div>
          <BrMeta meta={[]}>
            <a/>
            <b/>
          </BrMeta>
        </div>
      ));

      expect(wrapper.html()).toMatchSnapshot();
    });
  });
});
