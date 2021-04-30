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

import { Pipe, PipeTransform } from '@angular/core';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { MatIconModule } from '@angular/material/icon';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { TranslateModule } from '@ngx-translate/core';

import { Ng1IframeService, NG1_IFRAME_SERVICE } from '../../../services/ng1/iframe.ng1.service';
import { NG1_UI_ROUTER_GLOBALS } from '../../../services/ng1/ui-router-globals.ng1.service';
import { Ng1WorkflowService, NG1_WORKFLOW_SERVICE } from '../../../services/ng1/workflow.ng1.service';
import { Version } from '../../models/version.model';
import { VersionsInfo } from '../../models/versions-info.model';
import { VersionsService } from '../../services/versions.service';

import { VersionComponent } from './version.component';

@Pipe({ name: 'moment' })
export class MomentMockPipe implements PipeTransform {
  transform(value: number | string | Date, format?: string): string {
    return `${value}`;
  }
}

describe('VersionComponent', () => {
  let component: VersionComponent;
  let fixture: ComponentFixture<VersionComponent>;
  let componentEl: HTMLElement;

  let ng1IframeService: Ng1IframeService;
  let versionsService: VersionsService;
  let ng1WorkflowService: Ng1WorkflowService;

  const date = Date.parse('11/08/2020 16:03');
  const path = '/some/test/path';
  const renderPath = `${path}?withParam=test`;
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
    const iframeServiceMock = {
      load: jest.fn(() => Promise.resolve()),
    };

    const workflowServiceMock = {
      createWorkflowAction: jest.fn(() => Promise.resolve()),
    };

    const versionsServiceMock = {
      getVersionsInfo: () => Promise.resolve(),
    };

    const uiRouterGlobalsMock = {
      params: {
        documentId,
      },
    };

    TestBed.configureTestingModule({
      declarations: [
        VersionComponent,
        MomentMockPipe,
      ],
      imports: [
        MatIconModule,
        MatIconTestingModule,
        TranslateModule.forRoot(),
      ],
      providers: [
       { provide: NG1_IFRAME_SERVICE, useValue: iframeServiceMock },
       { provide: NG1_WORKFLOW_SERVICE, useValue: workflowServiceMock },
       { provide: NG1_UI_ROUTER_GLOBALS, useValue: uiRouterGlobalsMock },
       { provide: VersionsService, useValue: versionsServiceMock },
       ],
    })
      .compileComponents();

    ng1WorkflowService = TestBed.inject(NG1_WORKFLOW_SERVICE);
    ng1IframeService = TestBed.inject(NG1_IFRAME_SERVICE);
    versionsService = TestBed.inject(VersionsService);
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(VersionComponent);
    component = fixture.componentInstance;
    componentEl = fixture.nativeElement;

    component.versionsInfo = mockVersionsInfo;
    component.version = mockVersion;
    component.renderPath = renderPath;
    component.isSelected = false;

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
