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

import { of } from 'rxjs';

import { MenuStateService } from '../../main-menu/services/menu-state.service';

import { BreadcrumbsService } from './breadcrumbs.service';

describe('BreadcrumbsService', () => {
  let menuStateService: MenuStateService;
  let service: BreadcrumbsService;

  const activePathMock$ = of([
    { caption: 'some' },
    { caption: 'active' },
    { caption: 'path' },
   ]);

  beforeEach(() => {
    menuStateService = { activePath$: activePathMock$ } as MenuStateService; // TODO: used DI

    service = new BreadcrumbsService(menuStateService);
  });

  it('should emit breadcrumbs when there is a menu active path', () => {
    const expected = ['some', 'active', 'path', ''];
    let actual: string[];

    service.breadcrumbs$.subscribe(x => actual = x);

    expect(actual).toEqual(expected);
  });

  describe('when the suffix is set', () => {
    beforeEach(() => {
      service.setSuffix('suffix');
    });

    it('should emit breadcrumbs with the suffix', () => {
      const expected = ['some', 'active', 'path', 'suffix'];
      let actual: string[];

      service.breadcrumbs$.subscribe(x => actual = x);

      expect(actual).toEqual(expected);
    });

    it('should clear the suffix', () => {
      const expected = ['some', 'active', 'path', ''];
      let actual: string[];

      service.clearSuffix();

      service.breadcrumbs$.subscribe(x => actual = x);

      expect(actual).toEqual(expected);
    });
  });
});
