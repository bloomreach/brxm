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

import angular from 'angular';
import 'angular-mocks';
import MenuService from './menu.service';

describe('MenuService', () => {
  let menuService;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.menu');
    menuService = new MenuService();
  });

  it('can define a new menu', () => {
    menuService.defineMenu('testMenu');

    const menu = menuService.getMenu();
    expect(menu).toBeDefined();
    expect(menu.name).toBe('testMenu');
    expect(menu.items.length).toBe(0);
  });

  it('allows menu actions to be added', () => {
    menuService.defineMenu('testMenu');

    menuService.addAction('action1');
    expect(menuService.menu.items.length).toBe(1);
    expect(menuService.menu.items[0].name).toBe('action1');

    menuService.addAction('action2');
    expect(menuService.menu.items.length).toBe(2);
    expect(menuService.menu.items[1].name).toBe('action2');
  });

  it('allows menu dividers to be added', () => {
    menuService.defineMenu('testMenu');

    menuService.addDivider();
    expect(menuService.menu.items.length).toBe(1);
    expect(menuService.menu.items[0].name).toBe('divider-0');

    menuService.addDivider();
    expect(menuService.menu.items.length).toBe(2);
    expect(menuService.menu.items[1].name).toBe('divider-1');
  });

  it('has an optional callback handler for showing sub pages', () => {
    const showSubpageHandler = jasmine.createSpy('showSubpageHandler');
    menuService.defineMenu('testMenu');

    // nothing should happen if no handler has been registered
    menuService.showSubPage('test-page');

    menuService.getMenu(showSubpageHandler);
    menuService.showSubPage('test-page');
    expect(showSubpageHandler).toHaveBeenCalledWith('test-page');
  });

  it('should allow chaining when defining a menu', () => {
    menuService
      .defineMenu('testMenu')
      .addAction('testAction')
      .addDivider();
  });
});

