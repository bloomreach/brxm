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

import { DocumentState } from '../../models/document-state.enum';
import { ScheduledRequestType } from '../../models/scheduled-request-type.enum';
import { WorkflowRequestType } from '../../models/workflow-request-type.enum';
import { XPageState } from '../../models/xpage-state.model';
import { XPageStatus } from '../../models/xpage-status.enum';

import { NG1_PAGE_SERVICE } from './ng1/page.ng1.service';
import { PageService } from './page.service';

describe('PageService', () => {
  let service: PageService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        PageService,
        { provide: NG1_PAGE_SERVICE, useValue: {} },
      ],
    });

    service = TestBed.inject(PageService);
  });

  describe.each([
    [DocumentState.Live, 'Published', XPageStatus.Published],
    [DocumentState.Unpublished, 'UnpublishedChanges', XPageStatus.UnpublishedChanges],
    [DocumentState.New, 'Offline', XPageStatus.Offline],
  ])('getXPageStatus if xpage state is %s', (state, expectedStatusName, expectedStatusValue) => {
    test(`should return  ${expectedStatusName} label`, () => {
      const xpageState = { state } as XPageState;

      expect(service.getXPageStatus(xpageState)).toBe(expectedStatusValue);
    });
  });

  describe.each([
    [WorkflowRequestType.Publish, 'PublicationRequest', XPageStatus.PublicationRequest],
    [WorkflowRequestType.Depublish, 'TakeOfflineRequest', XPageStatus.TakeOfflineRequest],
    [WorkflowRequestType.Rejected, 'RejectedRequest', XPageStatus.RejectedRequest],
    [WorkflowRequestType.ScheduledPublish, 'ScheduledPublicationRequest', XPageStatus.ScheduledPublicationRequest],
    [WorkflowRequestType.ScheduledDepublish, 'ScheduledToTakeOfflineRequest', XPageStatus.ScheduledToTakeOfflineRequest],
  ])('getXPageStatus if xpage has a workflow request of type %s', (worflowRequestType, expectedStatusName, expectedStatusValue) => {
    test(`should return  ${expectedStatusName} label`, () => {
      const xpageState = {
        workflowRequest: {
          type: worflowRequestType,
        },
      } as XPageState;

      expect(service.getXPageStatus(xpageState)).toBe(expectedStatusValue);
    });
  });

  describe.each([
    [ScheduledRequestType.Publish, 'ScheduledPublication', XPageStatus.ScheduledPublication],
    [ScheduledRequestType.Depublish, 'ScheduledToTakeOffline', XPageStatus.ScheduledToTakeOffline],
  ])('getXPageStatus if xpage has a schedule request of type %s', (scheduleRequestType, expectedStatusName, expectedStatusValue) => {
    test(`should return  ${expectedStatusName} label`, () => {
      const xpageState = {
        scheduledRequest: {
          type: scheduleRequestType,
        },
      } as XPageState;

      expect(service.getXPageStatus(xpageState)).toBe(expectedStatusValue);
    });
  });
});
