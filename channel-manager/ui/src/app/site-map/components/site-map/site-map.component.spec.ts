/*
 * Copyright 2020-2023 Bloomreach
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

import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { MatTreeModule } from '@angular/material/tree';
import { TranslateModule } from '@ngx-translate/core';
import { Subject } from 'rxjs';

import { IframeService } from '../../../channels/services/iframe.service';
import { NG1_CHANNEL_SERVICE } from '../../../services/ng1/channel.ng1.service';
import { NG1_ROOT_SCOPE } from '../../../services/ng1/root-scope.ng1.service';
import { SiteMapService } from '../../services/site-map.service';

import { SiteMapComponent } from './site-map.component';

describe('SiteMapComponent', () => {
  let component: SiteMapComponent;
  let componentEl: HTMLElement;
  let fixture: ComponentFixture<SiteMapComponent>;
  let iframeService: IframeService;

  const siteMapItemsSubject = new Subject();
  const searchItemsSubject = new Subject();

  const scrollIntoViewMock = jest.fn();
  window.HTMLElement.prototype.scrollIntoView = scrollIntoViewMock;

  const siteMapServiceMock = {
    items$: siteMapItemsSubject.asObservable(),
    search$: searchItemsSubject.asObservable(),
    search: jest.fn(),
    load: jest.fn(),
    loadItem: jest.fn(),
  };

  const rootScopeMock: Partial<ng.IRootScopeService> = {
    $on: jest.fn(),
  };

  const iframeServiceMock = {
    load: jest.fn(() => Promise.resolve()),
  };

  const channelServiceMock = {
    getSiteMapId(): string {
      return 'siteMapId';
    },
  };

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [
        SiteMapComponent,
      ],
      imports: [
        FormsModule,
        MatTreeModule,
        MatIconModule,
        MatIconTestingModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        { provide: IframeService, useValue: iframeServiceMock },
        { provide: SiteMapService, useValue: siteMapServiceMock },
        { provide: NG1_CHANNEL_SERVICE, useValue: channelServiceMock },
        { provide: NG1_ROOT_SCOPE, useValue: rootScopeMock },
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
    }).compileComponents();

    iframeService = TestBed.inject(IframeService);
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SiteMapComponent);
    component = fixture.componentInstance;
    componentEl = fixture.nativeElement;

    (component as any).onLoadSiteMapUnsubscribe = jest.fn();
  });

  describe('no tree items', () => {
    it('should display empty tree', () => {
      siteMapItemsSubject.next([]);

      fixture.detectChanges();

      expect(componentEl.querySelector('mat-tree')).toMatchSnapshot();
    });
  });
});
