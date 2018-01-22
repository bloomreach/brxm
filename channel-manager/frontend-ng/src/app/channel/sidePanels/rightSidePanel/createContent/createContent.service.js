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
  constructor(
    $state,
    $transitions,
    $translate,
    ContentService,
    EditContentService,
    Step1Service,
    Step2Service,
    RightSidePanelService,
  ) {
    'ngInject';

    this.$state = $state;
    this.$translate = $translate;
    this.ContentService = ContentService;
    this.EditContentService = EditContentService;
    this.Step1Service = Step1Service;
    this.Step2Service = Step2Service;
    this.RightSidePanelService = RightSidePanelService;

    $transitions.onEnter(
      { entering: '**.create-content-step-1' },
      transition => this._step1(transition.params().config),
    );
    $transitions.onEnter(
      { entering: '**.create-content-step-2' },
      transition => this._step2(transition.params().document, transition.params().step1),
    );
  }

  start(config) {
    this.$state.go('hippo-cm.channel.create-content-step-1', { config });
  }

  next(document, step1) {
    this.$state.go('hippo-cm.channel.create-content-step-2', { document, step1 });
  }

  finish(documentId) {
    this.EditContentService.startEditing(documentId);
  }

  stop() {
    this.$state.go('^');
  }

  _step1(config) {
    if (!config) {
      throw new Error('Input "options" is required');
    }

    if (!config.templateQuery) {
      throw new Error('Configuration option "templateQuery" is required');
    }

    this._showStep1Title();
    this.RightSidePanelService.startLoading();
    return this.Step1Service.open(config.templateQuery, config.rootPath, config.defaultPath)
      .then(() => {
        this.RightSidePanelService.stopLoading();
      });
  }

  _showStep1Title() {
    const title = this.$translate.instant('CREATE_CONTENT');
    this.RightSidePanelService.setTitle(title);
  }

  _step2(document, step1) {
    this.RightSidePanelService.startLoading();
    this.Step2Service.open(document, step1)
      .then((step2) => {
        this._showStep2Title(step2);
        this.RightSidePanelService.stopLoading();
      });
  }

  _showStep2Title(step2) {
    const documentTitle = this.$translate.instant('CREATE_NEW_DOCUMENT_TYPE', { documentType: step2.docType.displayName });
    this.RightSidePanelService.setTitle(documentTitle);
  }

  generateDocumentUrlByName(name, locale) {
    return this.ContentService._send('POST', ['slugs'], name, true, { locale });
  }
}

export default CreateContentService;
