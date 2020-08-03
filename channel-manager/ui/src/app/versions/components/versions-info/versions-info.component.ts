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
import { PageStructureService } from '../../../page-structure/services/page-structure.service';
import { VersionsInfo } from '../../models/versions-info.model';

@Component({
  selector: 'em-versions-info',
  templateUrl: './versions-info.component.html',
  styleUrls: ['./versions-info.component.scss'],
})
export class VersionsInfoComponent implements OnInit {
  versionsInfo$ = new Subject<VersionsInfo>();

  constructor(
    private readonly pageStructureService: PageStructureService,
    private readonly contentService: ContentService,
  ) { }

  ngOnInit(): void {
    this.getVersionsInfo();
  }

  async getVersionsInfo(): Promise<void> {
    const pageMeta = this.pageStructureService
      .getPage()
      .getMeta();

    const documentId = pageMeta.getUnpublishedVariantId();
    const branchId = pageMeta.getBranchId();

    if (documentId && branchId) {
      const versionHistory = await this.contentService.getDocumentVersionsInfo(documentId, branchId);
      this.versionsInfo$.next(versionHistory);
    }
  }
}
