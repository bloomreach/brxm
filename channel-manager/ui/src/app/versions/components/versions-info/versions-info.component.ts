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

import { Component, Input, OnInit } from '@angular/core';

import { ChannelService } from '../../../channels/services/channel.service';
import { IframeService } from '../../../channels/services/iframe.service';
import { ContentService } from '../../../content/services/content.service';
import { WorkflowService } from '../../../content/services/workflow.service';
import { VersionsInfo } from '../../models/versions-info.model';

@Component({
  selector: 'em-versions-info',
  templateUrl: './versions-info.component.html',
  styleUrls: ['./versions-info.component.scss'],
})
export class VersionsInfoComponent implements OnInit {
  @Input()
  documentId!: string;

  @Input()
  branchId!: string;

  @Input()
  unpublishedVariantId!: string;

  versionsInfo: VersionsInfo = { versions: [] };

  constructor(
    private readonly contentService: ContentService,
    private readonly iframeService: IframeService,
    private readonly channelService: ChannelService,
    private readonly workflowService: WorkflowService,
  ) { }

  ngOnInit(): void {
    setTimeout(() => {
      // Angular Element inputs are not yet initialized onInit
      this.getVersionsInfo();
    });
  }

  async getVersionsInfo(): Promise<void> {
    this.versionsInfo = await this.contentService.getDocumentVersionsInfo(this.documentId, this.branchId);
  }

  async selectVersion(versionUUID: string): Promise<void> {
    const newPath = this.createVersionPath(versionUUID);
    await this.iframeService.load(newPath);
  }

  async createVersion(): Promise<void> {
    await this.workflowService.createWorkflowAction(this.documentId, 'version');
    await this.getVersionsInfo();
  }

  async restoreVersion(versionUUID: string): Promise<void> {
    await this.workflowService.createWorkflowAction(this.documentId, 'restore', versionUUID);
    const currentPath = this.iframeService.getCurrentRenderPathInfo();
    const renderPath = this.channelService.makeRenderPath(currentPath);
    await this.iframeService.load(renderPath);
    await this.getVersionsInfo();
  }

  private createVersionPath(selectedVersionUUID: string): string {
    const currentPath = this.iframeService.getCurrentRenderPathInfo();
    const renderPath = this.channelService.makeRenderPath(currentPath);
    const versionParam = `br_version_uuid=${selectedVersionUUID}`;
    const index = this.versionsInfo.versions.findIndex(v => v.jcrUUID === selectedVersionUUID);

    if (index <= 0) {
      return renderPath;
    }

    if (renderPath.includes('?')) {
      return `${renderPath}&${versionParam}`;
    }

    return `${renderPath}?${versionParam}`;
  }

}
