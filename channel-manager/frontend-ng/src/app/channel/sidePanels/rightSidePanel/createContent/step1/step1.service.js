/*
 * Copyright 2018-2020 Hippo B.V. (http://www.onehippo.com)
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
    $q,
    $translate,
    ChannelService,
    ContentService,
    FeedbackService,
  ) {
    'ngInject';

    this.$q = $q;
    this.$translate = $translate;
    this.ChannelService = ChannelService;
    this.ContentService = ContentService;
    this.FeedbackService = FeedbackService;
    this._reset();
  }

  _reset() {
    delete this.name;
    delete this.url;
    delete this.locale;
    delete this.rootPath;
    delete this.defaultPath;
    delete this.documentTemplateQuery;
    delete this.folderTemplateQuery;
    delete this.experiencePage;
  }

  stop() {
    this._reset();
  }

  open(documentTemplateQuery, folderTemplateQuery, rootPath, defaultPath) {
    this._reset();

    this.rootPath = this._initRootPath(rootPath);
    this.defaultPath = defaultPath;
    this.documentTemplateQuery = documentTemplateQuery;
    this.folderTemplateQuery = folderTemplateQuery;
    this.experiencePage = false;

    return this.ContentService.getDocumentTemplateQuery(documentTemplateQuery)
      .then(documentTemplateQueryResult => this._onLoadDocumentTypes(documentTemplateQueryResult.documentTypes))
      .catch(error => this._onError(error, `Unexpected error loading template query "${documentTemplateQuery}"`));
  }

  /**
   * Parse the rootPath input value;
   * - use as is if rootPath is absolute
   * - concatenate with channelRootPath if rootPath is relative
   * - make sure it does not end with a slash
   *
   * @param rootPath the component's rootPath
   */
  _initRootPath(rootPath) {
    const channel = this.ChannelService.getChannel();
    if (!rootPath) {
      return null;
    }

    if (rootPath.endsWith('/')) {
      rootPath = rootPath.substring(0, rootPath.length - 1);
    }

    if (!rootPath.startsWith('/')) {
      rootPath = `${channel.contentRoot}/${rootPath}`;
    }
    return rootPath;
  }

  _onLoadDocumentTypes(types) {
    this.documentTypes = types;

    if (types.length === 1) {
      this.documentType = types[0].id;
    } else {
      this.documentType = '';
    }
  }

  createDocument() {
    const document = {
      name: this.name,
      slug: this.url,
      documentTemplateQuery: this.documentTemplateQuery,
      documentTypeId: this.documentType,
      folderTemplateQuery: this.folderTemplateQuery,
      rootPath: this.rootPath,
      defaultPath: this.defaultPath,
      experiencePage: this.experiencePage,
    };
    return this.ContentService.createDocument(document)
      .catch(error => this._onError(error, 'Unexpected error creating a new document'));
  }

  getFolders(path) {
    return this.ContentService.getFolders(path)
      .catch(error => this._onError(error, `Unexpected error loading folders for path ${path}`));
  }

  _onError(error, genericMessage) {
    const errorKey = (error.data && error.data.reason) ? `ERROR_${error.data.reason}` : genericMessage;
    if (error.data && error.data.params) {
      this.FeedbackService.showError(errorKey, error.data.params);
    } else {
      this.FeedbackService.showError(errorKey);
    }
    return this.$q.reject();
  }
}

export default Step1Service;
