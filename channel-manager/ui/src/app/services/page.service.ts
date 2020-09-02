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

import { Inject, Injectable, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

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
import { Version } from '../versions/models/version.model';
import { VersionsService } from '../versions/services/versions.service';

import { Ng1PageService, NG1_PAGE_SERVICE } from './ng1/page.ng1.service';
import { ProjectService } from './project.service';

type StatusMatcher = (pageStates: PageStates) => XPageStatusInfo | undefined;

@Injectable({
  providedIn: 'root',
})
export class PageService implements OnDestroy {
  // Matches the state or returns undefined to let the next match a try
  private readonly statusMatchers: StatusMatcher[] = [
    (pageStates: PageStates) => this.matchLockedState(pageStates),
    (pageStates: PageStates) => this.matchPreviousPageVersion(pageStates),
    (pageStates: PageStates) => this.matchEditingSharedContainers(pageStates),
    (pageStates: PageStates) => this.matchProject(pageStates),
    (pageStates: PageStates) => this.matchWorkflowRequest(pageStates),
    (pageStates: PageStates) => this.matchScheduledRequest(pageStates),
    (pageStates: PageStates) => this.matchXPageState(pageStates),
  ];
  private readonly unsubscribe = new Subject();

  private pageVersions: Version[] | undefined;

  constructor(
    @Inject(NG1_PAGE_SERVICE) private readonly ng1PageService: Ng1PageService,
    private readonly projectService: ProjectService,
    private readonly iframeService: IframeService,
    private readonly versionsService: VersionsService,
  ) {
    ng1PageService.states$.pipe(
      takeUntil(this.unsubscribe),
    ).subscribe(async pageStates => {
      if (pageStates?.xpage?.id) {
        this.pageVersions = await this.versionsService.getVersions(pageStates?.xpage?.id);
      } else {
        this.pageVersions = undefined;
      }
    });
  }

  ngOnDestroy(): void {
    this.unsubscribe.next();
    this.unsubscribe.complete();
  }

  getXPageState(): XPageState | undefined {
    return this.ng1PageService.states.xpage;
  }

  getPageStatusInfo(): XPageStatusInfo | undefined {
    if (!this.ng1PageService.states) {
      return;
    }

    for (const statusMatcher of this.statusMatchers) {
      const statusInfo = statusMatcher(this.ng1PageService.states);

      if (statusInfo) {
        return statusInfo;
      }
    }
  }

  private matchXPageState(pageStates: PageStates): XPageStatusInfo | undefined {
    const xPageState = pageStates.xpage;

    if (!xPageState) {
      return;
    }

    const getPageStatus = (state: DocumentState) => {
      switch (state) {
        case DocumentState.Live: return XPageStatus.Published;
        case DocumentState.Changed:
        case DocumentState.Unpublished: return XPageStatus.UnpublishedChanges;
        case DocumentState.New: return XPageStatus.Offline;
      }
    };

    const pageStatus = getPageStatus(xPageState.state);

    if (!pageStatus) {
      return;
    }

    return new XPageStatusInfo(
      pageStatus,
      xPageState.state,
      xPageState.name,
    );
  }

  private matchScheduledRequest(pageStates: PageStates): XPageStatusInfo | undefined {
    const xPageState = pageStates.xpage;
    const xPageScheduledRequest = pageStates.scheduledRequest;

    if (!xPageState || !xPageScheduledRequest) {
      return;
    }

    const getPageStatus = (type: ScheduledRequestType) => {
      switch (type) {
        case ScheduledRequestType.Publish: return XPageStatus.ScheduledPublication;
        case ScheduledRequestType.Depublish: return XPageStatus.ScheduledToTakeOffline;
      }
    };

    const pageStatus = getPageStatus(xPageScheduledRequest.type);

    if (!pageStatus) {
      return;
    }

    return new XPageStatusInfo(
      pageStatus,
      xPageState.state,
      xPageState.name,
      xPageScheduledRequest.scheduledDate,
    );
  }

  private matchWorkflowRequest(pageStates: PageStates): XPageStatusInfo | undefined {
    const xPageState = pageStates.xpage;
    const xPageWorkflowRequest = pageStates.workflowRequest;

    if (!xPageState || !xPageWorkflowRequest) {
      return;
    }

    const getPageStatus = (type: WorkflowRequestType) => {
      switch (type) {
        case WorkflowRequestType.Publish: return XPageStatus.PublicationRequest;
        case WorkflowRequestType.Depublish: return XPageStatus.TakeOfflineRequest;
        case WorkflowRequestType.Rejected: return XPageStatus.RejectedRequest;
        case WorkflowRequestType.ScheduledPublish: return XPageStatus.ScheduledPublicationRequest;
        case WorkflowRequestType.ScheduledDepublish: return XPageStatus.ScheduledToTakeOfflineRequest;
      }
    };

    const pageStatus = getPageStatus(xPageWorkflowRequest.type);

    if (!pageStatus) {
      return;
    }

    return new XPageStatusInfo(
      pageStatus,
      xPageState.state,
      xPageState.name,
      xPageWorkflowRequest.requestDate,
    );
  }

  private matchProject(pageStates: PageStates): XPageStatusInfo | undefined {
    const xPageState = pageStates.xpage;
    const project = this.projectService.currentProject;

    if (!xPageState || !project || this.projectService.isCore(project)) {
      return;
    }

    const getPageStatus = (projectState: ProjectState, pageAcceptanceCriteria?: AcceptanceState) => {
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
    };

    const pageStatus = getPageStatus(project.state, xPageState.acceptanceState);

    if (!pageStatus) {
      return;
    }

    return new XPageStatusInfo(
      pageStatus,
      xPageState.state,
      xPageState.name,
      undefined,
      this.projectService.currentProject?.name,
    );
  }

  private matchEditingSharedContainers(pageStates: PageStates): XPageStatusInfo | undefined {
    const xPageState = pageStates.xpage;

    if (!this.iframeService.isEditSharedContainers() || !xPageState) {
      return;
    }

    return  new XPageStatusInfo(
      XPageStatus.EditingSharedContainers,
      xPageState.state,
      xPageState.name,
    );
  }

  private matchPreviousPageVersion(pageStates: PageStates): XPageStatusInfo | undefined {
    const xPageState = pageStates.xpage;

    if (!xPageState ||
      !this.pageVersions ||
      this.pageVersions.length === 0 ||
      this.versionsService.isCurrentVersion(this.pageVersions[0])) {
      return;
    }

    const currentVersion = this.pageVersions.find(v => this.versionsService.isCurrentVersion(v));

    return new XPageStatusInfo(
      XPageStatus.PreviousVersion,
      xPageState.state,
      xPageState.name,
      undefined,
      undefined,
      currentVersion,
    );
  }

  private matchLockedState(pageStates: PageStates): XPageStatusInfo | undefined {
    const xPageState = pageStates.xpage;

    if (!xPageState || !xPageState.lockedBy) {
      return;
    }

    return new XPageStatusInfo(
      XPageStatus.Locked,
      xPageState.state,
      xPageState.name,
      undefined,
      undefined,
      undefined,
      xPageState.lockedBy,
    );
  }
}
