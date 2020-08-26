/*!
 * Copyright 2020 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
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

import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PageService } from '../../../services/page.service';

import { NotificationBarComponent } from './notification-bar.component';

describe('NotificationBarComponent', () => {
  let component: NotificationBarComponent;
  let fixture: ComponentFixture<NotificationBarComponent>;

  let pageServiceMock: PageService;

  beforeEach(() => {
    pageServiceMock = {
      getPageStatusInfo: jest.fn(() => ({})),
    } as unknown as typeof pageServiceMock;

    fixture = TestBed.configureTestingModule({
      declarations: [NotificationBarComponent],
      providers: [
        { provide: PageService, useValue: pageServiceMock },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).createComponent(NotificationBarComponent);

    component = fixture.componentInstance;
  });

  it('should show the component', () => {
    component.pageStates = {};

    component.ngOnChanges();

    fixture.detectChanges();

    expect(fixture.nativeElement).toMatchSnapshot();
  });

  it('should calculate the page status', () => {
    component.ngOnChanges();

    expect(pageServiceMock.getPageStatusInfo).toHaveBeenCalled();
  });
});
