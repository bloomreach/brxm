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

import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { UIRouterGlobals } from '@uirouter/core';
import { Subject } from 'rxjs';
import { takeUntil, tap } from 'rxjs/operators';

import { Ng1ChannelService, NG1_CHANNEL_SERVICE } from '../../../services/ng1/channel.ng1.service';
import { Ng1IframeService, NG1_IFRAME_SERVICE } from '../../../services/ng1/iframe.ng1.service';
import { NG1_UI_ROUTER_GLOBALS } from '../../../services/ng1/ui-router-globals.ng1.service';
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

  actionInProgress = false;

  constructor(
    @Inject(NG1_IFRAME_SERVICE) private readonly ng1IframeService: Ng1IframeService,
    @Inject(NG1_CHANNEL_SERVICE) private readonly ng1ChannelService: Ng1ChannelService,
    @Inject(NG1_UI_ROUTER_GLOBALS) private readonly ng1UiRouterGlobals: UIRouterGlobals,
    private readonly versionsService: VersionsService,
  ) { }

  ngOnInit(): void {
    this.versionsService.getVersionsInfo(this.documentId);
    this.versionsService.versionsInfo$
      .pipe(takeUntil(this.unsubscribe))
      .subscribe(versionsInfo => {
        this.versionsInfo = versionsInfo;
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
    this.actionInProgress = true;
    if (this.isVersionSelected(versionUUID)) {
      return;
    }

    const newPath = this.createVersionPath(versionUUID);
    await this.ng1IframeService.load(newPath);
    this.actionInProgress = false;
  }

  isVersionSelected(versionUUID: string): boolean {
    return this.versionsService.isVersionFromPage(versionUUID);
  }

  getRenderPath(): string {
    const currentPath = this.ng1IframeService.getCurrentRenderPathInfo();
    const homePageRenderPath = this.ng1ChannelService.getHomePageRenderPathInfo();
    return this.ng1ChannelService.makeRenderPath(currentPath.replace(homePageRenderPath, ''));
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
