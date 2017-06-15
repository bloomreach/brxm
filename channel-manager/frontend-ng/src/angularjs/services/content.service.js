/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

const REST_API_PATH = 'ws/content';

class ContentService {

  constructor($http, ConfigService, PathService) {
    'ngInject';

    this.$http = $http;
    this.ConfigService = ConfigService;
    this.PathService = PathService;
  }

  createDraft(id) {
    return this._send('POST', ['documents', id, 'draft']);
  }

  saveDraft(doc) {
    return this._send('PUT', ['documents', doc.id, 'draft'], doc);
  }

  deleteDraft(id) {
    return this._send('DELETE', ['documents', id, 'draft']);
  }

  getDocumentType(id) {
    return this._send('GET', ['documenttypes', id]);
  }

  draftField(documentId, fieldName, value) {
    return this._send('PUT', ['documents', documentId, 'draft', fieldName], value);
  }

  _send(method, pathElements, data) {
    const path = this.PathService.concatPaths(this.ConfigService.getCmsContextPath(), REST_API_PATH, ...pathElements);
    const url = encodeURI(path);
    const headers = {};
    return this.$http({ method, url, headers, data })
      .then(result => result.data);
  }
}

export default ContentService;
