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

import { mount, shallow, ShallowWrapper } from 'enzyme';
import React from 'react';
import { MetaComment } from './MetaComment';
import { MetaComment as SpaMetaComment } from '@bloomreach/spa-sdk';

const meta: SpaMetaComment = {
  getData: () => 'comment-data',
  getPosition: jest.fn(),
}

describe('MetaComment', function() {
  it('should render a comment that contains data', () => {
    const wrapper = mount(<div><MetaComment meta={meta}/></div>);

    expect(wrapper.html()).toBe('<div><!--comment-data--></div>');

    wrapper.unmount();
  });

  it('should update if meta data has changed', () => {
    const shouldComponentUpdate = jest.spyOn(MetaComment.prototype, 'shouldComponentUpdate');
    const wrapper = mount(<MetaComment meta={meta}/>);
    expect(shouldComponentUpdate).not.toHaveBeenCalled();

    const newMeta = {
      getData: () => 'new-comment-data',
      getPosition: jest.fn(),
    };
    wrapper.setProps({ meta: newMeta });

    expect(shouldComponentUpdate).toHaveBeenCalled();
    expect(shouldComponentUpdate).lastReturnedWith(true);
  });

  it('should remove the comment when the component unmounts', () => {
    const wrapper = mount(<div><MetaComment meta={meta}/></div>);
    wrapper.unmount();

    expect(wrapper.contains(<MetaComment meta={meta}/>)).toBe(false);
  });

});
