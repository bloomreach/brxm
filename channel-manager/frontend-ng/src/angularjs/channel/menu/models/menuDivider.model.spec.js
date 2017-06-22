/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import MenuDivider from './menuDivider.model';

describe('MenuDivider model', () => {
  it('is of type "divider"', () => {
    const divider = new MenuDivider('test-menu-action');
    expect(divider.type).toBe('divider');
  });

  it('is visible by default', () => {
    const divider = new MenuDivider('test-menu-action');
    expect(divider.isVisible()).toBe(true);
  });

  it('has a configurable visible check', () => {
    const isVisible = jasmine.createSpy('isVisible');
    const divider = new MenuDivider('test-menu-action', {
      isVisible,
    });

    divider.isVisible();
    expect(isVisible).toHaveBeenCalled();
  });
});
