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
import { XPageState } from '../../../models/xpage-state.model';
import { PageStructureService } from '../../../pages/services/page-structure.service';
import { PageService } from '../../../pages/services/page.service';
import { ProjectService } from '../../../projects/services/project.service';
import { VersionsInfo } from '../../models/versions-info.model';

import { VersionsInfoComponent } from './versions-info.component';

describe('VersionsTabComponent', () => {
  let component: VersionsInfoComponent;
  let componentEl: HTMLElement;
  let fixture: ComponentFixture<VersionsInfoComponent>;

  const testDate = Date.parse('11/08/2020 16:03');

  beforeEach(() => {
    const contentServiceMock = {
      getDocumentVersionsInfo: jest.fn(() => Promise.resolve({
        versions: [
          {
            jcrUUID: 'testId',
            comment: 'testComment',
            userName: 'testUserName',
            timestamp: testDate,
          },
        ],
      } as VersionsInfo)),
    };

    const projectServiceMock = {
      getSelectedProjectId: jest.fn(() => ({ id: 'projectId'})),
    };

    const pageServiceMock = {
      getXPageState: jest.fn(() => ({ id: 'documentId' } as XPageState)),
    };

    const pageStructureServiceMock = {
      getUnpublishedVariantId: jest.fn(() => 'unpublishedVariantId'),
    };

    TestBed.configureTestingModule({
      declarations: [VersionsInfoComponent],
      imports: [
        MatListModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        { provide: ContentService, useValue: contentServiceMock },
        { provide: ProjectService, useValue: projectServiceMock },
        { provide: PageService, useValue: pageServiceMock },
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

  it('should show versions', () => {
    component.ngOnInit();

    fixture.detectChanges();

    expect(componentEl).toMatchSnapshot();
  });
});
