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

import { TestBed } from '@angular/core/testing';
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

import { Ng1PageService, NG1_PAGE_SERVICE } from './ng1/page.ng1.service';
import { PageService } from './page.service';
import { ProjectService } from './project.service';

describe('PageService', () => {
  let service: PageService;

  let ng1PageService: Ng1PageService;
  let projectService: ProjectService;
  let iframeService: IframeService;

  const xPageState = { branchId: 'testPageState' } as XPageState;

  beforeEach(() => {
    const ng1PageServiceMock = {
      states: {
        get xpage(): XPageState {
          return xPageState;
        },
      },
    };

    const projectServiceMock = {
      currentProject: undefined,
      isCore: jest.fn(x => x.id === 'master'),
    };

    const iframeServiceMock = {
      isEditSharedContainers: jest.fn(() => false),
    };

    TestBed.configureTestingModule({
      providers: [
        PageService,
        { provide: NG1_PAGE_SERVICE, useValue: ng1PageServiceMock },
        { provide: ProjectService, useValue: projectServiceMock },
        { provide: IframeService, useValue: iframeServiceMock },
      ],
    });

    service = TestBed.inject(PageService);
    ng1PageService = TestBed.inject(NG1_PAGE_SERVICE);
    projectService = TestBed.inject(ProjectService);
    iframeService = TestBed.inject(IframeService);
  });

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
        xpage: { state: DocumentState.Live, name: 'page name' },
      },
      undefined,
      new XPageStatusInfo(
        XPageStatus.Published,
        DocumentState.Live,
        'page name',
        undefined,
        undefined,
      ),
    ],
    [
      'UnpublishedChanges changed',
      {
        xpage: { state: DocumentState.Changed, name: 'page name' },
      },
      undefined,
      new XPageStatusInfo(
        XPageStatus.UnpublishedChanges,
        DocumentState.Changed,
        'page name',
        undefined,
        undefined,
      ),
    ],
    [
      'UnpublishedChanges unpublished',
      {
        xpage: { state: DocumentState.Unpublished, name: 'page name' },
      },
      undefined,
      new XPageStatusInfo(
        XPageStatus.UnpublishedChanges,
        DocumentState.Unpublished,
        'page name',
        undefined,
        undefined,
      ),
    ],
    [
      'Offline',
      {
        xpage: { name: 'page name', state: DocumentState.New },
      },
      undefined,
      new XPageStatusInfo(
        XPageStatus.Offline,
        DocumentState.New,
        'page name',
        undefined,
        undefined,
      ),
    ],
    [
      'PublicationRequest',
      {
        xpage: { name: 'page name', state: DocumentState.Unpublished },
        workflowRequest: { type: WorkflowRequestType.Publish, name: 'page name' },
      },
      undefined,
      new XPageStatusInfo(
        XPageStatus.PublicationRequest,
        DocumentState.Unpublished,
        'page name',
        undefined,
        undefined,
      ),
    ],
    [
      'TakeOfflineRequest',
      {
        xpage: { name: 'page name', state: DocumentState.Live },
        workflowRequest: { type: WorkflowRequestType.Depublish, name: 'page name' },
      },
      undefined,
      new XPageStatusInfo(
        XPageStatus.TakeOfflineRequest,
        DocumentState.Live,
        'page name',
        undefined,
        undefined,
      ),
    ],
    [
      'RejectedRequest',
      {
        xpage: { name: 'page name', state: DocumentState.Changed },
        workflowRequest: { type: WorkflowRequestType.Rejected },
      },
      undefined,
      new XPageStatusInfo(
        XPageStatus.RejectedRequest,
        DocumentState.Changed,
        'page name',
        undefined,
        undefined,
      ),
    ],
    [
      'ScheduledPublish',
      {
        xpage: { name: 'page name', state: DocumentState.Unpublished },
        workflowRequest: { type: WorkflowRequestType.ScheduledPublish, requestDate: 1596811323 },
      },
      undefined,
      new XPageStatusInfo(
        XPageStatus.ScheduledPublicationRequest,
        DocumentState.Unpublished,
        'page name',
        1596811323,
        undefined,
      ),
    ],
    [
      'ScheduledDepublish',
      {
        xpage: { name: 'page name', state: DocumentState.Live },
        workflowRequest: { type: WorkflowRequestType.ScheduledDepublish, requestDate: 1596811323 },
      },
      undefined,
      new XPageStatusInfo(
        XPageStatus.ScheduledToTakeOfflineRequest,
        DocumentState.Live,
        'page name',
        1596811323,
        undefined,
      ),
    ],
    [
      'ScheduledPublication',
      {
        xpage: { name: 'page name', state: DocumentState.Unpublished },
        scheduledRequest: { type: ScheduledRequestType.Publish, scheduledDate: 1596811323 },
      },
      undefined,
      new XPageStatusInfo(
        XPageStatus.ScheduledPublication,
        DocumentState.Unpublished,
        'page name',
        1596811323,
        undefined,
      ),
    ],
    [
      'ScheduledToTakeOffline',
      {
        xpage: { name: 'page name', state: DocumentState.Live },
        scheduledRequest: { type: ScheduledRequestType.Depublish, scheduledDate: 1596811323 },
      },
      undefined,
      new XPageStatusInfo(
        XPageStatus.ScheduledToTakeOffline,
        DocumentState.Live,
        'page name',
        1596811323,
        undefined,
      ),
    ],
    [
      'ProjectRunning',
      { xpage: { name: 'page name', state: DocumentState.Live } },
      { id: '123', name: 'some project name', state: ProjectState.Approved },
      new XPageStatusInfo(
        XPageStatus.ProjectRunning,
        DocumentState.Live,
        'page name',
        undefined,
        'some project name',
      ),
    ],
    [
      'ProjectRunning but is not set explicitly',
      { xpage: { name: 'page name', state: DocumentState.Live } },
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
      { xpage: { name: 'page name', state: DocumentState.Live } },
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
      'ProjectInReview',
      {
        xpage: { acceptanceState: AcceptanceState.InReview, name: 'page name' },
      },
      { id: '123', name: 'some project name', state: ProjectState.InReview },
      new XPageStatusInfo(
        XPageStatus.ProjectInReview,
        DocumentState.Draft,
        'page name',
        undefined,
        'some project name',
      ),
    ],
    [
      'ProjectInReview but is not set explicitly',
      {
        xpage: { name: 'page name' },
      },
      { id: '123', name: 'some project name', state: ProjectState.InReview },
      new XPageStatusInfo(
        XPageStatus.ProjectInReview,
        DocumentState.Draft,
        'page name',
        undefined,
        'some project name',
      ),
    ],
    [
      'ProjectPageApproved',
      {
        xpage: { acceptanceState: AcceptanceState.Approved, name: 'page name' },
      },
      { id: '123', name: 'some project name', state: ProjectState.InReview },
      new XPageStatusInfo(
        XPageStatus.ProjectPageApproved,
        DocumentState.Draft,
        'page name',
        undefined,
        'some project name',
      ),
    ],
    [
      'ProjectPageRejected',
      {
        xpage: { acceptanceState: AcceptanceState.Rejected, name: 'page name' },
      },
      { id: '123', name: 'some project name', state: ProjectState.InReview },
      new XPageStatusInfo(
        XPageStatus.ProjectPageRejected,
        DocumentState.Draft,
        'page name',
        undefined,
        'some project name',
      ),
    ],
    [
      'EditingSharedContainers',
      {
        xpage: { name: 'page name', state: DocumentState.Live },
      },
      undefined,
      new XPageStatusInfo(
        XPageStatus.EditingSharedContainers,
        DocumentState.Live,
        'page name',
        undefined,
        undefined,
      ),
    ],
  ])('getXPageStatus if page states represent "%s" state', (expectedStatusName, pageStates, project, expectedStatusInfo) => {
    test(`should return ${expectedStatusName} status info`, () => {
      ng1PageService.states = pageStates as PageStates;
      (projectService as any).currentProject = project;

      if (expectedStatusName === 'EditingSharedContainers') {
        mocked(iframeService.isEditSharedContainers).mockReturnValue(true);
      }

      const actual = service.getPageStatusInfo();

      expect(actual).toEqual(expectedStatusInfo);
    });
  });

  describe('getXPageState', () => {
    it('shoudl get xpage state', () => {
      expect(service.getXPageState()).toEqual(xPageState);
    });
  });
});
