/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { Container, Page } from '@bloomreach/spa-sdk';
import { BrContainerUnorderedListComponent } from './br-container-unordered-list.component';

describe('BrContainerUnorderedListComponent', () => {
  let component: BrContainerUnorderedListComponent;
  let fixture: ComponentFixture<BrContainerUnorderedListComponent>;
  let container: Container;
  let page: jest.Mocked<Page>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ BrContainerUnorderedListComponent ],
      schemas: [ NO_ERRORS_SCHEMA ],
    })
    .compileComponents();
  }));

  beforeEach(() => {
    container = {
      getChildren: () => ['item1', 'item2'],
    } as unknown as typeof container;
    page = {
      isPreview: jest.fn(),
    } as unknown as typeof page;

    fixture = TestBed.createComponent(BrContainerUnorderedListComponent);
    component = fixture.componentInstance;
    component.component = container;
    component.page = page;
  });

  it('should render a container', () => {
    fixture.detectChanges();

    expect(fixture.nativeElement).toMatchSnapshot();
  });

  describe('isPreview', () => {
    it('should add "hst-container-*" classes in preview', () => {
      page.isPreview.mockReturnValue(true);
      fixture.detectChanges();

      expect(fixture.nativeElement).toMatchSnapshot();
    });
  });
});
