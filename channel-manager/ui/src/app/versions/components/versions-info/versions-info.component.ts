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

import { Component, NgZone, OnInit } from '@angular/core';
import { MatSelectionListChange } from '@angular/material/list';
import { merge, Subject } from 'rxjs';
import { map, startWith, tap } from 'rxjs/operators';

import { ChannelService } from '../../../channels/services/channel.service';
import { IframeService } from '../../../channels/services/iframe.service';
import { ContentService } from '../../../content/services/content.service';
import { WorkflowService } from '../../../content/services/workflow.service';
import { PageStructureService } from '../../../pages/services/page-structure.service';
import { PageService } from '../../../pages/services/page.service';
import { ProjectService } from '../../../projects/services/project.service';
import { VersionsInfo } from '../../models/versions-info.model';

@Component({
  selector: 'em-versions-info',
  templateUrl: './versions-info.component.html',
  styleUrls: ['./versions-info.component.scss'],
})
export class VersionsInfoComponent implements OnInit {
  xpageId?: string;
  branchId!: string;
  versionsInfo$ = new Subject<VersionsInfo>();
  currentDocumentVersionUUID$ = merge(
    this.pageStructureService.pageParsed$,
    this.versionsInfo$,
  ).pipe(
    startWith(1),
    map(() => this.zone.run(() => this.pageStructureService.getUnpublishedVariantId())),
    tap(value => console.log('recieved value', value)),
  );

  constructor(
    private readonly contentService: ContentService,
    private readonly projectService: ProjectService,
    private readonly pageService: PageService,
    private readonly pageStructureService: PageStructureService,
    private readonly iframeService: IframeService,
    private readonly channelService: ChannelService,
    private readonly workflowService: WorkflowService,
    private readonly zone: NgZone,
  ) { }

  ngOnInit(): void {
    this.xpageId = this.pageService.getXPageState()?.id;
    // projectService selected project is always set, in project.service.js
    // and by default with id 'master';
    this.branchId = this.projectService.getSelectedProjectId();

    if (!this.xpageId || !this.branchId) {
      return;
    }

    this.getVersionsInfo(this.xpageId, this.branchId);
  }

  async getVersionsInfo(documentId: string, branchId: string): Promise<void> {
    const versionHistory = await this.contentService.getDocumentVersionsInfo(documentId, branchId);
    this.versionsInfo$.next(versionHistory);
  }

  async selectVersion(event: MatSelectionListChange): Promise<void> {
    const currentPath = this.iframeService.getCurrentRenderPathInfo();
    const renderPath = this.channelService.makeRenderPath(currentPath);
    const newPath = this.createVersionPath(renderPath, event.option.value);
    await this.iframeService.load(newPath);
  }

  async restoreVersion(xpageId: string, versionUUID: string): Promise<void> {
    await this.workflowService.createWorkflowAction(xpageId, 'restore', versionUUID);
    const currentPath = this.iframeService.getCurrentRenderPathInfo();
    const renderPath = this.channelService.makeRenderPath(currentPath);
    await this.iframeService.load(renderPath);
    await this.getVersionsInfo(xpageId, this.branchId);
  }

  private createVersionPath(path: string, selectedVersionUUID: string): string {
    const versionParam = `br_version_uuid=${selectedVersionUUID}`;

    if (path.includes('?')) {
      return `${path}&${versionParam}`;
    }

    return `${path}?${versionParam}`;
  }

}
