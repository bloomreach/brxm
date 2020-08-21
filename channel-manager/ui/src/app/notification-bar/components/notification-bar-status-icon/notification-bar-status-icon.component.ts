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

import { XPageState } from '../../../models/xpage-state.model';
import { XPageStatus } from '../../../models/xpage-status.enum';
import { PageService } from '../../../services/page.service';

@Component({
  selector: 'em-notification-bar-status-icon',
  templateUrl: 'notification-bar-status-icon.component.html',
  styleUrls: ['notification-bar-status-icon.component.scss'],
})
export class NotificationBarStatusIconComponent {
  @Input()
  set state(value: XPageState) {
    const status = this.pageService.getXPageStatus(value);

    if (!status) {
      this.icon = undefined;
      return;
    }

    this.icon = this.getIcon(status);
  }

  icon: string | undefined;

  constructor(private readonly pageService: PageService) {}

  private getIcon(status: XPageStatus): string {
    const statusIconMap = {
      [XPageStatus.Published]: 'xpage',
      [XPageStatus.Offline]: 'minus-circle-outline',
      [XPageStatus.UnpublishedChanges]: 'alert-outline',
      [XPageStatus.PublicationRequest]: 'comment-processing-outline',
      [XPageStatus.RejectedRequest]: 'comment-remove-outline',
      [XPageStatus.TakeOfflineRequest]: 'comment-processing-outline',
      [XPageStatus.ScheduledPublication]: 'calendar-clock',
      [XPageStatus.ScheduledToTakeOffline]: 'calendar-clock',
      [XPageStatus.ScheduledPublicationRequest]: 'comment-processing-outline',
      [XPageStatus.ScheduledToTakeOfflineRequest]: 'comment-processing-outline',
    };

    return statusIconMap[status];
  }
}
