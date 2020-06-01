/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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
  constructor($http, $q, ConfigService, PathService) {
    'ngInject';

    this.$http = $http;
    this.$q = $q;
    this.ConfigService = ConfigService;
    this.PathService = PathService;

    this._queue = [];
    this._running = false;
  }

  branchDocument(id) {
    return this._send('POST', ['documents', id, 'branch']);
  }

  getEditableDocument(id) {
    return this._send('POST', ['documents', id, 'editable']);
  }

  getDocument(id, branchId) {
    return this._send('GET', ['documents', id, branchId]);
  }

  saveDocument(doc) {
    return this._send('PUT', ['documents', doc.id, 'editable'], doc);
  }

  discardChanges(id) {
    return this._send('DELETE', ['documents', id, 'editable']);
  }

  saveField(documentId, fieldName, value) {
    return this._send('PUT', ['documents', documentId, 'editable', fieldName], value);
  }

  deleteDocument(id) {
    return this._send('DELETE', ['documents', id]);
  }

  getDocumentType(id) {
    return this._send('GET', ['documenttypes', id], null, true);
  }

  _send(method, pathElements, data = null, async = false, params = {}) {
    const path = this.PathService.concatPaths(this.ConfigService.getCmsContextPath(), REST_API_PATH, ...pathElements);
    const url = encodeURI(path);
    const headers = {};
    const opts = { method, url, headers, data, params };
    const promise = async ? this.$http(opts) : this._schedule(opts);

    return promise.then(result => result.data);
  }

  _schedule(opts) {
    const defer = this.$q.defer();

    this._queue.push(() => {
      this.$http(opts)
        .then(
          result => defer.resolve(result),
          result => defer.reject(result))
        .finally(() => this._next());
    });

    if (!this._running) {
      this._running = true;
      this._next();
    }

    return defer.promise;
  }

  _next() {
    if (this._queue.length === 0) {
      this._running = false;
    } else {
      this._queue.shift()();
    }
  }
}

export default ContentService;
