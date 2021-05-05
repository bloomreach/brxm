/*!
 * Copyright 2021 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
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

import { Location } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';

import { Ng1ConfigService, NG1_CONFIG_SERVICE } from './ng1/config.ng1.service';
import { NG1_ROOT_SCOPE } from './ng1/root-scope.ng1.service';

@Injectable({
  providedIn: 'root',
})
export class DocumentWorkflowService {
  baseUrl = Location.joinWithSlash(this.configService.getCmsContextPath(), '/ws/content/workflows/documents');

  constructor(
    @Inject(NG1_ROOT_SCOPE) private readonly $rootScope: ng.IRootScopeService,
    @Inject(NG1_CONFIG_SERVICE) private readonly configService: Ng1ConfigService,
    private readonly http: HttpClient,
  ) {
  }
  async putAction(documentId: string, pathElements: string[], body = {}): Promise<any> {
    const url = encodeURI(`${this.baseUrl}/${documentId}/${pathElements.join('/')}`);
    const response = await this.http.put(url, body).toPromise();

    this.$rootScope.$emit('page:check-changes');

    return response;
  }

  async postAction(documentId: string, pathElements: string[], body = {}): Promise<any> {
    const url = encodeURI(`${this.baseUrl}/${documentId}/${pathElements.join('/')}`);
    const response = await this.http.post(url, body).toPromise();

    this.$rootScope.$emit('page:check-changes');

    return response;
  }
}
