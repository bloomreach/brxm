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

import { CUSTOM_ELEMENTS_SCHEMA, Pipe, PipeTransform } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { MatIconModule } from '@angular/material/icon';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { MatListModule } from '@angular/material/list';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { TranslateModule } from '@ngx-translate/core';

import { Ng1ChannelService, NG1_CHANNEL_SERVICE } from '../../../services/ng1/channel.ng1.service';
import { NG1_CONTENT_SERVICE } from '../../../services/ng1/content.ng1.service';
import { Ng1IframeService, NG1_IFRAME_SERVICE } from '../../../services/ng1/iframe.ng1.service';
import { Ng1WorkflowService, NG1_WORKFLOW_SERVICE } from '../../../services/ng1/workflow.ng1.service';
import { VersionsInfo } from '../../models/versions-info.model';

import { VersionsInfoComponent } from './versions-info.component';

@Pipe({name: 'moment'})
export class MomentMockPipe implements PipeTransform {
  transform(value: number | string | Date, format?: string): string {
    return `${value}`;
  }
}

describe('VersionsInfoComponent', () => {
  let component: VersionsInfoComponent;
  let componentEl: HTMLElement;
  let fixture: ComponentFixture<VersionsInfoComponent>;
  let ng1IframeService: Ng1IframeService;
  let ng1ChannelService: Ng1ChannelService;
  let ng1WorkflowService: Ng1WorkflowService;

  const date = Date.parse('11/08/2020 16:03');
  const path = '/some/test/path';
  const renderPath = `${path}?withParam=test`;
  const homePageRenderPath = '';
  const firstVersionUUID = 'testId';
  const secondVersionUUID = 'testVariantId';
  const mockVersionsInfo = {
    versions: [
      {
        jcrUUID: firstVersionUUID,
        userName: 'testUserName',
        timestamp: date,
      },
      {
        jcrUUID: secondVersionUUID,
        userName: 'testUserName2',
        timestamp: date,
      },
    ],
    restoreEnabled: true,
    createEnabled: true,
  } as VersionsInfo;

  beforeEach(() => {
    const contentServiceMock = {
      getDocumentVersionsInfo: jest.fn(() => Promise.resolve(mockVersionsInfo)),
    };

    const channelServiceMock = {
      makeRenderPath: () => path,
      getHomePageRenderPathInfo: () => homePageRenderPath,
    };

    const iframeServiceMock = {
      getCurrentRenderPathInfo: () => path,
      load: jest.fn(() => Promise.resolve()),
    };

    const workflowServiceMock = {
      createWorkflowAction: jest.fn(() => { }),
    };

    TestBed.configureTestingModule({
      declarations: [
        VersionsInfoComponent,
        MomentMockPipe,
      ],
      imports: [
        MatListModule,
        MatIconModule,
        MatIconTestingModule,
        MatProgressBarModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        { provide: NG1_CONTENT_SERVICE, useValue: contentServiceMock },
        { provide: NG1_CHANNEL_SERVICE, useValue: channelServiceMock },
        { provide: NG1_IFRAME_SERVICE, useValue: iframeServiceMock },
        { provide: NG1_WORKFLOW_SERVICE, useValue: workflowServiceMock },
      ],
      schemas: [
        CUSTOM_ELEMENTS_SCHEMA,
      ],
    });

    ng1IframeService = TestBed.inject(NG1_IFRAME_SERVICE);
    ng1ChannelService = TestBed.inject(NG1_CHANNEL_SERVICE);
    ng1WorkflowService = TestBed.inject(NG1_WORKFLOW_SERVICE);
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(VersionsInfoComponent);
    component = fixture.componentInstance;
    componentEl = fixture.nativeElement;

    component.documentId = 'testDocumentId';
    component.branchId = 'projectId';
    component.unpublishedVariantId = 'testId';
    fixture.detectChanges();
  });

  describe('initial rendering', () => {
    it('should show list of versions', fakeAsync(() => {
      component.ngOnInit();
      tick();
      fixture.detectChanges();

      expect(componentEl).toMatchSnapshot();
    }));
  });

  describe('selecting a version', () => {
    beforeEach(fakeAsync(() => {
      component.ngOnInit();
      tick();
      fixture.detectChanges();
    }));

    it('should add the version param to the url and load that url', () => {
      jest.spyOn(ng1IframeService, 'load');

      const versionItem = componentEl.querySelector<HTMLElement>(`.qa-version-${secondVersionUUID}`);
      versionItem?.click();

      expect(ng1IframeService.load).toHaveBeenCalledWith(`${path}?br_version_uuid=${secondVersionUUID}`);
    });

    it('should append the version param to the url if params are already present and load that url', () => {
      jest.spyOn(ng1IframeService, 'load');
      jest.spyOn(ng1ChannelService, 'makeRenderPath').mockReturnValueOnce(renderPath);

      const versionItem = componentEl.querySelector<HTMLElement>(`.qa-version-${secondVersionUUID}`);
      versionItem?.click();

      expect(ng1IframeService.load).toHaveBeenCalledWith(`${renderPath}&br_version_uuid=${secondVersionUUID}`);
    });

    it('should indicate the selected version', () => {
      jest.spyOn(ng1IframeService, 'load').mockImplementationOnce(() => {
        component.unpublishedVariantId = secondVersionUUID;
        return Promise.resolve();
      });

      const versionItem = componentEl.querySelector<HTMLElement>(`.qa-version-${secondVersionUUID}`);
      versionItem?.click();
      fixture.detectChanges();

      const versionList = componentEl.querySelector<HTMLElement>('.qa-version-list');
      expect(versionList).toMatchSnapshot();
    });

    it('should select the latest version when removing the component from the dom', fakeAsync(() => {
      jest.spyOn(component, 'selectVersion');

      component.ngOnInit();
      tick();
      component.unpublishedVariantId = secondVersionUUID;
      component.ngOnDestroy();

      expect(component.selectVersion).toHaveBeenCalledWith(firstVersionUUID);
    }));
  });
});
