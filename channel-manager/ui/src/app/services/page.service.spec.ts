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

import { async, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { mocked } from 'ts-jest/utils';

import { IframeService } from '../channels/services/iframe.service';
import { AcceptanceState } from '../models/acceptance-state.enum';
import { DocumentState } from '../models/document-state.enum';
import { PageStates } from '../models/page-states.model';
import { XPageStatusInfo } from '../models/page-status-info.model';
import { ProjectState } from '../models/project-state.enum';
import { ScheduledRequestType } from '../models/scheduled-request-type.enum';
import { WorkflowRequestType } from '../models/workflow-request-type.enum';
import { XPageState } from '../models/xpage-state.model';
import { XPageStatus } from '../models/xpage-status.enum';
import { VersionsService } from '../versions/services/versions.service';

import { Ng1PageService, NG1_PAGE_SERVICE } from './ng1/page.ng1.service';
import { NG1_ROOT_SCOPE } from './ng1/root-scope.service';
import { PageService } from './page.service';
import { ProjectService } from './project.service';

describe('PageService', () => {
  let service: PageService;

  let ng1PageService: Ng1PageService;
  let projectService: ProjectService;
  let iframeService: IframeService;
  let versionsService: VersionsService;

  const xPageState = { branchId: 'testPageState' } as XPageState;

  beforeEach(async(() => {
    const ng1PageServiceMock = {
      states: {
        get xpage(): XPageState {
          return xPageState;
        },
      },
      states$: of({ xpage: { id: 'xpage-id' } }),
    };

    const $rootScopeMock = {
      $on: jest.fn(),
    };

    const projectServiceMock = {
      currentProject: undefined,
      isCore: jest.fn(x => x.id === 'master'),
      afterChange: jest.fn(),
      coreBranchId: 'master',
    };

    const iframeServiceMock = {
      isEditSharedContainers: jest.fn(() => false),
    };

    const versionsServiceMock = {
      getVersions: jest.fn().mockResolvedValue([
        { jcrUUID: '1', timestamp: 123, userName: 'user1' },
        { jcrUUID: '2', timestamp: 1234, userName: 'user2' },
      ]),
      isCurrentVersion: jest.fn(v => v.jcrUUID === '1'),
    };

    TestBed.configureTestingModule({
      providers: [
        PageService,
        { provide: NG1_PAGE_SERVICE, useValue: ng1PageServiceMock },
        { provide: NG1_ROOT_SCOPE, useValue: $rootScopeMock },
        { provide: ProjectService, useValue: projectServiceMock },
        { provide: IframeService, useValue: iframeServiceMock },
        { provide: VersionsService, useValue: versionsServiceMock },
      ],
    });

    service = TestBed.inject(PageService);
    ng1PageService = TestBed.inject(NG1_PAGE_SERVICE);
    projectService = TestBed.inject(ProjectService);
    iframeService = TestBed.inject(IframeService);
    versionsService = TestBed.inject(VersionsService);
  }));

  describe.each([
    [
      'Page states are undefined',
      undefined,
      undefined,
      undefined,
    ],
    [
      'Page name is undefined',
      { xpage:  {} },
      undefined,
      undefined,
    ],
    [
      'Published',
      {
        xpage: { state: DocumentState.Live, name: 'page name', branchId: 'master' },
      },
      undefined,
      new XPageStatusInfo(
        XPageStatus.Published,
        DocumentState.Live,
        'page name',
      ),
    ],
    [
      'UnpublishedChanges changed',
      {
        xpage: { state: DocumentState.Changed, name: 'page name', branchId: 'master' },
      },
      undefined,
      new XPageStatusInfo(
        XPageStatus.UnpublishedChanges,
        DocumentState.Changed,
        'page name',
      ),
    ],
    [
      'UnpublishedChanges unpublished',
      {
        xpage: { state: DocumentState.Unpublished, name: 'page name', branchId: 'master' },
      },
      undefined,
      new XPageStatusInfo(
        XPageStatus.UnpublishedChanges,
        DocumentState.Unpublished,
        'page name',
      ),
    ],
    [
      'Offline',
      {
        xpage: { name: 'page name', state: DocumentState.New, branchId: 'master' },
      },
      undefined,
      new XPageStatusInfo(
        XPageStatus.Offline,
        DocumentState.New,
        'page name',
      ),
    ],
    [
      'PublicationRequest',
      {
        xpage: { name: 'page name', state: DocumentState.Unpublished, branchId: 'master' },
        workflow: {
          requests: [{ type: WorkflowRequestType.Publish, name: 'page name' }],
        },
      },
      undefined,
      new XPageStatusInfo(
        XPageStatus.PublicationRequest,
        DocumentState.Unpublished,
        'page name',
      ),
    ],
    [
      'TakeOfflineRequest',
      {
        xpage: { name: 'page name', state: DocumentState.Live, branchId: 'master' },
        workflow: {
          requests: [{ type: WorkflowRequestType.Depublish, name: 'page name' }],
        },
      },
      undefined,
      new XPageStatusInfo(
        XPageStatus.TakeOfflineRequest,
        DocumentState.Live,
        'page name',
      ),
    ],
    [
      'RejectedRequest',
      {
        xpage: { name: 'page name', state: DocumentState.Changed, branchId: 'master' },
        workflow: {
          requests: [{ type: WorkflowRequestType.Rejected }],
        },
      },
      undefined,
      new XPageStatusInfo(
        XPageStatus.RejectedRequest,
        DocumentState.Changed,
        'page name',
      ),
    ],
    [
      'ScheduledPublish',
      {
        xpage: { name: 'page name', state: DocumentState.Unpublished, branchId: 'master' },
        workflow: {
          requests: [{ type: WorkflowRequestType.ScheduledPublish, requestDate: 1596811323 }],
        },
      },
      undefined,
      new XPageStatusInfo(
        XPageStatus.ScheduledPublicationRequest,
        DocumentState.Unpublished,
        'page name',
        1596811323,
      ),
    ],
    [
      'ScheduledDepublish',
      {
        xpage: { name: 'page name', state: DocumentState.Live, branchId: 'master' },
        workflow: {
          requests: [{ type: WorkflowRequestType.ScheduledDepublish, requestDate: 1596811323 }],
        },
      },
      undefined,
      new XPageStatusInfo(
        XPageStatus.ScheduledToTakeOfflineRequest,
        DocumentState.Live,
        'page name',
        1596811323,
      ),
    ],
    [
      'ScheduledPublication',
      {
        xpage: { name: 'page name', state: DocumentState.Unpublished, branchId: 'master' },
        scheduledRequest: { type: ScheduledRequestType.Publish, scheduledDate: 1596811323 },
      },
      undefined,
      new XPageStatusInfo(
        XPageStatus.ScheduledPublication,
        DocumentState.Unpublished,
        'page name',
        1596811323,
      ),
    ],
    [
      'ScheduledToTakeOffline',
      {
        xpage: { name: 'page name', state: DocumentState.Live, branchId: 'master' },
        scheduledRequest: { type: ScheduledRequestType.Depublish, scheduledDate: 1596811323 },
      },
      undefined,
      new XPageStatusInfo(
        XPageStatus.ScheduledToTakeOffline,
        DocumentState.Live,
        'page name',
        1596811323,
      ),
    ],
    [
      'ProjectRunning',
      { xpage: { name: 'page name', state: DocumentState.Live, branchId: 'ABC123' } },
      { id: '123', name: 'some project name', state: ProjectState.Running },
      new XPageStatusInfo(
        XPageStatus.ProjectRunning,
        DocumentState.Live,
        'page name',
        undefined,
        'some project name',
      ),
    ],
    [
      'ProjectRunning but XPage is not a part of the project',
      { xpage: { name: 'page name', state: DocumentState.Live, branchId: 'master' } },
      { id: '123', name: 'some project name', state: ProjectState.Running },
      new XPageStatusInfo(
        XPageStatus.Published,
        DocumentState.Live,
        'page name',
      ),
    ],
    [
      'ProjectRunning but it is not set explicitly',
      { xpage: { name: 'page name', state: DocumentState.Live, branchId: 'ABC123'  } },
      { id: '123', name: 'some project name' },
      new XPageStatusInfo(
        XPageStatus.ProjectRunning,
        DocumentState.Live,
        'page name',
        undefined,
        'some project name',
      ),
    ],
    [
      'ProjectInProgress',
      { xpage: { name: 'page name', state: DocumentState.Live, branchId: 'ABC123'  } },
      { id: '123', name: 'some project name', state: ProjectState.Unapproved },
      new XPageStatusInfo(
        XPageStatus.ProjectInProgress,
        DocumentState.Live,
        'page name',
        undefined,
        'some project name',
      ),
    ],
    [
      'ProjectInProgress but XPage is not a part of the project',
      { xpage: { name: 'page name', state: DocumentState.Live, branchId: 'master'  } },
      { id: '123', name: 'some project name', state: ProjectState.Unapproved },
      new XPageStatusInfo(
        XPageStatus.Published,
        DocumentState.Live,
        'page name',
      ),
    ],
    [
      'ProjectInReview',
      {
        xpage: { acceptanceState: AcceptanceState.InReview, name: 'page name', state: DocumentState.Changed, branchId: 'ABC123'  },
      },
      { id: '123', name: 'some project name', state: ProjectState.InReview },
      new XPageStatusInfo(
        XPageStatus.ProjectInReview,
        DocumentState.Changed,
        'page name',
        undefined,
        'some project name',
      ),
    ],
    [
      'ProjectInReview but XPage is not a part of the project',
      {
        xpage: { acceptanceState: AcceptanceState.InReview, name: 'page name', state: DocumentState.Changed, branchId: 'master'  },
      },
      { id: '123', name: 'some project name', state: ProjectState.InReview },
      new XPageStatusInfo(
        XPageStatus.UnpublishedChanges,
        DocumentState.Changed,
        'page name',
      ),
    ],
    [
      'ProjectPageApproved',
      {
        xpage: { acceptanceState: AcceptanceState.Approved, name: 'page name', state: DocumentState.Changed, branchId: 'ABC123'  },
      },
      { id: '123', name: 'some project name', state: ProjectState.InReview },
      new XPageStatusInfo(
        XPageStatus.ProjectPageApproved,
        DocumentState.Changed,
        'page name',
        undefined,
        'some project name',
      ),
    ],
    [
      'ProjectPageApproved but XPage is not a part of the project',
      {
        xpage: { acceptanceState: AcceptanceState.Approved, name: 'page name', state: DocumentState.Changed, branchId: 'master'  },
      },
      { id: '123', name: 'some project name', state: ProjectState.InReview },
      new XPageStatusInfo(
        XPageStatus.UnpublishedChanges,
        DocumentState.Changed,
        'page name',
      ),
    ],
    [
      'ProjectPageRejected',
      {
        xpage: { acceptanceState: AcceptanceState.Rejected, name: 'page name', state: DocumentState.Changed, branchId: 'ABC123'  },
      },
      { id: '123', name: 'some project name', state: ProjectState.InReview },
      new XPageStatusInfo(
        XPageStatus.ProjectPageRejected,
        DocumentState.Changed,
        'page name',
        undefined,
        'some project name',
      ),
    ],
    [
      'ProjectPageRejected but XPage is not a part of the project',
      {
        xpage: { acceptanceState: AcceptanceState.Rejected, name: 'page name', state: DocumentState.Changed, branchId: 'master'  },
      },
      { id: '123', name: 'some project name', state: ProjectState.InReview },
      new XPageStatusInfo(
        XPageStatus.UnpublishedChanges,
        DocumentState.Changed,
        'page name',
      ),
    ],
    [
      'EditingSharedContainers',
      {
        xpage: { name: 'page name', state: DocumentState.Live, branchId: 'master'  },
      },
      undefined,
      new XPageStatusInfo(
        XPageStatus.EditingSharedContainers,
        DocumentState.Live,
        'page name',
      ),
    ],
    [
      'PreviousVersion',
      {
        xpage: { name: 'page name', state: DocumentState.Live, branchId: 'master' },
      },
      undefined,
      new XPageStatusInfo(
        XPageStatus.PreviousVersion,
        DocumentState.Live,
        'page name',
        undefined,
        undefined,
        {
          jcrUUID: '2',
          timestamp: 1234,
          userName: 'user2',
       },
      ),
    ],
    [
      'PreviousVersion if a project is active but XPage is not a part of the project',
      {
        xpage: { name: 'page name', state: DocumentState.Live, branchId: 'master' },
      },
      { id: '123', name: 'some project name', state: ProjectState.InReview },
      new XPageStatusInfo(
        XPageStatus.Published,
        DocumentState.Live,
        'page name',
      ),
    ],
    [
      'PreviousVersion if a project is active but XPage is a part of the project',
      {
        xpage: { name: 'page name', state: DocumentState.Live, branchId: 'ABC123' },
      },
      { id: '123', name: 'some project name', state: ProjectState.InReview },
      new XPageStatusInfo(
        XPageStatus.PreviousVersion,
        DocumentState.Live,
        'page name',
        undefined,
        undefined,
        {
          jcrUUID: '2',
          timestamp: 1234,
          userName: 'user2',
        },
      ),
    ],
    [
      'Locked',
      {
        xpage: { name: 'page name', state: DocumentState.Live, lockedBy: 'username', branchId: 'master' },
      },
      undefined,
      new XPageStatusInfo(
        XPageStatus.Locked,
        DocumentState.Live,
        'page name',
        undefined,
        undefined,
        undefined,
        'username',
      ),
    ],
  ])('getXPageStatus if page states represent "%s" state', (expectedStatusName, pageStates, project, expectedStatusInfo) => {
    test(`should return ${expectedStatusName} status info`, async () => {
      ng1PageService.states = pageStates as PageStates;
      (projectService as any).currentProject = project;

      if (expectedStatusName === 'EditingSharedContainers') {
        mocked(iframeService.isEditSharedContainers).mockReturnValue(true);
      }

      if (expectedStatusName.startsWith('PreviousVersion')) {
        mocked(versionsService.isCurrentVersion).mockImplementation(v => v.jcrUUID === '2');
      }

      const actual = await service.getPageStatusInfo();

      expect(actual).toEqual(expectedStatusInfo);
    });
  });

  describe('getXPageState', () => {
    it('should get xpage state', () => {
      expect(service.getXPageState()).toEqual(xPageState);
    });
  });
});
