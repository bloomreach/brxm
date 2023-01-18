/*
 * Copyright 2020-2023 Bloomreach
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

import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { MatCheckboxChange } from '@angular/material/checkbox';
import { UIRouterGlobals } from '@uirouter/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { Ng1ChannelService, NG1_CHANNEL_SERVICE } from '../../../services/ng1/channel.ng1.service';
import { Ng1IframeService, NG1_IFRAME_SERVICE } from '../../../services/ng1/iframe.ng1.service';
import { NG1_UI_ROUTER_GLOBALS } from '../../../services/ng1/ui-router-globals.ng1.service';
import { NotificationService } from '../../../services/notification.service';
import { ProjectService } from '../../../services/project.service';
import { Version } from '../../models/version.model';
import { VersionsInfo } from '../../models/versions-info.model';
import { VersionsService } from '../../services/versions.service';

@Component({
  selector: 'em-versions-info',
  templateUrl: 'versions-info.component.html',
  styleUrls: ['versions-info.component.scss'],
})
export class VersionsInfoComponent implements OnInit, OnDestroy {
  private readonly documentId = this.ng1UiRouterGlobals.params.documentId;
  private readonly unsubscribe = new Subject();

  versionsInfo?: VersionsInfo;
  filteredVersions?: Version[];
  showFilteredVersions?: boolean;
  actionInProgress = false;

  constructor(
    @Inject(NG1_IFRAME_SERVICE) private readonly ng1IframeService: Ng1IframeService,
    @Inject(NG1_CHANNEL_SERVICE) private readonly ng1ChannelService: Ng1ChannelService,
    @Inject(NG1_UI_ROUTER_GLOBALS) private readonly ng1UiRouterGlobals: UIRouterGlobals,
    private readonly versionsService: VersionsService,
    private readonly notificationService: NotificationService,
    private readonly projectService: ProjectService,
  ) { }

  ngOnInit(): void {
    this.versionsService.getVersionsInfo(this.documentId);
    this.versionsService.versionsInfo$
      .pipe(takeUntil(this.unsubscribe))
      .subscribe(async versionsInfo => {
        this.versionsInfo = versionsInfo;
        this.filteredVersions = versionsInfo.versions;

        if (this.showFilteredVersions) {
          this.filteredVersions = await this.versionsService.getVersions(this.documentId, true);
        }
      });
  }

  ngOnDestroy(): void {
    const latestVersionId = this.versionsInfo?.versions[0]?.jcrUUID;

    if (latestVersionId) {
      this.selectVersion(latestVersionId);
    }

    this.unsubscribe.next();
    this.unsubscribe.complete();
  }

  async selectVersion(versionUUID: string): Promise<void> {
    if (this.isVersionSelected(versionUUID)) {
      return;
    }

    try {
      this.actionInProgress = true;
      const newPath = this.createVersionPath(versionUUID);
      await this.ng1IframeService.load(newPath);
    } catch (error) {
      this.notificationService.showErrorNotification('VERSION_SELECTION_ERROR');
    } finally {
      this.actionInProgress = false;
    }
  }

  isBranch(): boolean {
    return this.projectService.isBranch();
  }

  isVersionSelected(versionUUID: string): boolean {
    return this.versionsService.isVersionFromPage(versionUUID);
  }

  getRenderPath(): string {
    const currentPath = this.ng1IframeService.getCurrentRenderPathInfo();
    const homePageRenderPath = this.ng1ChannelService.getHomePageRenderPathInfo();
    return this.ng1ChannelService.makeRenderPath(currentPath.replace(homePageRenderPath, ''));
  }

  async filterVersions(change: MatCheckboxChange): Promise<void> {
    this.showFilteredVersions = change.checked;
    this.filteredVersions = await this.versionsService.getVersions(this.documentId, change.checked);
  }

  private createVersionPath(selectedVersionUUID: string): string {
    const renderPath = this.getRenderPath();
    const versionParam = `br_version_uuid=${selectedVersionUUID}`;
    const index = this.versionsInfo?.versions.findIndex(v => v.jcrUUID === selectedVersionUUID);

    if (index === undefined || index <= 0) {
      return renderPath;
    }

    if (renderPath.includes('?')) {
      return `${renderPath}&${versionParam}`;
    }

    return `${renderPath}?${versionParam}`;
  }

}
