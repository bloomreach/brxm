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

import { Inject, Injectable } from '@angular/core';

import { VersionHistory } from '../versions/models/version-history.model';

import { Ng1ContentService, NG1_CONTENT_SERVICE } from './ng1/content.ng1service';

@Injectable({
  providedIn: 'root',
})
export class ContentService {
  constructor(
    @Inject(NG1_CONTENT_SERVICE) private readonly ng1ContentService: Ng1ContentService,
  ) { }

  getDocumentVersions(id: string, branchId: string): Promise<VersionHistory> {
    return this.ng1ContentService.getDocumentVersions(id, branchId);
  }
}
