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

import { Component, OnInit } from '@angular/core';
import { Subject } from 'rxjs';

import { ContentService } from '../../../content/services/content.service';
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
  versionsInfo$ = new Subject<VersionsInfo>();
  currentDocumentVersionUUID?: string;

  constructor(
    private readonly contentService: ContentService,
    private readonly projectService: ProjectService,
    private readonly pageService: PageService,
    private readonly pageStructureService: PageStructureService,
  ) { }

  ngOnInit(): void {
    this.getVersionsInfo();

    this.currentDocumentVersionUUID = this.pageStructureService.getUnpublishedVariantId();
  }

  async getVersionsInfo(): Promise<void> {
    const state = this.pageService.getXPageState();
    const documentId = state?.id || '';
    const branchId = this.projectService.getSelectedProjectId();

    const versionHistory = await this.contentService.getDocumentVersionsInfo(documentId, branchId);
    this.versionsInfo$.next(versionHistory);
  }
}
