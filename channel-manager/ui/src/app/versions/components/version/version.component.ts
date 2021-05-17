/*!
 * Copyright 2021 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
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

import { Component, EventEmitter, HostBinding, Inject, Input, OnChanges, Output } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { UIRouterGlobals } from '@uirouter/core';
import moment from 'moment';

import { DateService } from '../../../services/date.service';
import { DocumentWorkflowService } from '../../../services/document-workflow.service';
import { Ng1IframeService, NG1_IFRAME_SERVICE } from '../../../services/ng1/iframe.ng1.service';
import { NG1_UI_ROUTER_GLOBALS } from '../../../services/ng1/ui-router-globals.ng1.service';
import { Version } from '../../models/version.model';
import { VersionsInfo } from '../../models/versions-info.model';
import { VersionsService } from '../../services/versions.service';

@Component({
  selector: 'em-version',
  templateUrl: './version.component.html',
  styleUrls: ['./version.component.scss'],
})
export class VersionComponent implements OnChanges {
  @Input()
  version!: Version;

  @Input()
  versionsInfo!: VersionsInfo;

  @HostBinding('class.selected')
  @Input()
  isSelected!: boolean;

  @Input()
  renderPath!: string;

  @Input()
  actionInProgress!: boolean;
  @Output()
  actionInProgressChange = new EventEmitter<boolean>();

  isScheduleFormVisible = false;

  currentDate?: Date;

  constructor(
    @Inject(NG1_IFRAME_SERVICE) private readonly ng1IframeService: Ng1IframeService,
    @Inject(NG1_UI_ROUTER_GLOBALS) private readonly ng1UiRouterGlobals: UIRouterGlobals,
    private readonly documentWorkflowService: DocumentWorkflowService,
    private readonly versionsService: VersionsService,
    private readonly dateService: DateService,
    private readonly translateService: TranslateService,
  ) { }

  ngOnChanges(): void {
    this.isScheduleFormVisible = false;
    this.currentDate = this.dateService.getCurrentDate();
  }

  async restoreVersion(versionUUID: string): Promise<void> {
    this.actionInProgressChange.emit(true);
    const documentId = this.ng1UiRouterGlobals.params.documentId;
    await this.documentWorkflowService.postAction(documentId, ['restore', versionUUID]);
    await this.ng1IframeService.load(this.renderPath);
    await this.versionsService.getVersionsInfo(documentId);
    this.actionInProgressChange.emit(false);
  }

  getTooltip(version: Version): string {
    if (!!version.campaign?.to && this.hasCampaignEnded(version)) {
      return this.translateService.instant('VERSION_TOOLTIP_CAMPAIGN_ENDED');
    }

    if (this.versionsInfo.live) {
      if (version.campaign && version.active) {
        return this.translateService.instant('VERSION_TOOLTIP_CAMPAIGN_LIVE');
      }

      if (version.campaign) {
        return this.translateService.instant('VERSION_TOOLTIP_CAMPAIGN_SCHEDULED');
      }

    }

    if (!this.versionsInfo.live) {
      if (version.campaign && version.active) {
        return this.translateService.instant('VERSION_TOOLTIP_CAMPAIGN_BLOCKED');
      }

      if (version.campaign) {
        return this.translateService.instant('VERSION_TOOLTIP_CAMPAIGN_WONT_RUN');
      }
    }

    if (version.published) {
      return this.translateService.instant('VERSION_TOOLTIP_PUBLISHED_VERSION');
    }

    return '';
  }

  getIcon(version: Version): string {
    if (!!version.campaign?.to && this.hasCampaignEnded(version)) {
      return 'info_outline';
    }

    if (version.campaign && version.active && !this.versionsInfo.live) {
      return 'remove_circle_outline';
    }

    if (version.campaign) {
      return 'schedule';
    }

    if (version.published) {
      return 'check_circle_outline';
    }

    return '';
  }

  hasCampaignEnded(version: Version): boolean {
    return moment(version.campaign?.to).isBefore(this.currentDate);
  }
}
