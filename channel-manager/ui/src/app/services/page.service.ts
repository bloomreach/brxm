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
import { merge, Observable, Subject } from 'rxjs';
import { switchMap } from 'rxjs/operators';

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
import { ProjectService } from './project.service';

type StatusMatcher = (pageStates: PageStates) => Promise<XPageStatusInfo | undefined> | XPageStatusInfo | undefined;

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
  private readonly pageStatusInfoChangeTrigger = new Subject<void>();
  private readonly onEditSharedContainersUnsubscribe: () => void;

  constructor(
    @Inject(NG1_PAGE_SERVICE) private readonly ng1PageService: Ng1PageService,
    @Inject(NG1_ROOT_SCOPE) private readonly $rootScope: ng.IRootScopeService,
    private readonly projectService: ProjectService,
    private readonly iframeService: IframeService,
    private readonly versionsService: VersionsService,
  ) {
    this.onEditSharedContainersUnsubscribe = this.$rootScope.$on(
      'iframe:page:edit-shared-containers',
      () => this.pageStatusInfoChangeTrigger.next(),
    );

    this.projectService.afterChange(
      'PageService:project-change-listener',
      () => this.pageStatusInfoChangeTrigger.next(),
    );
  }

  get pageStatusInfo$(): Observable<XPageStatusInfo | undefined> {
    return merge(
      this.ng1PageService.states$,
      this.pageStatusInfoChangeTrigger,
    ).pipe(
      switchMap(async () => await this.getPageStatusInfo()),
    );
  }

  ngOnDestroy(): void {
    this.onEditSharedContainersUnsubscribe();
  }

  getXPageState(): XPageState | undefined {
    return this.ng1PageService.states?.xpage;
  }

  async getPageStatusInfo(): Promise<XPageStatusInfo | undefined> {
    if (!this.ng1PageService.states) {
      return;
    }

    for (const statusMatcher of this.statusMatchers) {
      const statusInfo = await statusMatcher(this.ng1PageService.states);

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
    const xPageWorkflowRequests = pageStates.workflow && pageStates.workflow.requests;
    const xPageWorkflowRequest = xPageWorkflowRequests && xPageWorkflowRequests.length && xPageWorkflowRequests[0];

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

    const getPageStatus = (projectState: ProjectState, pageAcceptanceState?: AcceptanceState) => {
      if (xPageState.branchId === this.projectService.coreBranchId) {
        return XPageStatus.NotPartOfProject;
      }

      switch (project.state) {
        case ProjectState.InReview:
          switch (pageAcceptanceState) {
            case AcceptanceState.InReview: return XPageStatus.ProjectInReview;
            case AcceptanceState.Approved: return XPageStatus.ProjectPageApproved;
            case AcceptanceState.Rejected: return XPageStatus.ProjectPageRejected;
          }

          return XPageStatus.ProjectInReview;

        case ProjectState.Unapproved: return XPageStatus.ProjectInProgress;
        case ProjectState.Approved: return XPageStatus.ProjectPageApproved;
        case ProjectState.Running: return XPageStatus.ProjectRunning;
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

  private async matchPreviousPageVersion(pageStates: PageStates): Promise<XPageStatusInfo | undefined> {
    const xPageState = pageStates.xpage;
    const project = this.projectService.currentProject;

    if (!xPageState) {
      return;
    }

    // skip the matcher if user is observing a project but the XPage document isn't a part of the project since
    // the unpublished variant id has an unrelated state
    if (project && !this.projectService.isCore(project) && xPageState.branchId === this.projectService.coreBranchId) {
      return;
    }

    const pageVersions = await this.versionsService.getVersions(xPageState.id);

    if (!pageVersions ||
      pageVersions.length === 0 ||
      this.versionsService.isCurrentVersion(pageVersions[0])) {
      return;
    }

    const currentVersion = pageVersions.find(v => this.versionsService.isCurrentVersion(v));

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
