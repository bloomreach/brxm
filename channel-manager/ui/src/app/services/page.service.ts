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

import { Inject, Injectable } from '@angular/core';

import { DocumentState } from '../models/document-state.enum';
import { ScheduledRequestType } from '../models/scheduled-request-type.enum';
import { WorkflowRequestType } from '../models/workflow-request-type.enum';
import { XPageState } from '../models/xpage-state.model';
import { XPageStatus } from '../models/xpage-status.enum';

import { Ng1PageService, NG1_PAGE_SERVICE } from './ng1/page.ng1.service';

@Injectable({
  providedIn: 'root',
})
export class PageService {
  constructor(@Inject(NG1_PAGE_SERVICE) private readonly ng1PageService: Ng1PageService) { }

  getXPageStatus(state: XPageState): XPageStatus | undefined {
    if (!this.ng1PageService.states?.xpage) {
      return undefined;
    }

    const xPageState = this.ng1PageService.states.xpage.state;
    const xPageWorkflowRequest = this.ng1PageService.states.xpage.workflowRequest;
    const xPageScheduledRequest = this.ng1PageService.states.xpage.scheduledRequest;

    if (xPageWorkflowRequest) {
      switch (xPageWorkflowRequest.type) {
        case WorkflowRequestType.Publish: return XPageStatus.PublicationRequest;
        case WorkflowRequestType.Depublish: return XPageStatus.TakeOfflineRequest;
        case WorkflowRequestType.Rejected: return XPageStatus.RejectedRequest;
        case WorkflowRequestType.ScheduledPublish: return XPageStatus.ScheduledPublicationRequest;
        case WorkflowRequestType.ScheduledDepublish: return XPageStatus.ScheduledToTakeOfflineRequest;
      }
    }

    if (xPageScheduledRequest) {
      switch (xPageScheduledRequest.type) {
        case ScheduledRequestType.Publish: return XPageStatus.ScheduledPublication;
        case ScheduledRequestType.Depublish: return XPageStatus.ScheduledToTakeOffline;
      }
    }

    switch (xPageState) {
      case DocumentState.Live: return XPageStatus.Published;
      case DocumentState.Unpublished: return XPageStatus.UnpublishedChanges;
      case DocumentState.New: return XPageStatus.Offline;
    }
  }
}
