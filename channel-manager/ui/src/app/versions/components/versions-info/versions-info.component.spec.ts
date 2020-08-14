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

import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { MatListModule, MatSelectionListChange } from '@angular/material/list';
import { TranslateModule } from '@ngx-translate/core';

import { ChannelService } from '../../../channels/services/channel.service';
import { IframeService } from '../../../channels/services/iframe.service';
import { ContentService } from '../../../content/services/content.service';
import { WorkflowService } from '../../../content/services/workflow.service';
import { VersionsInfo } from '../../models/versions-info.model';

import { VersionsInfoComponent } from './versions-info.component';

describe('VersionsInfoComponent', () => {
  let component: VersionsInfoComponent;
  let componentEl: HTMLElement;
  let fixture: ComponentFixture<VersionsInfoComponent>;
  let iframeService: IframeService;
  let channelService: ChannelService;

  const date = Date.parse('11/08/2020 16:03');
  const path = '/some/test/path';
  const renderPath = `${path}?withParam=test`;
  const selectedVersionUUID = 'testVariantId';
  const selectEvent = {
    option: {
      value: selectedVersionUUID,
    },
  } as MatSelectionListChange;

  beforeEach(() => {
    const contentServiceMock = {
      getDocumentVersionsInfo: jest.fn(() => Promise.resolve({
        versions: [
          {
            jcrUUID: 'testId',
            userName: 'testUserName',
            timestamp: date,
          },
        ],
      } as VersionsInfo)),
    };

    const channelServiceMock = {
      makeRenderPath: () => path,
    };

    const iframeServiceMock = {
      getCurrentRenderPathInfo: () => path,
      load: jest.fn(() => Promise.resolve()),
    };

    const workflowServiceMock = {};

    TestBed.configureTestingModule({
      declarations: [VersionsInfoComponent],
      imports: [
        MatListModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        { provide: ContentService, useValue: contentServiceMock },
        { provide: ChannelService, useValue: channelServiceMock },
        { provide: IframeService, useValue: iframeServiceMock },
        { provide: WorkflowService, useValue: workflowServiceMock },
      ],
    });

    iframeService = TestBed.inject(IframeService);
    channelService = TestBed.inject(ChannelService);
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(VersionsInfoComponent);
    component = fixture.componentInstance;
    componentEl = fixture.nativeElement;

    component.documentId = 'testDocumentId';
    component.branchId = 'projectId';
    component.unpublishedVariantId = 'unpublishedVariantId';
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(componentEl).toMatchSnapshot();
  });

  describe('showing versions', () => {
    it('should show list of versions', fakeAsync(() => {
      component.ngOnInit();

      tick();

      fixture.detectChanges();

      expect(componentEl).toMatchSnapshot();
    }));
  });

  describe('selecting version', () => {
    it('should add the version param to the url and load that url', async () => {
      jest.spyOn(iframeService, 'load');
      await component.selectVersion(selectEvent);

      expect(iframeService.load).toHaveBeenCalledWith(`${path}?br_version_uuid=${selectedVersionUUID}`);
    });

    it('should append the version param to the url if params are already present and load that url', async () => {
      jest.spyOn(iframeService, 'load');
      jest.spyOn(channelService, 'makeRenderPath').mockReturnValueOnce(renderPath);
      await component.selectVersion(selectEvent);

      expect(iframeService.load).toHaveBeenCalledWith(`${renderPath}&br_version_uuid=${selectedVersionUUID}`);
    });
  });
});
