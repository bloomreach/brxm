/*
 * Copyright 2020 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

import { TestBed } from '@angular/core/testing';
import { Title } from '@angular/platform-browser';
import { Subject } from 'rxjs';

import { BreadcrumbsService } from '../top-panel/services/breadcrumbs.service';

import { PageTitleManagerService } from './page-title-manager.service';

describe('PageTitleManagerService', () => {
  let service: PageTitleManagerService;
  let titleServiceMock: jasmine.SpyObj<Title>;
  let breadcrumbsServiceMock: BreadcrumbsService;
  let breadcrumbs$: Subject<string[]>;

  beforeEach(() => {
    titleServiceMock = jasmine.createSpyObj('TitleService', {
      getTitle: 'Some page title',
      setTitle: undefined,
    });

    breadcrumbs$ = new Subject<string[]>();

    breadcrumbsServiceMock = {
      breadcrumbs$,
    } as any;

    TestBed.configureTestingModule({
      providers: [
        PageTitleManagerService,
        { provide: Title, useValue: titleServiceMock },
        { provide: BreadcrumbsService, useValue: breadcrumbsServiceMock },
      ],
    });

    service = TestBed.inject(PageTitleManagerService);
  });

  describe('upon initialization', () => {
    beforeEach(() => {
      service.init();
    });

    it('should request initial title', () => {
      expect(titleServiceMock.getTitle).toHaveBeenCalled();
    });

    it('should subscribe on nav location updates', () => {
      expect(breadcrumbs$.observers.length).toBe(1);
    });
  });

  describe('when initialized', () => {
    beforeEach(() => {
      service.init();
    });

    it('should use the breadcrumb suffix when it is updated to set the page title', () => {
      breadcrumbs$.next(['Some breadcrumb', 'Some breadcrumb label']);

      expect(titleServiceMock.setTitle).toHaveBeenCalledWith('Some page title | Some breadcrumb label');
    });

    it('should use the last breadcrumb when the breadcrumb suffix is absent to set the page title', () => {
      breadcrumbs$.next(['Some breadcrumb', '']);

      expect(titleServiceMock.setTitle).toHaveBeenCalledWith('Some page title | Some breadcrumb');
    });
  });
});
