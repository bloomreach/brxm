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

import { Component, TYPE_COMPONENT } from './component';
import { Page } from './page';

describe('Page', () => {
  let root: Component;

  beforeEach(() => {
    root = new Component({ type: TYPE_COMPONENT });
    jest.spyOn(root, 'getComponent');
  });

  describe('getComponent', () => {
    it('should forward a call to the root component', () => {
      const page = new Page({ page: { type: TYPE_COMPONENT } }, root);
      page.getComponent('a', 'b');

      expect(root.getComponent).toBeCalledWith('a', 'b');
    });
  });

  describe('getTitle', () => {
    it('should return a page title', () => {
      const page = new Page(
        {
          page: {
            type: TYPE_COMPONENT,
            _meta: { pageTitle: 'something' },
          },
        },
        root,
      );

      expect(page.getTitle()).toBe('something');
    });

    it('should return an undefined value', () => {
      const page1 = new Page({ page: { type: TYPE_COMPONENT, _meta: {} } }, root);
      const page2 = new Page({ page: { type: TYPE_COMPONENT } }, root);

      expect(page1.getTitle()).toBeUndefined();
      expect(page2.getTitle()).toBeUndefined();
    });
  });
});
