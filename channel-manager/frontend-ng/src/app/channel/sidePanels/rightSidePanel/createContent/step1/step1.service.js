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

class Step1Service {
  constructor(
    ContentService,
  ) {
    'ngInject';

    this.ContentService = ContentService;
    this._resetState();
  }

  getData() {
    return this.data;
  }

  setLocale(locale) {
    this.data.locale = locale;
  }

  open(templateQuery, rootPath, defaultPath) {
    this._resetState();

    this.data.templateQuery = templateQuery;
    this.data.rootPath = rootPath;
    this.data.defaultPath = defaultPath;

    return this.getTemplateQuery(templateQuery)
      .then(templateQueryResult => this._onLoadDocumentTypes(templateQueryResult.documentTypes))
      .catch(error => this._onError(error, 'Unknown error loading template query'));
  }

  createDraft() {
    const draft = {
      name: this.data.nameField,
      slug: this.data.urlField,
      templateQuery: this.data.templateQuery,
      documentTypeId: this.data.documentType,
      rootPath: this.data.rootPath,
      defaultPath: this.data.defaultPath,
    };

    return this.ContentService._send('POST', ['documents'], draft);
  }

  getFolders(path) {
    return this.ContentService._send('GET', ['folders', path], null, true);
  }

  getTemplateQuery(id) {
    return this.ContentService._send('GET', ['templatequery', id], null, true);
  }

  stop() {
    this._resetState();
  }

  _onLoadDocumentTypes(types) {
    this.data.documentTypes = types;

    if (types.length === 1) {
      this.data.documentType = types[0].id;
    } else {
      this.data.documentType = '';
    }
  }

  _resetState() {
    this.data = {
      nameField: '',
      urlField: '',
      isUrlUpdating: false,
      locale: '',
      rootPath: '',
      defaultPath: '',
      templateQuery: '',
    };
  }
}

export default Step1Service;
