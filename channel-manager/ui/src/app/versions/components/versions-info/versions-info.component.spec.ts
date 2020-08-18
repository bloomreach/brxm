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
  let workflowService: WorkflowService;

  const date = Date.parse('11/08/2020 16:03');
  const path = '/some/test/path';
  const renderPath = `${path}?withParam=test`;
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
  } as VersionsInfo;

  beforeEach(() => {
    const contentServiceMock = {
      getDocumentVersionsInfo: jest.fn(() => Promise.resolve(mockVersionsInfo)),
    };

    const channelServiceMock = {
      makeRenderPath: () => path,
    };

    const iframeServiceMock = {
      getCurrentRenderPathInfo: () => path,
      load: jest.fn(() => Promise.resolve()),
    };

    const workflowServiceMock = {
      createWorkflowAction: jest.fn(() => { }),
    };

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
    workflowService = TestBed.inject(WorkflowService);
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

  describe('showing versions', () => {
    it('should show header indicating no versions are available', () => {
      expect(componentEl).toMatchSnapshot();
    });

    it('should show list of versions', fakeAsync(() => {
      component.ngOnInit();
      tick();
      fixture.detectChanges();

      expect(componentEl).toMatchSnapshot();
    }));
  });

  describe('selecting version', () => {
    beforeEach(fakeAsync(() => {
      component.ngOnInit();
      tick();
      fixture.detectChanges();
    }));

    it('should add the version param to the url and load that url', () => {
      jest.spyOn(iframeService, 'load');

      const versionItem = componentEl.querySelector<HTMLElement>(`.qa-version-${secondVersionUUID}`);
      versionItem?.click();

      expect(iframeService.load).toHaveBeenCalledWith(`${path}?br_version_uuid=${secondVersionUUID}`);
    });

    it('should append the version param to the url if params are already present and load that url', () => {
      jest.spyOn(iframeService, 'load');
      jest.spyOn(channelService, 'makeRenderPath').mockReturnValueOnce(renderPath);

      const versionItem = componentEl.querySelector<HTMLElement>(`.qa-version-${secondVersionUUID}`);
      versionItem?.click();

      expect(iframeService.load).toHaveBeenCalledWith(`${renderPath}&br_version_uuid=${secondVersionUUID}`);
    });

    it('should show the selected version', () => {
      jest.spyOn(iframeService, 'load').mockImplementationOnce(() => {
        component.unpublishedVariantId = secondVersionUUID;
        return Promise.resolve();
      });

      const versionItem = componentEl.querySelector<HTMLElement>(`.qa-version-${secondVersionUUID}`);
      versionItem?.click();
      fixture.detectChanges();

      const versionList = componentEl.querySelector<HTMLElement>('.qa-version-list');
      expect(versionList).toMatchSnapshot();
    });
  });

  describe('restoring a version', () => {
    beforeEach(fakeAsync(() => {
      component.ngOnInit();
      tick();
      fixture.detectChanges();
    }));

    it('should show restore button for other versions when selected', async () => {
      jest.spyOn(iframeService, 'load').mockImplementationOnce(() => {
        component.unpublishedVariantId = secondVersionUUID;
        return Promise.resolve();
      });

      await component.selectVersion(secondVersionUUID);
      fixture.detectChanges();

      expect(componentEl).toMatchSnapshot();
    });

    it('should not show restore button for first version when selected', async () => {
      jest.spyOn(iframeService, 'load').mockImplementationOnce(() => {
        component.unpublishedVariantId =  firstVersionUUID;
        return Promise.resolve();
      });

      await component.selectVersion(firstVersionUUID);
      fixture.detectChanges();

      expect(componentEl).toMatchSnapshot();
    });

    it('should call to restore', async () => {
      jest.spyOn(iframeService, 'load').mockImplementationOnce(() => {
        component.unpublishedVariantId = secondVersionUUID;
        return Promise.resolve();
      });

      await component.selectVersion(secondVersionUUID);
      fixture.detectChanges();

      const restoreButton = componentEl.querySelector<HTMLButtonElement>('.qa-restore-version-action');
      restoreButton?.click();

      expect(workflowService.createWorkflowAction).toHaveBeenCalledWith(component.documentId, 'restore', secondVersionUUID);
    });
  });

  describe('create version', () => {
    it('should show version button for first version when selected', async () => {
      jest.spyOn(iframeService, 'load').mockImplementationOnce(() => {
        component.unpublishedVariantId = firstVersionUUID;
        return Promise.resolve();
      });

      await component.selectVersion(firstVersionUUID);
      fixture.detectChanges();

      expect(componentEl).toMatchSnapshot();
    });

    it('should not show version button for other versions when selected', async () => {
      jest.spyOn(iframeService, 'load').mockImplementationOnce(() => {
        component.unpublishedVariantId = secondVersionUUID;
        return Promise.resolve();
      });

      await component.selectVersion(secondVersionUUID);
      fixture.detectChanges();

      expect(componentEl).toMatchSnapshot();
    });

    it('should call to create version', fakeAsync(() => {
      component.ngOnInit();
      tick();
      fixture.detectChanges();

      const newVersionButton = componentEl.querySelector<HTMLButtonElement>(`.qa-new-version-action`);
      newVersionButton?.click();
      fixture.detectChanges();

      expect(workflowService.createWorkflowAction).toHaveBeenCalledWith(component.documentId, 'version');
    }));
  });
});
