/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

// TODO: Create spec file for this service

const CMS_CONTEXT_PATH = window.parent ? window.parent.location.pathname : '/cms/';
const CMS_CONTENT_REST_API_PATH = 'ws/content';

class ContentService {

  constructor($http, PathService) {
    'ngInject';

    this.$http = $http;
    this.PathService = PathService;
  }

  createDraft(id) {
    return this.$http.post(this._draftUrlForDocument(id))
      .then(result => result.data);
  }

  getDocumentType(id) {
    return this._doGet('documenttypes', id);
  }

  _doGet(path, id) {
    const apiUrl = this.PathService.concatPaths(CMS_CONTEXT_PATH, CMS_CONTENT_REST_API_PATH, path, id);
    return this.$http.get(apiUrl)
      .then(result => result.data);
  }

  _draftUrlForDocument(id) {
    return this.PathService.concatPaths(CMS_CONTEXT_PATH, CMS_CONTENT_REST_API_PATH, 'documents', id, 'draft');
  }
}

export default ContentService;
