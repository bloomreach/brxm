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

import { Component, Input } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

import { DocumentState } from '../../../models/document-state.enum';
import { XPageStatusInfo } from '../../../models/page-status-info.model';
import { XPageStatus } from '../../../models/xpage-status.enum';
import { MomentPipe } from '../../../shared/pipes/moment.pipe';

@Component({
  selector: 'em-notification-bar-status-text',
  templateUrl: 'notification-bar-status-text.component.html',
})
export class NotificationBarStatusTextComponent {
  @Input()
  set statusInfo(value: XPageStatusInfo) {
    this.text = this.getText(value.status);

    const statusTextTranslationKey = this.getDocumentStatusTextTranslationKey(value.xPageDocumentState);
    const translatedStatusText = statusTextTranslationKey ? this.translateService.instant(statusTextTranslationKey) : undefined;

    this.textParams = {
      pageName: value.pageName || '',
      status: translatedStatusText,
      dateTime: value.scheduledDateTime ? this.momentPipe.transform(value.scheduledDateTime) : undefined,
      projectName: value.projectName || '',
      versionNumber: value.version?.timestamp ? this.momentPipe.transform(value.version.timestamp) : undefined,
      userName: value.lockedByUsername,
    };
  }

  text: string | undefined;
  textParams: {
    pageName: string,
    status: string | undefined,
    dateTime: string | undefined,
    projectName: string | undefined,
    versionNumber: string | undefined,
    userName: string | undefined,
  } | undefined;

  constructor(
    private readonly momentPipe: MomentPipe,
    private readonly translateService: TranslateService,
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
      [XPageStatus.NotPartOfProject]: 'NOTIFICATION_BAR_XPAGE_LABEL_NOT_PART_OF_PROJECT',
      [XPageStatus.ProjectInProgress]: 'NOTIFICATION_BAR_XPAGE_LABEL_PROJECT_IN_PROGRESS',
      [XPageStatus.ProjectInReview]: 'NOTIFICATION_BAR_XPAGE_LABEL_PROJECT_IN_REVIEW',
      [XPageStatus.ProjectPageApproved]: 'NOTIFICATION_BAR_XPAGE_LABEL_PROJECT_PAGE_APPROVED',
      [XPageStatus.ProjectPageRejected]: 'NOTIFICATION_BAR_XPAGE_LABEL_PROJECT_PAGE_REJECTED',
      [XPageStatus.ProjectRunning]: 'NOTIFICATION_BAR_XPAGE_LABEL_PROJECT_IS_RUNNING',
      [XPageStatus.EditingSharedContainers]: 'NOTIFICATION_BAR_XPAGE_LABEL_EDITING_SHARED_CONTAINERS',
      [XPageStatus.PreviousVersion]: 'NOTIFICATION_BAR_XPAGE_LABEL_PREVIOUS_VERSION',
      [XPageStatus.Locked]: 'NOTIFICATION_BAR_XPAGE_LABEL_LOCKED_BY_USER',
      [XPageStatus.Live]: 'NOTIFICATION_BAR_XPAGE_LABEL_VERSION_LIVE',
      [XPageStatus.ScheduledCampaign]: 'NOTIFICATION_BAR_XPAGE_LABEL_VERSION_SCHEDULED',
    };

    return statusTextMap[pageStatus];
  }

  private getDocumentStatusTextTranslationKey(state: DocumentState | undefined): string | undefined {
    switch (state) {
      case DocumentState.Live: return 'LIVE';
      case DocumentState.Changed: return 'CHANGED';
      case DocumentState.Unpublished: return 'UNPUBLISHED_CHANGES';
      case DocumentState.New: return 'OFFLINE';
    }
  }
}
