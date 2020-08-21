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
import { XPageState } from '../../../models/xpage-state.model';
import { XPageStatus } from '../../../models/xpage-status.enum';
import { PageService } from '../../../services/page.service';

@Component({
  selector: 'em-notification-bar-status-text',
  templateUrl: 'notification-bar-status-text.component.html',
})
export class NotificationBarStatusTextComponent {
  @Input()
  set state(value: XPageState) {
    this.text = this.getText(value);
    this.textParams = {
      pageName: value.name,
      status: this.getDocumentStatusText(value.state),
      dateTime: this.datePipe.transform(value.scheduledRequest?.scheduledDate, 'full'),
    };
  }

  text: string | undefined;
  textParams: { pageName: string, status: string | undefined, dateTime: string | null } | undefined;

  constructor(
    private readonly pageService: PageService,
    private readonly datePipe: DatePipe,
  ) {}

  private getText(state: XPageState): string | undefined {
    const status = this.pageService.getXPageStatus(state);

    if (!status) {
      this.text = undefined;
      return;
    }

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
    };

    return statusTextMap[status];
  }

  private getDocumentStatusText(state: DocumentState): string | undefined {
    switch (state) {
      case DocumentState.Live: return 'life';
      case DocumentState.Unpublished: return 'unpublished changes';
      case DocumentState.New: return 'offline';
    }
  }
}
