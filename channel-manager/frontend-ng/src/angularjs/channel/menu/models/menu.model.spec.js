
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

import Menu from './menu.model';
import MenuAction from './menuAction.model';
import MenuDivider from './menuDivider.model';

describe('Menu model', () => {
  it('has a default width', () => {
    const menu = new Menu('test-menu');
    expect(menu.width).toBe(3);
  });

  it('can be configured with a custom width', () => {
    const menu = new Menu('test-menu', { width: 4 });
    expect(menu.width).toBe(4);
  });

  it('inits with zero items', () => {
    const menu = new Menu('test-menu');
    expect(menu.items.length).toBe(0);
  });

  it('allows actions and dividers to be added to it\'s items', () => {
    const menu = new Menu('test-menu');

    menu.add(new MenuDivider('a'));
    expect(menu.items.length).toBe(1);

    menu.add(new MenuAction('b'));
    expect(menu.items.length).toBe(2);
  });

  it('can detect if any of it\'s items has an icon defined', () => {
    const menu = new Menu('test-menu');
    expect(menu.hasIcons()).toBe(false);

    const divider = new MenuDivider('divider-1');
    menu.add(divider);
    expect(menu.hasIcons()).toBe(false);

    const action = new MenuAction('action1', { iconName: 'iconName' });
    menu.add(action);
    expect(menu.hasIcons()).toBe(true);

    delete action.iconName;
    expect(menu.hasIcons()).toBe(false);

    action.iconSrc = 'iconSrc';
    expect(menu.hasIcons()).toBe(true);
  });
});
