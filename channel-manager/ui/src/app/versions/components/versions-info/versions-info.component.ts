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

import { ChangeDetectorRef, Component, Inject, Input, OnDestroy, OnInit } from '@angular/core';

import { Ng1ChannelService, NG1_CHANNEL_SERVICE } from '../../../services/ng1/channel.ng1service';
import { Ng1ContentService, NG1_CONTENT_SERVICE } from '../../../services/ng1/content.ng1.service';
import { Ng1IframeService, NG1_IFRAME_SERVICE } from '../../../services/ng1/iframe.ng1service';
import { Ng1WorkflowService, NG1_WORKFLOW_SERVICE } from '../../../services/ng1/workflow.ng1.service';
import { VersionsInfo } from '../../models/versions-info.model';

@Component({
  selector: 'em-versions-info',
  templateUrl: 'versions-info.component.html',
  styleUrls: ['versions-info.component.scss'],
})
export class VersionsInfoComponent implements OnInit, OnDestroy {
  @Input()
  documentId!: string;

  @Input()
  branchId!: string;

  @Input()
  unpublishedVariantId!: string;

  versionsInfo?: VersionsInfo;

  actionInProgress = false;

  constructor(
    @Inject(NG1_CONTENT_SERVICE) private readonly ng1ContentService: Ng1ContentService,
    @Inject(NG1_IFRAME_SERVICE) private readonly ng1IframeService: Ng1IframeService,
    @Inject(NG1_CHANNEL_SERVICE) private readonly ng1ChannelService: Ng1ChannelService,
    @Inject(NG1_WORKFLOW_SERVICE) private readonly ng1WorkflowService: Ng1WorkflowService,
    private readonly changeDetector: ChangeDetectorRef,
  ) { }

  ngOnInit(): void {
    setTimeout(() => {
      // Angular Element inputs are not yet initialized onInit
      this.getVersionsInfo();
    });
  }

  ngOnDestroy(): void {
    const latestVersion = this.versionsInfo?.versions[0];
    const id = latestVersion?.jcrUUID;

    if (id && id !== this.unpublishedVariantId) {
      this.selectVersion(id);
    }
  }

  async getVersionsInfo(): Promise<void> {
    this.actionInProgress = true;
    this.versionsInfo = await this.ng1ContentService.getDocumentVersionsInfo(this.documentId, this.branchId);

    this.changeDetector.markForCheck();
    this.actionInProgress = false;
  }

  async selectVersion(versionUUID: string): Promise<void> {
    if (this.isVersionSelected(versionUUID)) {
      return;
    }

    this.actionInProgress = true;
    const newPath = this.createVersionPath(versionUUID);
    await this.ng1IframeService.load(newPath);
    this.actionInProgress = false;
  }

  async createVersion(): Promise<void> {
    this.actionInProgress = true;
    await this.ng1WorkflowService.createWorkflowAction(this.documentId, 'version');
    await this.getVersionsInfo();
  }

  async restoreVersion(versionUUID: string): Promise<void> {
    this.actionInProgress = true;
    await this.ng1WorkflowService.createWorkflowAction(this.documentId, 'restore', versionUUID);
    const renderPath = this.getRenderPath();
    await this.ng1IframeService.load(renderPath);
    await this.getVersionsInfo();
  }

  isVersionSelected(versionUUID: string): boolean {
    return this.unpublishedVariantId === versionUUID;
  }

  private getRenderPath(): string {
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
