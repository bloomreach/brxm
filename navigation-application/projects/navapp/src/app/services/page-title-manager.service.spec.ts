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
import { NavLocation } from '@bloomreach/navapp-communication';
import { Subject } from 'rxjs';

import { ConnectionService } from './connection.service';
import { PageTitleManagerService } from './page-title-manager.service';

describe('PageTitleManagerService', () => {
  let service: PageTitleManagerService;
  let titleServiceMock: jasmine.SpyObj<Title>;
  let connectionServiceMock: ConnectionService;
  let updateNavLocation$: Subject<NavLocation>;

  beforeEach(() => {
    titleServiceMock = jasmine.createSpyObj('TitleService', {
      getTitle: 'Some title',
      setTitle: undefined,
    });

    updateNavLocation$ = new Subject<NavLocation>();

    connectionServiceMock = {
      updateNavLocation$,
    } as ConnectionService;

    TestBed.configureTestingModule({
      providers: [
        PageTitleManagerService,
        { provide: Title, useValue: titleServiceMock },
        { provide: ConnectionService, useValue: connectionServiceMock },
      ],
    });

    service = TestBed.get(PageTitleManagerService);
  });

  describe('upon initialization', () => {
    beforeEach(() => {
      service.init();
    });

    it('should request initial title', () => {
      expect(titleServiceMock.getTitle).toHaveBeenCalled();
    });

    it('should subscribe on nav location updates', () => {
      expect(updateNavLocation$.observers.length).toBe(1);
    });
  });

  describe('when initialized', () => {
    beforeEach(() => {
      service.init();
    });

    it('should set the page title whenever breadcrumb label is updated', () => {
      updateNavLocation$.next({ path: '/some/path', breadcrumbLabel: 'Some breadcrumb label' });

      expect(titleServiceMock.setTitle).toHaveBeenCalledWith('Some title | Some breadcrumb label');
    });

    it('should set the default page title when breadcrumb label is absent', () => {
      updateNavLocation$.next({ path: '/some/path', breadcrumbLabel: undefined });

      expect(titleServiceMock.setTitle).toHaveBeenCalledWith('Some title');
    });
  });
});
