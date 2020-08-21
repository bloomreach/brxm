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

import { IframeService } from '../channels/services/iframe.service';
import { AcceptanceState } from '../models/acceptance-state.enum';
import { DocumentState } from '../models/document-state.enum';
import { PageStates } from '../models/page-states.model';
import { XPageStatusInfo } from '../models/page-status-info.model';
import { ProjectState } from '../models/project-state.enum';
import { Project } from '../models/project.model';
import { ScheduledRequestType } from '../models/scheduled-request-type.enum';
import { WorkflowRequestType } from '../models/workflow-request-type.enum';
import { XPageState } from '../models/xpage-state.model';
import { XPageStatus } from '../models/xpage-status.enum';

import { Ng1PageService, NG1_PAGE_SERVICE } from './ng1/page.ng1.service';
import { ProjectService } from './project.service';

@Injectable({
  providedIn: 'root',
})
export class PageService {
  constructor(
    @Inject(NG1_PAGE_SERVICE) private readonly ng1PageService: Ng1PageService,
    private readonly projectService: ProjectService,
    private readonly iframeService: IframeService,
  ) { }

  getXPageState(): XPageState | undefined {
    return this.ng1PageService.states.xpage;
  }

  getPageStatusInfo(): XPageStatusInfo | undefined {
    const pageStates = this.ng1PageService.states;

    if (!pageStates) {
      return;
    }

    const pageStatus = this.getXPageStatus(pageStates, this.projectService.currentProject);
    const pageName = pageStates?.xpage?.name;

    if (!pageStatus || !pageName) {
      return;
    }

    return new XPageStatusInfo(
      pageStatus,
      pageStates.xpage?.state || DocumentState.Draft,
      pageName,
      pageStates?.scheduledRequest?.scheduledDate || pageStates?.workflowRequest?.requestDate,
      this.projectService.currentProject?.name,
    );
  }

  private getXPageStatus(pageStates: PageStates, project: Project | undefined): XPageStatus | undefined {
    const xPageState = pageStates.xpage;
    const xPageWorkflowRequest = pageStates.workflowRequest;
    const xPageScheduledRequest = pageStates.scheduledRequest;

    if (this.iframeService.isEditSharedContainers()) {
      return XPageStatus.EditingSharedContainers;
    }

    if (project && !this.projectService.isCore(project)) {
      return this.getXPageProjectRelatedStatus(project, xPageState?.acceptanceState);
    }

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

    switch (xPageState?.state) {
      case DocumentState.Live: return XPageStatus.Published;
      case DocumentState.Changed:
      case DocumentState.Unpublished: return XPageStatus.UnpublishedChanges;
      case DocumentState.New: return XPageStatus.Offline;
    }
  }

  private getXPageProjectRelatedStatus(project: Project, pageAcceptanceCriteria: AcceptanceState | undefined): XPageStatus {
    switch (project.state) {
      case ProjectState.InReview:
        switch (pageAcceptanceCriteria) {
          case AcceptanceState.InReview: return XPageStatus.ProjectInReview;
          case AcceptanceState.Approved: return XPageStatus.ProjectPageApproved;
          case AcceptanceState.Rejected: return XPageStatus.ProjectPageRejected;
        }

        return XPageStatus.ProjectInReview;

      case ProjectState.Unapproved: return XPageStatus.ProjectInProgress;
      case ProjectState.Approved: return XPageStatus.ProjectRunning;
    }

    return XPageStatus.ProjectRunning;
  }
}
