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

import { Component, Input } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { BehaviorSubject } from 'rxjs';
import { Document, MetaCollection, Page } from '@bloomreach/spa-sdk';
import { BrManageContentButtonDirective } from './br-manage-content-button.directive';
import { BrPageComponent } from './br-page/br-page.component';

@Component({ template: '<a [brManageContentButton]="content"></a>' })
class TestComponent {
  @Input() content!: Document;
}

describe('BrManageContentButtonDirective', () => {
  let content: Document;
  let meta: jest.Mocked<MetaCollection>;
  let page: jest.Mocked<Page>;
  let fixture: ComponentFixture<TestComponent>;

  beforeEach(() => {
    content = {} as typeof content;
    meta = { render: jest.fn() } as unknown as typeof meta;
    page = { getButton: jest.fn(() => meta) } as unknown as typeof page;
  });

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ BrManageContentButtonDirective, TestComponent ],
      providers: [
        { provide: BrPageComponent, useValue: { state: new BehaviorSubject(page) } },
      ],
    })
    .compileComponents();

    fixture = TestBed.createComponent(TestComponent);
    fixture.componentInstance.content = content;
    fixture.detectChanges();
  }));

  describe('ngOnChanges', () => {
    it('should render a meta', () => {
      expect(meta.render).toBeCalledWith(
        fixture.nativeElement.querySelector('a'),
        fixture.nativeElement.querySelector('a'),
      );
    });
  });
});
