/*!
 * Copyright 2020-2021 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
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

import { Inject, Injectable } from '@angular/core';
import { ReplaySubject } from 'rxjs';

import { DocumentWorkflowService } from '../../services/document-workflow.service';
import { Ng1ContentService, NG1_CONTENT_SERVICE } from '../../services/ng1/content.ng1.service';
import { PageStructureService } from '../../services/page-structure.service';
import { ProjectService } from '../../services/project.service';
import { Version, VersionUpdateBody } from '../models/version.model';
import { VersionsInfo } from '../models/versions-info.model';

@Injectable({
  providedIn: 'root',
})
export class VersionsService {
  private readonly versionsInfo = new ReplaySubject<VersionsInfo>(1);
  readonly versionsInfo$ = this.versionsInfo.asObservable();

  constructor(
    @Inject(NG1_CONTENT_SERVICE) private readonly ng1ContentService: Ng1ContentService,
    private readonly pageStructureService: PageStructureService,
    private readonly projectService: ProjectService,
    private readonly documentWorkflowService: DocumentWorkflowService,
  ) { }

  async getVersionsInfo(documentId: string): Promise<void> {
    const branchId = this.projectService.getSelectedProjectId();
    const versionsInfo = await this.ng1ContentService.getDocumentVersionsInfo(documentId, branchId);
    this.versionsInfo.next(versionsInfo);
  }

  async getVersionsInfoCampaignsOnly(documentId: string): Promise<VersionsInfo> {
    const branchId = this.projectService.getSelectedProjectId();
    const versionInfo = await this.ng1ContentService.getDocumentVersionsInfo(documentId, branchId, { campaignVersionOnly: true });
    return versionInfo;
  }

  async getVersions(documentId: string, campaignVersionOnly?: boolean): Promise<Version[]> {
    const branchId = this.projectService.getSelectedProjectId();
    const { versions } = await this.ng1ContentService.getDocumentVersionsInfo(documentId, branchId, { campaignVersionOnly });
    return versions;
  }

  async updateVersion(documentId: string, versionUUID: string, body: VersionUpdateBody): Promise<void> {
    const branchId = this.projectService.getSelectedProjectId();
    return this.documentWorkflowService.putAction(documentId, [branchId, 'versions', versionUUID], body);
  }

  isVersionFromPage(versionUUID: string): boolean {
    return versionUUID === this.pageStructureService.getUnpublishedVariantId();
  }
}
