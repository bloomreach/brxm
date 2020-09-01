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

import { Inject, Injectable, OnDestroy } from '@angular/core';

import { Ng1ContentService, NG1_CONTENT_SERVICE } from '../../services/ng1/content.ng1.service';
import { NG1_ROOT_SCOPE } from '../../services/ng1/root-scope.service';
import { PageStructureService } from '../../services/page-structure.service';
import { ProjectService } from '../../services/project.service';
import { Version } from '../models/version.model';

@Injectable({
  providedIn: 'root',
})
export class VersionsService implements OnDestroy {
  private unpublishedVariantId: string | undefined = undefined;
  private readonly onPageChangeUnsubscribe: () => void;
  private readonly onPageCheckChangesUnsubscribe: () => void;

  constructor(
    @Inject(NG1_ROOT_SCOPE) private readonly $rootScope: ng.IRootScopeService,
    @Inject(NG1_CONTENT_SERVICE) private readonly ng1ContentService: Ng1ContentService,
    private readonly pageStructureService: PageStructureService,
    private readonly projectService: ProjectService,
  ) {
    this.unpublishedVariantId = this.pageStructureService.getUnpublishedVariantId();

    this.onPageChangeUnsubscribe = this.$rootScope.$on('page:change', () => {
      this.unpublishedVariantId = this.pageStructureService.getUnpublishedVariantId();
    });
    this.onPageCheckChangesUnsubscribe = this.$rootScope.$on('page:check-changes', () => {
      this.unpublishedVariantId = this.pageStructureService.getUnpublishedVariantId();
    });
  }

  ngOnDestroy(): void {
    this.onPageChangeUnsubscribe();
    this.onPageCheckChangesUnsubscribe();
  }

  async getVersions(documentId: string): Promise<Version[]> {
    const branchId = this.projectService.getSelectedProjectId();

    const { versions } = await this.ng1ContentService.getDocumentVersionsInfo(documentId, branchId);

    return versions;
  }

  isCurrentVersion(version: Version): boolean {
    return version.jcrUUID === this.unpublishedVariantId;
  }
}
