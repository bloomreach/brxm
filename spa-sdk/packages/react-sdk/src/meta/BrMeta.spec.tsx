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
import { mount } from 'enzyme';
import { MetaCollection } from '@bloomreach/spa-sdk';
import { BrMeta } from './BrMeta';

describe('BrMeta', () => {
  let meta: jest.Mocked<MetaCollection>;

  beforeEach(() => {
    meta = {
      render: jest.fn(),
      length: 1,
    } as unknown as jest.Mocked<MetaCollection>;
  });

  it('should render meta-data surrounding children', () => {
    mount((
      <div>
        <BrMeta meta={meta}>
          <a/>
          <b/>
        </BrMeta>
      </div>
    ));

    expect(meta.render).toBeCalled();

    const [head, tail] = meta.render.mock.calls[0];

    expect(head).toMatchSnapshot();
    expect(tail.previousSibling).toMatchSnapshot();
  });

  it('should rerender meta-data on update', () => {
    const clear = jest.fn();
    meta.render.mockReturnValueOnce(clear);

    const container = document.createElement('div');
    const wrapper = mount(<BrMeta meta={meta}><a/></BrMeta>, { attachTo: container });
    const newMeta = { length: 1, render: jest.fn() };
    wrapper.setProps({ meta: newMeta });

    expect(clear).toBeCalled();
    expect(newMeta.render).toBeCalled();
  });

  it('should clear meta-data when the component unmounts', () => {
    const clear = jest.fn();
    meta.render.mockReturnValueOnce(clear);

    const container = document.createElement('div');
    const wrapper = mount(<div><BrMeta meta={meta} /></div>, { attachTo: container });
    wrapper.detach();

    expect(clear).toBeCalled();
  });

  it('should render only children if there is no meta', () => {
    meta.length = 0;

    const wrapper = mount((
      <div>
        <BrMeta meta={meta}>
          <a/>
          <b/>
        </BrMeta>
      </div>
    ));

    expect(wrapper.html()).toMatchSnapshot();
  });
});
