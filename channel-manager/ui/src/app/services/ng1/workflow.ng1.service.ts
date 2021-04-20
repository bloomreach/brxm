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

import { InjectionToken } from '@angular/core';

import { VersionUpdateBody } from '../../versions/models/version.model';

export interface Ng1WorkflowService {
  createWorkflowAction<T>(documentId: string, body: object, ...pathElements: string[]): Promise<T>;
  updateWorkflowAction<T>(documentId: string, body: VersionUpdateBody, ...pathElements: string[]): Promise<T>;
}

export const NG1_WORKFLOW_SERVICE = new InjectionToken<Ng1WorkflowService>('NG1_WORKFLOW_SERVICE');
