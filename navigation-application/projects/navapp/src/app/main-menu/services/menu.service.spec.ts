/*
 * Copyright 2019 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

import { async, fakeAsync, tick } from '@angular/core/testing';
import { of } from 'rxjs';

import { NavigationConfigurationService } from '../../services';
import { menuStructureMock, navigationConfigurationMapMock } from '../../test-mocks';
import { MenuItem, MenuItemContainer, MenuItemLink } from '../models';

import { MenuBuilderService } from './menu-builder.service';
import { MenuStructureService } from './menu-structure.service';

describe('MenuBuilderService', () => {
  const navConfigServiceMock: NavigationConfigurationService = {
    navigationConfiguration$: of(navigationConfigurationMapMock),
  } as any;

  const structureMock: MenuStructureService = {
    getMenuStructure: () => menuStructureMock,
  } as any;

  let service: MenuBuilderService;

  beforeEach(async(() => {
    service = new MenuBuilderService(navConfigServiceMock, structureMock);
  }));

  it('should get the filtered menu populated with app paths', fakeAsync(() => {
    let actual: MenuItem[];
    const expected = (() => {
      const subsubitem1 = new MenuItemLink(
        'subsubitem1',
        'Sub sub item 1',
      );
      subsubitem1.appId = 'iframe1-url';
      subsubitem1.appPath = 'app-path-for-sub-sub-item1';

      const subitem2 = new MenuItemLink(
        'subitem2',
        'Sub item 2',
      );
      subitem2.appId = 'iframe2-url';
      subitem2.appPath = 'app-path-for-sub-item2';

      return [
        new MenuItemContainer(
          'Item 2',
          [
            new MenuItemContainer(
              'Sub item 1',
              [subsubitem1],
            ),
            subitem2,
          ],
          'br-logo',
        ),
      ];
    })();

    service.buildMenu().subscribe(menu => actual = menu);

    tick();

    expect(actual).toEqual(expected);
  }));
});
