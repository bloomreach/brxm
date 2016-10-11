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

const CMS_CONTEXT_PATH = window.parent ? window.parent.location.pathname : '/cms/';

class ContentService {

  constructor($http, PathService) {
    'ngInject';

    this.$http = $http;
    this.PathService = PathService;
  }

  getDocument(id) {
    return this._doGet('documents', id);
  }

  getDocumentType(id) {
    return this._doGet('documenttypes', id);
  }

  _doGet(path, id) {
    const apiUrl = this.PathService.concatPaths(CMS_CONTEXT_PATH, 'ws/content', path, id);
    return this.$http.get(apiUrl)
      .then(result => result.data);
  }
}

export default ContentService;
