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

import { DatePipe } from '@angular/common';
import { Component, Input } from '@angular/core';

import { DocumentState } from '../../../models/document-state.enum';
import { XPageStatusInfo } from '../../../models/page-status-info.model';
import { XPageStatus } from '../../../models/xpage-status.enum';

@Component({
  selector: 'em-notification-bar-status-text',
  templateUrl: 'notification-bar-status-text.component.html',
})
export class NotificationBarStatusTextComponent {
  @Input()
  set statusInfo(value: XPageStatusInfo) {
    this.text = this.getText(value.status);
    this.textParams = {
      pageName: value.pageName || '',
      status: this.getDocumentStatusText(value.xPageDocumentState),
      dateTime: this.datePipe.transform(value.scheduledDateTime, 'full'),
      projectName: value.projectName || '',
    };
  }

  text: string | undefined;
  textParams: {
    pageName: string,
    status: string | undefined,
    dateTime: string | null,
    projectName: string | undefined,
  } | undefined;

  constructor(
    private readonly datePipe: DatePipe,
  ) {}

  private getText(pageStatus: XPageStatus): string | undefined {
    const statusTextMap = {
      [XPageStatus.Published]: 'NOTIFICATION_BAR_XPAGE_LABEL_LIVE',
      [XPageStatus.Offline]: 'NOTIFICATION_BAR_XPAGE_LABEL_OFFLINE',
      [XPageStatus.UnpublishedChanges]: 'NOTIFICATION_BAR_XPAGE_LABEL_LIVE_UNPUBLISHED_CHANGES',
      [XPageStatus.PublicationRequest]: 'NOTIFICATION_BAR_XPAGE_LABEL_PUBLICATION_REQUESTED',
      [XPageStatus.RejectedRequest]: 'NOTIFICATION_BAR_XPAGE_LABEL_REQUEST_REJECTED',
      [XPageStatus.TakeOfflineRequest]: 'NOTIFICATION_BAR_XPAGE_LABEL_TAKE_OFFLINE_REQUESTED',
      [XPageStatus.ScheduledPublication]: 'NOTIFICATION_BAR_XPAGE_LABEL_SCHEDULED_PUBLICATION',
      [XPageStatus.ScheduledToTakeOffline]: 'NOTIFICATION_BAR_XPAGE_LABEL_SCHEDULED_TO_TAKE_OFFLINE',
      [XPageStatus.ScheduledPublicationRequest]: 'NOTIFICATION_BAR_XPAGE_LABEL_SCHEDULED_PUBLICATION_REQUESTED',
      [XPageStatus.ScheduledToTakeOfflineRequest]: 'NOTIFICATION_BAR_XPAGE_LABEL_SCHEDULED_TO_TAKE_OFFLINE_REQUESTED',
      [XPageStatus.ProjectInProgress]: 'NOTIFICATION_BAR_XPAGE_LABEL_PROJECT_IN_PROGRESS',
      [XPageStatus.ProjectInReview]: 'NOTIFICATION_BAR_XPAGE_LABEL_PROJECT_IN_REVIEW',
      [XPageStatus.ProjectPageApproved]: 'NOTIFICATION_BAR_XPAGE_LABEL_PROJECT_PAGE_APPROVED',
      [XPageStatus.ProjectPageRejected]: 'NOTIFICATION_BAR_XPAGE_LABEL_PROJECT_PAGE_REJECTED',
      [XPageStatus.ProjectRunning]: 'NOTIFICATION_BAR_XPAGE_LABEL_PROJECT_IS_RUNNING',
      [XPageStatus.EditingSharedContainers]: 'NOTIFICATION_BAR_XPAGE_LABEL_EDITING_SHARED_CONTAINERS',
    };

    return statusTextMap[pageStatus];
  }

  private getDocumentStatusText(state: DocumentState | undefined): string | undefined {
    switch (state) {
      case DocumentState.Live: return 'life';
      case DocumentState.Changed: return 'changed';
      case DocumentState.Unpublished: return 'unpublished changes';
      case DocumentState.New: return 'offline';
    }
  }
}
