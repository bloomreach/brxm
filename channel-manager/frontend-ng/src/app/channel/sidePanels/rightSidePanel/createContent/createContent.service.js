/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

class CreateContentService {
  constructor($state, $transitions, $translate, ContentEditor, ContentService, RightSidePanelService) {
    'ngInject';

    this.doc = null;
    this.$state = $state;
    this.$translate = $translate;
    this.ContentEditor = ContentEditor;
    this.ContentService = ContentService;
    this.RightSidePanelService = RightSidePanelService;

    $transitions.onEnter(
      { entering: '**.create-content-step-1' },
      () => this._init(),
    );
  }

  _init() {
    // TODO: translate title
    this.RightSidePanelService.setTitle('Create new content');
  }

  start() {
    this.$state.go('hippo-cm.channel.create-content-step-1');
  }

  stop() {
    this.$state.go('^');
  }

  getTemplateQuery(id) {
    return this.ContentService._send('GET', ['templatequery', id], null, true);
  }

  getDocument() {
    return this.doc;
  }

  createDraft(documentDetails) {
    return this.ContentService._send('POST', ['documents'], documentDetails).then((doc) => {
      this.doc = doc;
      return doc;
    });
  }

  generateDocumentUrlByName(name, locale) {
    return this.ContentService._send('POST', ['slugs'], name, true, { locale });
  }

  getFolders(path) {
    return this.ContentService._send('GET', ['folders', path], null, true);
  }

  deleteDraft(id) {
    return this.ContentService._send('DELETE', ['documents', id]);
  }

  setDraftNameUrl(documentId, data) {
    return this.ContentService._send('PUT', ['documents', documentId], { displayName: data.name, urlName: data.url });
  }
}

export default CreateContentService;
