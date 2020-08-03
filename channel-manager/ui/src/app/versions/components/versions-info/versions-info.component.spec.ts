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

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatListModule } from '@angular/material/list';
import { TranslateModule } from '@ngx-translate/core';

import { ContentService } from '../../../content/services/content.service';
import { PageStructureService } from '../../../page-structure/services/page-structure.service';

import { VersionsInfoComponent } from './versions-info.component';

describe('VersionsTabComponent', () => {
  let component: VersionsInfoComponent;
  let componentEl: HTMLElement;
  let fixture: ComponentFixture<VersionsInfoComponent>;

  beforeEach(() => {
    const contentServiceMock = {};
    const pageStructureServiceMock = {
      getPage: () => ({
        getMeta: () => ({
          getBranchId: jest.fn(() => 'testBranchId'),
          getUnpublishedVariantId: jest.fn(() => 'testUnpublishedVariantId'),
        }),
      }),
    };

    TestBed.configureTestingModule({
      declarations: [VersionsInfoComponent],
      imports: [
        MatListModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        { provide: ContentService, useValue: contentServiceMock },
        { provide: PageStructureService, useValue: pageStructureServiceMock },
      ],
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(VersionsInfoComponent);
    component = fixture.componentInstance;
    componentEl = fixture.nativeElement;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(componentEl).toMatchSnapshot();
  });
});
