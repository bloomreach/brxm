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

import { CUSTOM_ELEMENTS_SCHEMA, Pipe, PipeTransform } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { MatIconModule } from '@angular/material/icon';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule } from '@ngx-translate/core';

import { DocumentWorkflowService } from '../../../services/document-workflow.service';
import { NG1_IFRAME_SERVICE } from '../../../services/ng1/iframe.ng1.service';
import { NG1_UI_ROUTER_GLOBALS } from '../../../services/ng1/ui-router-globals.ng1.service';
import { Version } from '../../models/version.model';
import { VersionsInfo } from '../../models/versions-info.model';
import { VersionsService } from '../../services/versions.service';

import { VersionComponent } from './version.component';

@Pipe({ name: 'moment' })
class MomentMockPipe implements PipeTransform {
  transform(value: number | string | Date, format?: string): string {
    return `${value}`;
  }
}

describe('VersionComponent', () => {
  let component: VersionComponent;
  let fixture: ComponentFixture<VersionComponent>;
  let componentEl: HTMLElement;

  const path = '/some/test/path';
  const renderPath = `${path}?withParam=test`;
  const documentId = 'testDocumentId';
  const firstVersionUUID = 'testId';
  const date = Date.parse('11/08/2020 16:03');
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

  beforeEach(waitForAsync(() => {
    const iframeServiceMock = {
      load: jest.fn(() => Promise.resolve()),
    };

    const documentWorkflowServiceMock = {
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
        MatTooltipModule,
        MatIconModule,
        MatIconTestingModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        { provide: NG1_IFRAME_SERVICE, useValue: iframeServiceMock },
        { provide: DocumentWorkflowService, useValue: documentWorkflowServiceMock },
        { provide: NG1_UI_ROUTER_GLOBALS, useValue: uiRouterGlobalsMock },
        { provide: VersionsService, useValue: versionsServiceMock },
      ],
      schemas: [
        CUSTOM_ELEMENTS_SCHEMA,
      ],
    })
      .compileComponents();
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
