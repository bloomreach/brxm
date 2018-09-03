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
    CmsService,
    ContentService,
    EditContentService,
    FeedbackService,
    HippoIframeService,
    Step1Service,
    Step2Service,
    RightSidePanelService,
  ) {
    'ngInject';

    this.$state = $state;
    this.$translate = $translate;
    this.ContentService = ContentService;
    this.EditContentService = EditContentService;
    this.FeedbackService = FeedbackService;
    this.HippoIframeService = HippoIframeService;
    this.Step1Service = Step1Service;
    this.Step2Service = Step2Service;
    this.RightSidePanelService = RightSidePanelService;
    this.CmsService = CmsService;

    $transitions.onBefore(
      { to: '**.create-content-step-1' },
      transition => this._validateStep1(transition.params().config),
    );
    $transitions.onEnter(
      { entering: '**.create-content-step-1' },
      transition => this._step1(transition.params().config),
    );
    $transitions.onEnter(
      { entering: '**.create-content-step-2' },
      (transition) => {
        const params = transition.params();
        return this._step2(params.document, params.url, params.locale, params.componentInfo);
      },
    );

    CmsService.subscribe('kill-editor', (documentId) => {
      this._stopStep2(documentId);
    });
  }

  start(config) {
    this.$state.go('hippo-cm.channel.create-content-step-1', { config });
  }

  next(document, url, locale) {
    const componentInfo = this.componentInfo;
    this.$state.go('hippo-cm.channel.create-content-step-2', { document, url, locale, componentInfo });
  }

  finish(documentId) {
    this.HippoIframeService.reload();
    this.EditContentService.startEditing(documentId);
  }

  stop() {
    this.$state.go('^');
  }

  _validateStep1(config) {
    if (config && config.documentTemplateQuery) {
      return true;
    }
    this.FeedbackService.showError('Failed to open create-content-step1 sidepanel due to missing configuration option "documentTemplateQuery"');
    return false;
  }

  _step1(config) {
    const component = config.containerItem;
    if (component) {
      if (config.parameterName) {
        this.CmsService.reportUsageStatistic('CreateContentButtonWithComponent');
      } else {
        this.CmsService.reportUsageStatistic('CreateContentButton');
      }
      this.componentInfo = {
        id: component.getId(),
        label: component.getLabel(),
        variant: component.getRenderVariant(),
        parameterName: config.parameterName,
        parameterBasePath: config.parameterBasePath,
      };
    } else {
      this.componentInfo = {};
    }

    this._showStep1Title();
    this.RightSidePanelService.startLoading();
    return this.Step1Service.open(config.documentTemplateQuery, config.rootPath, config.defaultPath)
      .then(() => {
        this.RightSidePanelService.stopLoading();
      });
  }

  _showStep1Title() {
    const title = this.$translate.instant('CREATE_CONTENT');
    this.RightSidePanelService.setTitle(title);
  }

  _step2(document, url, locale, componentInfo) {
    this.RightSidePanelService.startLoading();
    this.Step2Service.open(document, url, locale, componentInfo)
      .then((documentType) => {
        this._showStep2Title(documentType);
        this.RightSidePanelService.stopLoading();
      });
  }

  _showStep2Title(documentType) {
    const documentTypeName = { documentType: documentType.displayName };
    const documentTitle = this.$translate.instant('CREATE_NEW_DOCUMENT_TYPE', documentTypeName);
    this.RightSidePanelService.setTitle(documentTitle);
  }

  generateDocumentUrlByName(name, locale) {
    return this.ContentService._send('POST', ['slugs'], name, true, { locale });
  }

  _stopStep2(documentId) {
    if (this.$state.$current.name === 'hippo-cm.channel.create-content-step-2'
      && this.Step2Service.killEditor(documentId)) {
      this.stop();
    }
  }
}

export default CreateContentService;
