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

import { Component, HostBinding, Input, OnChanges } from '@angular/core';

import { PageStates } from '../../../models/page-states.model';
import { XPageStatusInfo } from '../../../models/page-status-info.model';
import { Project } from '../../../models/project.model';
import { XPageStatus } from '../../../models/xpage-status.enum';
import { PageService } from '../../../services/page.service';

const DANGER_XPAGE_STATUSES = [
  XPageStatus.RejectedRequest,
  XPageStatus.ProjectPageRejected,
  XPageStatus.EditingSharedContainers,
];

@Component({
  templateUrl: 'notification-bar.component.html',
  styleUrls: ['notification-bar.component.scss'],
})
export class NotificationBarComponent implements OnChanges {
  @Input()
  pageStates!: PageStates;

  @Input()
  currentProject!: Project;

  @Input()
  isEditingSharedContainers = false;

  @HostBinding('class.danger')
  danger = false;

  pageStatusInfo: XPageStatusInfo | undefined;

  constructor(private readonly pageService: PageService) {}

  ngOnChanges(): void {
    // PageService extracts necessary data from appropriate services and use inputs for triggering change detection
    this.pageStatusInfo = this.pageService.getPageStatusInfo();
    this.danger = DANGER_XPAGE_STATUSES.some(x => x === this.pageStatusInfo?.status);
  }
}
