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

import angular from 'angular';
import 'angular-mocks';

describe('PagePropertiesComponent', () => {
  let $compile;
  let $componentController;
  let $ctrl;
  let $mdDialog;
  let $q;
  let $rootScope;
  let $translate;
  let ChannelService;
  let FeedbackService;
  let HippoIframeService;
  let SiteMapItemService;
  let SiteMapService;
  let mockAlert;
  let siteMapItem;
  let pageModel;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((
      _$compile_,
      _$componentController_,
      _$mdDialog_,
      _$q_,
      _$rootScope_,
      _$translate_,
      _ChannelService_,
      _FeedbackService_,
      _HippoIframeService_,
      _SiteMapItemService_,
      _SiteMapService_,
    ) => {
      $compile = _$compile_;
      $componentController = _$componentController_;
      $mdDialog = _$mdDialog_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      $translate = _$translate_;
      ChannelService = _ChannelService_;
      FeedbackService = _FeedbackService_;
      HippoIframeService = _HippoIframeService_;
      SiteMapItemService = _SiteMapItemService_;
      SiteMapService = _SiteMapService_;
    });

    pageModel = {
      prototypes: [
        {
          id: 'prototype-a',
          displayName: 'Prototype A',
          hasContainerInPageDefinition: true,
        },
        {
          id: 'prototype-b',
          displayName: 'Prototype B',
          hasContainerInPageDefinition: false,
        },
      ],
    };

    siteMapItem = {
      id: 'siteMapItemId',
      parentId: null,
      name: 'name',
      pageTitle: 'title',
      availableDocumentRepresentations: [
        { displayName: 'document A', path: '/test/a' },
        { displayName: 'document B', path: '/test/b' },
      ],
      hasContainerItemInPageDefinition: true,
    };

    mockAlert = jasmine.createSpyObj('mockAlert', ['clickOutsideToClose', 'title', 'textContent', 'ok']);
    mockAlert.clickOutsideToClose.and.returnValue(mockAlert);
    mockAlert.title.and.returnValue(mockAlert);
    mockAlert.textContent.and.returnValue(mockAlert);
    mockAlert.ok.and.returnValue(mockAlert);

    spyOn($translate, 'instant').and.callFake(key => key);
    spyOn($mdDialog, 'alert').and.returnValue(mockAlert);
    spyOn($mdDialog, 'show');
    spyOn(ChannelService, 'getNewPageModel').and.returnValue($q.when(pageModel));
    spyOn(ChannelService, 'getSiteMapId').and.returnValue('siteMapId');
    spyOn(ChannelService, 'recordOwnChange');
    spyOn(FeedbackService, 'showErrorResponse');
    spyOn(HippoIframeService, 'reload');
    spyOn(SiteMapItemService, 'get').and.returnValue(siteMapItem);
    spyOn(SiteMapItemService, 'isEditable').and.returnValue(true);
    spyOn(SiteMapItemService, 'updateItem').and.returnValue($q.when());
    spyOn(SiteMapService, 'load');

    $ctrl = $componentController('pageProperties', null, {
      onDone: jasmine.createSpy('onDone'),
    });
  });

  describe('$element tests', () => {
    let $element;
    let $scope;

    function compileComponentAndGetController() {
      $scope = $rootScope.$new();
      $scope.onDone = jasmine.createSpy('onDone');
      $element = angular.element('<page-properties on-done="onDone()"> </page-properties>');
      $compile($element)($scope);
      $scope.$digest();

      return $element.controller('pageProperties');
    }

    it('calls the callback when navigating back', () => {
      compileComponentAndGetController();

      $element.find('.qa-button-back').click();
      expect($scope.onDone).toHaveBeenCalled();
    });
  });

  it('initializes correctly', () => {
    $ctrl.$onInit();
    $rootScope.$digest();

    expect($translate.instant).toHaveBeenCalledWith('SUBPAGE_PAGE_PROPERTIES_TITLE', { pageName: 'name' });
    expect($translate.instant).toHaveBeenCalledWith('SUBPAGE_PAGE_PROPERTIES_PRIMARY_DOCUMENT_VALUE_NONE');
    expect($ctrl.title).toBe('title');
    expect($ctrl.availableDocuments.length).toBe(3);
    expect($ctrl.availableDocuments[0].path).toBe('');
    expect($ctrl.primaryDocument).toBe($ctrl.availableDocuments[0]);
    expect($ctrl.isAssigningNewTemplate).toBeFalsy();
    expect(ChannelService.getNewPageModel).toHaveBeenCalled();
    $rootScope.$digest();

    expect($ctrl.prototypes.length).toBe(2);

    // try again with different document settings
    siteMapItem.primaryDocumentRepresentation = { path: '/test/b' };
    siteMapItem.availableDocumentRepresentations.shift();
    $ctrl.$onInit();
    $rootScope.$digest();

    expect($ctrl.primaryDocument).toBe($ctrl.availableDocuments[2]);

    siteMapItem.primaryDocumentRepresentation = { path: '/test/c' }; // no match, fallback to none-document
    siteMapItem.availableDocumentRepresentations.shift();
    $ctrl.$onInit();
    $rootScope.$digest();

    expect($ctrl.primaryDocument).toBe($ctrl.availableDocuments[0]);

    delete siteMapItem.availableDocumentRepresentations;
    $ctrl.$onInit();
    $rootScope.$digest();

    expect($ctrl.availableDocuments.length).toBe(1);
    expect($ctrl.primaryDocument).toBe($ctrl.availableDocuments[0]);

    siteMapItem.availableDocumentRepresentations = [];
    $ctrl.$onInit();
    $rootScope.$digest();

    expect($ctrl.availableDocuments.length).toBe(1);
    expect($ctrl.primaryDocument).toBe($ctrl.availableDocuments[0]);
  });

  it('flashes a toast when the retrieval of the templates fails', () => {
    ChannelService.getNewPageModel.and.returnValue($q.reject());
    $ctrl.$onInit();
    $rootScope.$digest();

    expect($ctrl.prototypes).toEqual([]);
    expect(FeedbackService.showErrorResponse).toHaveBeenCalledWith(undefined, 'ERROR_PAGE_MODEL_RETRIEVAL_FAILED');
  });

  it('saves the new item values successfully', () => {
    $ctrl.$onInit();
    $rootScope.$digest();

    $ctrl.title = 'newTitle';
    $ctrl.primaryDocument = $ctrl.availableDocuments[1];
    $ctrl.isAssigningNewTemplate = true;
    $ctrl.prototype = pageModel.prototypes[1];

    const savedItem = {
      id: 'siteMapItemId',
      parentId: null,
      name: 'name',
      pageTitle: 'newTitle',
      primaryDocumentRepresentation: $ctrl.availableDocuments[1],
      componentConfigurationId: 'prototype-b',
    };

    $ctrl.save();
    expect(SiteMapItemService.updateItem).toHaveBeenCalledWith(savedItem, 'siteMapId');
    $rootScope.$digest();

    expect(HippoIframeService.reload).toHaveBeenCalled();
    expect(SiteMapService.load).toHaveBeenCalledWith('siteMapId');
    expect(ChannelService.recordOwnChange).toHaveBeenCalled();
    expect($ctrl.onDone).toHaveBeenCalled();
  });

  it('tells the user that the page is already locked', () => {
    const response = { key: 'value' };
    SiteMapItemService.updateItem.and.returnValue($q.reject(response));
    $ctrl.$onInit();
    $rootScope.$digest();

    const savedItem = {
      id: 'siteMapItemId',
      parentId: null,
      name: 'name',
      pageTitle: 'title',
      primaryDocumentRepresentation: $ctrl.availableDocuments[0],
    };

    $ctrl.save();
    expect(SiteMapItemService.updateItem).toHaveBeenCalledWith(savedItem, 'siteMapId');
    $rootScope.$digest();

    expect(FeedbackService.showErrorResponse)
      .toHaveBeenCalledWith(response, 'ERROR_PAGE_SAVE_FAILED', $ctrl.errorMap);
    expect($ctrl.onDone).not.toHaveBeenCalled();
  });

  it('flashes a toast when saving failed', () => {
    SiteMapItemService.updateItem.and.returnValue($q.reject());
    $ctrl.$onInit();
    $rootScope.$digest();

    $ctrl.title = 'newTitle';
    $ctrl.primaryDocument = $ctrl.availableDocuments[0];

    const savedItem = {
      id: 'siteMapItemId',
      parentId: null,
      name: 'name',
      pageTitle: 'newTitle',
      primaryDocumentRepresentation: $ctrl.availableDocuments[0],
    };

    $ctrl.save();
    expect(SiteMapItemService.updateItem).toHaveBeenCalledWith(savedItem, 'siteMapId');
    $rootScope.$digest();

    expect(FeedbackService.showErrorResponse)
      .toHaveBeenCalledWith(undefined, 'ERROR_PAGE_SAVE_FAILED', $ctrl.errorMap);
    expect($ctrl.onDone).not.toHaveBeenCalled();
  });

  it('checks if the channel has prototypes available', () => {
    $ctrl.$onInit();
    $rootScope.$digest();

    expect($ctrl.hasPrototypes()).toBe(true);

    siteMapItem.availableDocumentRepresentations.shift();
    ChannelService.getNewPageModel.and.returnValue($q.when({ prototypes: [] }));
    $ctrl.$onInit();
    $rootScope.$digest();

    expect($ctrl.hasPrototypes()).toBe(false);

    siteMapItem.availableDocumentRepresentations.shift();
    ChannelService.getNewPageModel.and.returnValue($q.reject());
    $ctrl.$onInit();
    $rootScope.$digest();

    expect($ctrl.hasPrototypes()).toBe(false);
  });

  it('shows an alert dialog when assigning a new template to a page with container items', () => {
    $ctrl.$onInit();
    $rootScope.$digest();

    siteMapItem.hasContainerItemInPageDefinition = true;
    $ctrl.isAssigningNewTemplate = false;
    $ctrl.evaluatePrototype();
    expect($mdDialog.show).not.toHaveBeenCalled();

    siteMapItem.hasContainerItemInPageDefinition = false;
    $ctrl.isAssigningNewTemplate = false;
    $ctrl.evaluatePrototype();
    expect($mdDialog.show).not.toHaveBeenCalled();

    siteMapItem.hasContainerItemInPageDefinition = false;
    $ctrl.isAssigningNewTemplate = true;
    $ctrl.evaluatePrototype();
    expect($mdDialog.show).not.toHaveBeenCalled();

    $mdDialog.show.calls.reset();
    mockAlert.textContent.calls.reset();
    siteMapItem.hasContainerItemInPageDefinition = true;
    $ctrl.isAssigningNewTemplate = true;
    $ctrl.prototype = pageModel.prototypes[0]; // has containers
    $ctrl.evaluatePrototype();
    expect($mdDialog.show).toHaveBeenCalledWith(mockAlert);
    expect(mockAlert.textContent).toHaveBeenCalledWith('SUBPAGE_PAGE_PROPERTIES_ALERT_CONTENT_REPOSITIONING');

    $mdDialog.show.calls.reset();
    mockAlert.textContent.calls.reset();
    siteMapItem.hasContainerItemInPageDefinition = true;
    $ctrl.isAssigningNewTemplate = true;
    $ctrl.prototype = pageModel.prototypes[1]; // has no
    $ctrl.evaluatePrototype();
    expect($mdDialog.show).toHaveBeenCalledWith(mockAlert);
    expect(mockAlert.textContent).toHaveBeenCalledWith('SUBPAGE_PAGE_PROPERTIES_ALERT_CONTENT_REMOVAL');
  });
});
