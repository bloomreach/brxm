/*
 * Copyright 2017-2020 Hippo B.V. (http://www.onehippo.com)
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
    $q,
    $state,
    $transitions,
    $translate,
    ChannelService,
    CmsService,
    ContentService,
    EditContentService,
    FeedbackService,
    HippoIframeService,
    HstService,
    PageStructureService,
    ProjectService,
    RightSidePanelService,
    SiteMapService,
    Step1Service,
    Step2Service,
  ) {
    'ngInject';

    this.$q = $q;
    this.$state = $state;
    this.$translate = $translate;
    this.ChannelService = ChannelService;
    this.CmsService = CmsService;
    this.ContentService = ContentService;
    this.EditContentService = EditContentService;
    this.FeedbackService = FeedbackService;
    this.HippoIframeService = HippoIframeService;
    this.HstService = HstService;
    this.PageStructureService = PageStructureService;
    this.ProjectService = ProjectService;
    this.RightSidePanelService = RightSidePanelService;
    this.SiteMapService = SiteMapService;
    this.Step1Service = Step1Service;
    this.Step2Service = Step2Service;

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
        return this._step2(params.document, params.url, params.locale, params.componentInfo, params.xpage);
      },
    );

    CmsService.subscribe('kill-editor', (documentId) => {
      this._stopStep2(documentId);
    });

    ProjectService.beforeChange('createContent', (projectIdIdentical) => {
      if (!projectIdIdentical) {
        return this._beforeSwitchProject();
      }
      return this.$q.resolve();
    });
  }

  start(config) {
    this.$state.go('hippo-cm.channel.create-content-step-1', { config });
  }

  next(document, url, locale, xpage) {
    this.$state.go('hippo-cm.channel.create-content-step-2', {
      componentInfo: this.componentInfo,
      document,
      locale,
      url,
      xpage,
    });
  }

  async finish(documentId) {
    let experiencePage;
    let renderPathInfo;

    try {
      ({ data: { renderPathInfo, experiencePage } } = await this.HstService.doGet(documentId, 'representation'));
    } catch (ignore) {
      this.FeedbackService.showError(`Failed to retrieve Sitemap representation for document[${documentId}]`);
      this.HippoIframeService.reload();
      return;
    }

    if (!experiencePage) {
      this.HippoIframeService.reload();
      this.EditContentService.startEditing(documentId);
      return;
    }

    const pageMeta = this.PageStructureService.getPage().getMeta();
    if (pageMeta.getPathInfo() !== renderPathInfo) {
      this.HippoIframeService.load(renderPathInfo);
    }

    const siteMapId = this.ChannelService.getSiteMapId();
    this.SiteMapService.load(siteMapId); // reload sitemap (left side panel)

    this.EditContentService.startEditing(documentId, 'hippo-cm.channel.edit-page.content');
  }

  stop() {
    return this.$state.go('hippo-cm.channel');
  }

  _validateStep1(config) {
    if (config && config.documentTemplateQuery) {
      return true;
    }
    this.FeedbackService.showError(
      'Failed to open create-content-step1 sidepanel due to missing configuration option "documentTemplateQuery"',
    );
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

    this._showStep1Title(Object.keys(config.layouts || {}).length > 0 ? 'CREATE_XPAGE' : 'CREATE_CONTENT');
    this.RightSidePanelService.startLoading();

    return this.Step1Service.open(
      config.documentTemplateQuery,
      config.folderTemplateQuery,
      config.rootPath,
      config.defaultPath,
      config.layouts,
    ).then(() => {
      this.RightSidePanelService.stopLoading();
    });
  }

  _showStep1Title(key) {
    this.RightSidePanelService.clearContext();
    const title = this.$translate.instant(key);
    this.RightSidePanelService.setTitle(title);
  }

  _step2(document, url, locale, componentInfo, xpage) {
    this.RightSidePanelService.startLoading();
    this.Step2Service.open(document, url, locale, componentInfo, xpage)
      .then((documentType) => {
        const translationKey = xpage ? 'CREATE_XPAGE' : 'CREATE_NEW_DOCUMENT_TYPE';
        this._showStep2Title(translationKey, documentType);
        this.RightSidePanelService.stopLoading();
      });
  }

  _showStep2Title(key, documentType) {
    const documentTypeName = { documentType: documentType.displayName };
    const documentTitle = this.$translate.instant(key, documentTypeName);
    this.RightSidePanelService.setTitle(documentTitle);
  }

  generateDocumentUrlByName(name, locale) {
    return this.ContentService.generateDocumentUrl(name, locale);
  }

  _isStep2() {
    return this.$state.$current.name === 'hippo-cm.channel.create-content-step-2';
  }

  _stopStep2(documentId) {
    if (this._isStep2() && this.Step2Service.killEditor(documentId)) {
      this.stop();
    }
  }

  _beforeSwitchProject() {
    if (this._isStep2()) {
      return this.stop();
    }
    return this.$q.resolve();
  }
}

export default CreateContentService;
