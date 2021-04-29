/*!
 * Copyright 2020-2021 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
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

import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatIconModule } from '@angular/material/icon';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { MatListModule } from '@angular/material/list';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { TranslateModule } from '@ngx-translate/core';
import { of } from 'rxjs';

import { Ng1ChannelService, NG1_CHANNEL_SERVICE } from '../../../services/ng1/channel.ng1.service';
import { Ng1IframeService, NG1_IFRAME_SERVICE } from '../../../services/ng1/iframe.ng1.service';
import { NG1_UI_ROUTER_GLOBALS } from '../../../services/ng1/ui-router-globals.ng1.service';
import { NotificationService } from '../../../services/notification.service';
import { ProjectService } from '../../../services/project.service';
import { VersionsService } from '../../../versions/services/versions.service';
import { VersionsInfo } from '../../models/versions-info.model';

import { VersionsInfoComponent } from './versions-info.component';

describe('VersionsInfoComponent', () => {
  let component: VersionsInfoComponent;
  let componentEl: HTMLElement;
  let fixture: ComponentFixture<VersionsInfoComponent>;
  let ng1IframeService: Ng1IframeService;
  let ng1ChannelService: Ng1ChannelService;
  let versionsService: VersionsService;
  let notificationService: NotificationService;
  let projectService: ProjectService;

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

  let isBranchMock = false;

  beforeEach(() => {
    const channelServiceMock: Partial<Ng1ChannelService> = {
      makeRenderPath: () => path,
      getHomePageRenderPathInfo: () => homePageRenderPath,
    };

    const iframeServiceMock: Partial<Ng1IframeService> = {
      getCurrentRenderPathInfo: () => path,
      load: jest.fn(() => Promise.resolve()),
    };

    const versionsServiceMock: Partial<VersionsService> = {
      versionsInfo$: of(mockVersionsInfo),
      getVersionsInfo: jest.fn(),
      isVersionFromPage: jest.fn((id: string) => id === firstVersionUUID),
    };

    const uiRouterGlobalsMock = {
      params: {
        documentId: 'testDocumentId',
      },
    };

    const notificationServiceMock: Partial<NotificationService> = {
      showErrorNotification: jest.fn(),
    };

    const projectServiceMock: Partial<ProjectService> = {
      isBranch: jest.fn(() => isBranchMock),
    };

    TestBed.configureTestingModule({
      declarations: [
        VersionsInfoComponent,
      ],
      imports: [
        MatListModule,
        MatIconModule,
        MatIconTestingModule,
        MatProgressBarModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        { provide: NG1_IFRAME_SERVICE, useValue: iframeServiceMock },
        { provide: NG1_CHANNEL_SERVICE, useValue: channelServiceMock },
        { provide: NG1_UI_ROUTER_GLOBALS, useValue: uiRouterGlobalsMock },
        { provide: VersionsService, useValue: versionsServiceMock },
        { provide: NotificationService, useValue: notificationServiceMock },
        { provide: ProjectService, useValue: projectServiceMock },
      ],
      schemas: [
        CUSTOM_ELEMENTS_SCHEMA,
      ],
    });

    ng1IframeService = TestBed.inject(NG1_IFRAME_SERVICE);
    ng1ChannelService = TestBed.inject(NG1_CHANNEL_SERVICE);
    versionsService = TestBed.inject(VersionsService);
    notificationService = TestBed.inject(NotificationService);
    projectService = TestBed.inject(ProjectService);
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(VersionsInfoComponent);
    component = fixture.componentInstance;
    componentEl = fixture.nativeElement;
    component.ngOnInit();
    fixture.detectChanges();
  });

  describe('initial rendering', () => {
    it('should show list of versions', () => {
      expect(componentEl).toMatchSnapshot();
    });
  });

  describe('selecting a version', () => {
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

    it('should select the latest version when removing the component from the dom', () => {
      jest.spyOn(component, 'selectVersion');

      component.ngOnInit();
      component.ngOnDestroy();

      expect(component.selectVersion).toHaveBeenCalledWith(firstVersionUUID);
    });

    it('should display error if load is rejected', () => {
      jest.spyOn(ng1IframeService, 'load').mockImplementationOnce(() => Promise.reject());

      const versionItem = componentEl.querySelector<HTMLElement>(`.qa-version-${secondVersionUUID}`);
      versionItem?.click();

      expect(notificationService.showErrorNotification).toHaveBeenCalledWith('VERSION_SELECTION_ERROR');
      expect(component.actionInProgress).toBe(false);
    });
  });

  describe('showing filter', () => {
    it('should show filter for Core of document', () => {
      isBranchMock = false;

      component.ngOnInit();
      fixture.detectChanges();

      expect(componentEl).toMatchSnapshot();
    });
    it('should not show filter for project branch of document', () => {
      isBranchMock = true;

      component.ngOnInit();
      fixture.detectChanges();

      expect(componentEl).toMatchSnapshot();
    });
  });
});
