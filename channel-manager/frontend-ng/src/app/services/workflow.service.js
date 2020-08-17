/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

class WorkflowService {
  constructor($http, ConfigService, PathService) {
    'ngInject';

    this.$http = $http;
    this.baseUrl = PathService.concatPaths(ConfigService.getCmsContextPath(), '/ws/content/workflows/documents');
  }

  createWorkflowAction(documentId, ...pathElements) {
    const url = encodeURI(`${this.baseUrl}/${documentId}/${pathElements.join('/')}`);
    return this.$http
      .post(url)
      .then(response => response.data);
  }
}

export default WorkflowService;
