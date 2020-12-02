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

describe('CreateContentService', () => {
  let $q;
  let $rootScope;
  let $state;
  let $translate;
  let $window;
  let ChannelService;
  let ContentService;
  let CreateContentService;
  let EditContentService;
  let FeedbackService;
  let HippoIframeService;
  let HstService;
  let PageStructureService;
  let RightSidePanelService;
  let SiteMapService;
  let Step1Service;
  let Step2Service;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.createContentModule');

    ContentService = jasmine.createSpyObj('ContentService', ['generateDocumentUrl']);
    ContentService.generateDocumentUrl.and.returnValue(Promise.resolve());

    RightSidePanelService = jasmine.createSpyObj('RightSidePanelService', [
      'clearContext', 'setTitle', 'startLoading', 'stopLoading',
    ]);

    angular.mock.module(($provide) => {
      $provide.value('ContentService', ContentService);
      $provide.value('RightSidePanelService', RightSidePanelService);
    });

    inject((
      _$q_,
      _$rootScope_,
      _$state_,
      _$translate_,
      _$window_,
      _ChannelService_,
      _CreateContentService_,
      _EditContentService_,
      _FeedbackService_,
      _HippoIframeService_,
      _HstService_,
      _PageStructureService_,
      _SiteMapService_,
      _Step1Service_,
      _Step2Service_,
    ) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      $state = _$state_;
      $translate = _$translate_;
      $window = _$window_;
      ChannelService = _ChannelService_;
      CreateContentService = _CreateContentService_;
      EditContentService = _EditContentService_;
      FeedbackService = _FeedbackService_;
      HippoIframeService = _HippoIframeService_;
      HstService = _HstService_;
      PageStructureService = _PageStructureService_;
      SiteMapService = _SiteMapService_;
      Step1Service = _Step1Service_;
      Step2Service = _Step2Service_;
    });

    spyOn($translate, 'instant').and.callThrough();
  });

  describe('validate step1', () => {
    it('does not transition to "step1" state if configuration is invalid', () => {
      CreateContentService.start();
      $rootScope.$apply();

      expect(CreateContentService.$state.$current.name).toBe('hippo-cm');
    });

    it('displays an error if configuration is missing property "documentTemplateQuery"', () => {
      spyOn(FeedbackService, 'showError');

      CreateContentService.start({});
      $rootScope.$apply();

      expect(FeedbackService.showError).toHaveBeenCalledWith(
        'Failed to open create-content-step1 sidepanel due to missing configuration option "documentTemplateQuery"',
      );
    });
  });

  it('starts creating a new document', () => {
    spyOn(Step1Service, 'open').and.returnValue($q.resolve());
    const config = { documentTemplateQuery: 'tpl-query' };
    CreateContentService.start(config);
    $rootScope.$digest();

    expect(RightSidePanelService.clearContext).toHaveBeenCalled();
    expect(RightSidePanelService.setTitle).toHaveBeenCalledWith('CREATE_CONTENT');
    expect(RightSidePanelService.startLoading).toHaveBeenCalled();
    expect(Step1Service.open).toHaveBeenCalledWith('tpl-query', undefined, undefined, undefined, undefined);
    expect(RightSidePanelService.stopLoading).toHaveBeenCalled();
    expect(CreateContentService.componentInfo).toEqual({});
  });

  it('starts creating a new document for a component', () => {
    spyOn(Step1Service, 'open').and.returnValue($q.resolve());

    const component = jasmine.createSpyObj('Component', ['getId', 'getLabel', 'getRenderVariant']);
    component.getId.and.returnValue('1234');
    component.getLabel.and.returnValue('Banner');
    component.getRenderVariant.and.returnValue('hippo-default');
    const config = {
      documentTemplateQuery: 'tpl-query',
      folderTemplateQuery: 'fldr-tpl-query',
      containerItem: component,
      parameterName: 'document',
      parameterBasePath: '/content/documents/channel',
    };
    CreateContentService.start(config);
    $rootScope.$digest();

    expect(RightSidePanelService.clearContext).toHaveBeenCalled();
    expect(RightSidePanelService.setTitle).toHaveBeenCalledWith('CREATE_CONTENT');
    expect(RightSidePanelService.startLoading).toHaveBeenCalled();
    expect(Step1Service.open).toHaveBeenCalledWith('tpl-query', 'fldr-tpl-query', undefined, undefined, undefined);
    expect(RightSidePanelService.stopLoading).toHaveBeenCalled();
    expect(CreateContentService.componentInfo).toEqual({
      id: '1234',
      label: 'Banner',
      variant: 'hippo-default',
      parameterName: 'document',
      parameterBasePath: '/content/documents/channel',
    });
  });

  it('opens the second step of creating a new document', () => {
    const docType = { displayName: 'document-type-name' };
    spyOn(Step2Service, 'open').and.returnValue($q.resolve(docType));
    CreateContentService.componentInfo = {
      id: '1234',
      label: 'Banner',
      variant: 'hippo-default',
      parameterName: 'document',
      parameterBasePath: '/content/documents/channel',
    };
    CreateContentService.next({}, 'url', 'locale');
    $rootScope.$digest();

    expect($translate.instant).toHaveBeenCalledWith('CREATE_NEW_DOCUMENT_TYPE', { documentType: 'document-type-name' });
    expect(RightSidePanelService.setTitle).toHaveBeenCalledWith('CREATE_NEW_DOCUMENT_TYPE');
    expect(RightSidePanelService.startLoading).toHaveBeenCalled();
    expect(Step2Service.open).toHaveBeenCalledWith({}, 'url', 'locale', CreateContentService.componentInfo, false);
    expect(RightSidePanelService.stopLoading).toHaveBeenCalled();
  });

  it('cancels creating a new document', () => {
    spyOn($state, 'go');
    CreateContentService.stop();
    expect($state.go).toHaveBeenCalledWith('hippo-cm.channel');
  });

  describe('creating a new page', () => {
    it('opens the second step of creating a new page', () => {
      spyOn(Step2Service, 'open').and.returnValue($q.resolve({ displayName: 'page-type-name' }));

      CreateContentService.next({}, 'url', 'locale', true);
      $rootScope.$digest();

      expect($translate.instant).toHaveBeenCalledWith('CREATE_XPAGE', { documentType: 'page-type-name' });
      expect(Step2Service.open).toHaveBeenCalledWith({}, 'url', 'locale', {}, true);
    });
  });

  describe('validate config data for transition to step1', () => {
    it('should have a documentTemplateQuery configuration option', () => {
      spyOn(FeedbackService, 'showError');
      CreateContentService.start();
      expect(FeedbackService.showError).toHaveBeenCalledWith(
        'Failed to open create-content-step1 sidepanel due to missing configuration option "documentTemplateQuery"',
      );
    });
  });

  describe('finish', () => {
    let representation;

    beforeEach(() => {
      representation = {
        experiencePage: false,
        renderPathInfo: 'new-render-path',
      };
      spyOn(HstService, 'doGet').and.returnValue($q.resolve({ data: representation }));
    });

    it('request a SiteMapPageRepresentation', () => {
      CreateContentService.finish('document-id');
      $rootScope.$digest();

      expect(HstService.doGet).toHaveBeenCalledWith('document-id', 'representation');
    });

    it('shows an error and reloads the page if the request for a SiteMapPageRepresentation fails', () => {
      spyOn(FeedbackService, 'showError');
      spyOn(HippoIframeService, 'reload');
      HstService.doGet.and.returnValue($q.reject('failed to load representation'));

      CreateContentService.finish('document-id');
      $rootScope.$digest();

      expect(FeedbackService.showError).toHaveBeenCalled();
      expect(HippoIframeService.reload).toHaveBeenCalled();
    });

    describe('for documents', () => {
      it('should reload the iframe', () => {
        spyOn(HippoIframeService, 'reload');

        CreateContentService.finish('document-id');
        $rootScope.$digest();

        expect(HippoIframeService.reload).toHaveBeenCalled();
      });

      it('should switch to state "edit-content"', () => {
        spyOn(EditContentService, 'startEditing');

        CreateContentService.finish('document-id');
        $rootScope.$digest();

        expect(EditContentService.startEditing).toHaveBeenCalledWith('document-id');
      });
    });

    describe('for pages', () => {
      beforeEach(() => {
        representation.experiencePage = true;

        const page = jasmine.createSpyObj('page', ['getMeta']);
        const pageMeta = jasmine.createSpyObj('pageMeta', ['getPathInfo']);
        spyOn(PageStructureService, 'getPage').and.returnValue(page);
        page.getMeta.and.returnValue(pageMeta);
        pageMeta.getPathInfo.and.returnValue('old-render-path');
      });

      it('should load the renderPath of a new XPage document', () => {
        spyOn(HippoIframeService, 'load');

        CreateContentService.finish('document-id');
        $rootScope.$digest();

        expect(HippoIframeService.load).toHaveBeenCalled();
      });

      it('should reload the sitemap-listing in the left side-panel', () => {
        spyOn(ChannelService, 'getSiteMapId').and.returnValue('current-sitemap');
        spyOn(SiteMapService, 'load');

        CreateContentService.finish('document-id');
        $rootScope.$digest();

        expect(SiteMapService.load).toHaveBeenCalledWith('current-sitemap');
      });

      it('should switch to state "edit-page"', () => {
        spyOn(EditContentService, 'startEditing');
        representation.experiencePage = true;

        CreateContentService.finish('document-id');
        $rootScope.$digest();

        expect(EditContentService.startEditing).toHaveBeenCalledWith(
          'document-id',
          'hippo-cm.channel.edit-page.content',
        );
      });
    });
  });

  it('generates a document url by calling the content service', () => {
    CreateContentService.generateDocumentUrlByName('name', 'nl');
    expect(ContentService.generateDocumentUrl).toHaveBeenCalledWith('name', 'nl');
  });

  describe('reacts to "kill-editor" events from the CMS', () => {
    it('never stops if step1 is active', () => {
      spyOn(CreateContentService, 'stop');
      CreateContentService.$state.$current.name = 'hippo-cm.channel.create-content-step-1';

      $window.CMS_TO_APP.publish('kill-editor', 'document-id');

      expect(CreateContentService.stop).not.toHaveBeenCalled();
    });

    it('only stops if step2 is active and Step2Service can be killed', () => {
      CreateContentService.$state.$current.name = 'hippo-cm.channel.create-content-step-2';
      spyOn(CreateContentService, 'stop');
      spyOn(Step2Service, 'killEditor');

      Step2Service.killEditor.and.returnValue(false);
      $window.CMS_TO_APP.publish('kill-editor', 'document-id');

      expect(CreateContentService.stop).not.toHaveBeenCalled();

      Step2Service.killEditor.and.returnValue(true);
      $window.CMS_TO_APP.publish('kill-editor', 'document-id');

      expect(CreateContentService.stop).toHaveBeenCalled();
      expect(Step2Service.killEditor).toHaveBeenCalledWith('document-id');
    });
  });
});
