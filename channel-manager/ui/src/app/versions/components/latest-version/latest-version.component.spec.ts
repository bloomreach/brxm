/*!
 * Copyright 2021 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
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

import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { MatIconModule } from '@angular/material/icon';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { TranslateModule } from '@ngx-translate/core';

import { DocumentWorkflowService } from '../../../services/document-workflow.service';
import { NG1_UI_ROUTER_GLOBALS } from '../../../services/ng1/ui-router-globals.ng1.service';
import { Version } from '../../models/version.model';
import { VersionsInfo } from '../../models/versions-info.model';
import { VersionsService } from '../../services/versions.service';

import { LatestVersionComponent } from './latest-version.component';

describe('LatestVersionComponent', () => {
  let component: LatestVersionComponent;
  let fixture: ComponentFixture<LatestVersionComponent>;
  let componentEl: HTMLElement;

  const date = Date.parse('11/08/2020 16:03');
  const documentId = 'testDocumentId';
  const firstVersionUUID = 'testId';
  const mockVersion = {
    jcrUUID: firstVersionUUID,
    branchId: 'master',
    userName: 'testUserName',
    timestamp: date,
  } as Version;

  const mockVersionsInfo = {
    versions: [ mockVersion ],
    restoreEnabled: true,
    createEnabled: true,
    labelEnabled: true,
    campaignEnabled: true,
  } as VersionsInfo;

  beforeEach(async(() => {
    const documentWorkflowServiceMock = {
      createWorkflowAction: jest.fn(() => { }),
    };

    const versionsServiceMock = {
      getVersionsInfo: jest.fn(() => Promise.resolve()),
    };

    const uiRouterGlobalsMock = {
      params: {
        documentId,
      },
    };

    TestBed.configureTestingModule({
      declarations: [
        LatestVersionComponent,
      ],
      imports: [
        MatIconModule,
        MatIconTestingModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        { provide: DocumentWorkflowService, useValue: documentWorkflowServiceMock },
        { provide: NG1_UI_ROUTER_GLOBALS, useValue: uiRouterGlobalsMock },
        { provide: VersionsService, useValue: versionsServiceMock },
      ],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(LatestVersionComponent);
    component = fixture.componentInstance;
    componentEl = fixture.nativeElement;

    component.versionsInfo = mockVersionsInfo;
    component.version = mockVersion;
    component.isSelected = false;

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
