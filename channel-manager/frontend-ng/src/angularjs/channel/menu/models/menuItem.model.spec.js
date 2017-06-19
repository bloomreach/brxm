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

import MenuItem from './menuItem.model';

describe('MenuItem model', () => {
  it('stores the name of an item', () => {
    const item = new MenuItem('test-menu-item');
    expect(item.name).toBe('test-menu-item');
  });

  it('uses the name as the default translation key', () => {
    const item = new MenuItem('test-menu-item');
    expect(item.translationKey).toBe('test-menu-item');
  });

  it('can have a custom translation key', () => {
    const item = new MenuItem('test-menu-item', { translationKey: 'test-translation-key' });
    expect(item.translationKey).toBe('test-translation-key');
  });

  it('is visible and enabled by default', () => {
    const item = new MenuItem('test-menu-item');
    expect(item.isVisible()).toBe(true);
    expect(item.isEnabled()).toBe(true);
  });

  it('has configurable visible and enabled checks', () => {
    const isVisible = jasmine.createSpy('isVisible');
    const isEnabled = jasmine.createSpy('isEnabled');
    const item = new MenuItem('test-menu-item', {
      isVisible,
      isEnabled,
    });

    item.isEnabled();
    expect(isEnabled).toHaveBeenCalled();

    item.isVisible();
    expect(isVisible).toHaveBeenCalled();
  });

  it('NOOP onClick handler by default', () => {
    const item = new MenuItem('test-menu-item');
    item.onClick();
  });

  it('has a configurable onClick handler', () => {
    const onClick = jasmine.createSpy('onClick');
    const item = new MenuItem('test-menu-item', {
      onClick,
    });

    item.onClick();
    expect(onClick).toHaveBeenCalled();
  });

  it('has no icons by default', () => {
    const item = new MenuItem('test-menu-item');
    expect(item.hasIconName()).toBe(false);
    expect(item.hasIconSrc()).toBe(false);
    expect(item.isIconVisible()).toBe(false);
  });

  it('can have an icon name', () => {
    const item = new MenuItem('test-menu-item', { iconName: 'icon1' });
    expect(item.hasIconName()).toBe(true);
    expect(item.iconName).toBe('icon1');
  });

  it('can have an icon src', () => {
    const item = new MenuItem('test-menu-item', { iconSrc: 'icon1' });
    expect(item.hasIconSrc()).toBe(true);
    expect(item.iconSrc).toBe('icon1');
  });

  it('icons are visible by default', () => {
    const item0 = new MenuItem('test-menu-item');
    expect(item0.isIconVisible()).toBe(false);

    const item1 = new MenuItem('test-menu-item', { iconName: 'icon1' });
    expect(item1.isIconVisible()).toBe(true);

    const item2 = new MenuItem('test-menu-item', { iconSrc: 'icon1' });
    expect(item2.isIconVisible()).toBe(true);
  });
});
