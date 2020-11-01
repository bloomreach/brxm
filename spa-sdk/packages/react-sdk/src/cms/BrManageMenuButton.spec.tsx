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
import { mocked } from 'ts-jest/utils';
import { shallow } from 'enzyme';
import { Menu, MetaCollection, Page, isMenu } from '@bloomreach/spa-sdk';
import { BrManageMenuButton } from './BrManageMenuButton';
import { BrMeta } from '../meta';
import { BrNodeComponent } from '../component';

jest.mock('@bloomreach/spa-sdk');

describe('BrManageMenuButton', () => {
  const context = {
    isPreview: jest.fn(),
    getMeta: jest.fn(),
  } as unknown as jest.Mocked<Page>;
  let props: React.ComponentProps<typeof BrManageMenuButton>;

  beforeEach(() => {
    jest.restoreAllMocks();

    props = { menu: {} as Menu };

    // @see https://github.com/airbnb/enzyme/issues/1553
    /// @ts-ignore
    BrManageMenuButton.contextTypes = {
      isPreview: () => null,
      getMeta: () => null,
    };
    delete (BrManageMenuButton as Partial<typeof BrManageMenuButton>).contextType;
  });

  it('should only render in preview mode', () => {
    context.isPreview.mockReturnValueOnce(false);
    const wrapper = shallow(<BrManageMenuButton {...props} />, { context });

    expect(wrapper.html()).toBe(null);
  });

  it('should only render if meta-data is available', () => {
    context.isPreview.mockReturnValueOnce(true);
    const wrapper = shallow(<BrManageMenuButton {...props} />, { context });

    expect(wrapper.html()).toBe(null);
  });

  it('should render menu-button meta-data created with page context', () => {
    const meta = {} as MetaCollection;
    context.getMeta.mockReturnValueOnce(meta);
    context.isPreview.mockReturnValueOnce(true);
    props.menu._meta = {};
    const wrapper = shallow(<BrManageMenuButton {...props} />, { context });

    expect(context.getMeta).toHaveBeenCalledWith(props.menu._meta);
    expect(
      wrapper
        .find(BrMeta)
        .first()
        .prop('meta'),
    ).toBe(meta);
  });

  it('should render a menu-button meta-data', () => {
    const meta = {} as MetaCollection;
    props.menu = { getMeta: jest.fn(() => meta) } as unknown as Menu;
    context.isPreview.mockReturnValueOnce(true);
    mocked(isMenu).mockReturnValueOnce(true);
    const wrapper = shallow(<BrManageMenuButton {...props} />, { context });

    expect(
      wrapper
        .find(BrMeta)
        .first()
        .prop('meta'),
    ).toBe(meta);
  });
});
