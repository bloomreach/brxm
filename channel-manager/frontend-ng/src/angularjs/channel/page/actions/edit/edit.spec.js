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

import angular from 'angular';
import 'angular-mocks';

describe('PageActionEdit', () => {
  let $q;
  let $scope;
  let $rootScope;
  let $compile;
  let $element;
  let $translate;
  let $mdDialog;
  let ChannelService;
  let FeedbackService;
  let HippoIframeService;
  let SiteMapService;
  let SiteMapItemService;
  let mockAlert;
  let siteMapItem;
  const pageModel = {
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

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$q_, _$rootScope_, _$compile_, _$translate_, _$mdDialog_, _ChannelService_, _FeedbackService_,
            _HippoIframeService_, _SiteMapService_, _SiteMapItemService_) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      $compile = _$compile_;
      $translate = _$translate_;
      $mdDialog = _$mdDialog_;
      ChannelService = _ChannelService_;
      FeedbackService = _FeedbackService_;
      HippoIframeService = _HippoIframeService_;
      SiteMapService = _SiteMapService_;
      SiteMapItemService = _SiteMapItemService_;
    });

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
    spyOn(FeedbackService, 'showErrorResponseOnSubpage');
    spyOn(HippoIframeService, 'reload');
    spyOn(SiteMapItemService, 'get').and.returnValue(siteMapItem);
    spyOn(SiteMapItemService, 'isEditable').and.returnValue(true);
    spyOn(SiteMapItemService, 'updateItem').and.returnValue($q.when());
    spyOn(SiteMapService, 'load');
  });

  function compileDirectiveAndGetController() {
    $scope = $rootScope.$new();
    $scope.onDone = jasmine.createSpy('onDone');
    $element = angular.element('<page-edit on-done="onDone()"> </page-edit>');
    $compile($element)($scope);
    $scope.$digest();

    return $element.controller('pageEdit');
  }

  it('initializes correctly', () => {
    let PageEditCtrl = compileDirectiveAndGetController();

    expect($translate.instant).toHaveBeenCalledWith('SUBPAGE_PAGE_EDIT_TITLE', { pageName: 'name' });
    expect($translate.instant).toHaveBeenCalledWith('SUBPAGE_PAGE_EDIT_PRIMARY_DOCUMENT_VALUE_NONE');
    expect(PageEditCtrl.title).toBe('title');
    expect(PageEditCtrl.availableDocuments.length).toBe(3);
    expect(PageEditCtrl.availableDocuments[0].path).toBe('');
    expect(PageEditCtrl.primaryDocument).toBe(PageEditCtrl.availableDocuments[0]);
    expect(PageEditCtrl.isAssigningNewTemplate).toBeFalsy();
    expect(ChannelService.getNewPageModel).toHaveBeenCalled();

    $rootScope.$digest();
    expect(PageEditCtrl.prototypes.length).toBe(2);

    // try again with different document settings
    siteMapItem.primaryDocumentRepresentation = { path: '/test/b' };
    siteMapItem.availableDocumentRepresentations.shift();
    PageEditCtrl = compileDirectiveAndGetController();
    expect(PageEditCtrl.primaryDocument).toBe(PageEditCtrl.availableDocuments[2]);

    siteMapItem.primaryDocumentRepresentation = { path: '/test/c' }; // no match, fallback to none-document
    siteMapItem.availableDocumentRepresentations.shift();
    PageEditCtrl = compileDirectiveAndGetController();
    expect(PageEditCtrl.primaryDocument).toBe(PageEditCtrl.availableDocuments[0]);

    delete siteMapItem.availableDocumentRepresentations;
    PageEditCtrl = compileDirectiveAndGetController();
    expect(PageEditCtrl.availableDocuments.length).toBe(1);
    expect(PageEditCtrl.primaryDocument).toBe(PageEditCtrl.availableDocuments[0]);

    siteMapItem.availableDocumentRepresentations = [];
    PageEditCtrl = compileDirectiveAndGetController();
    expect(PageEditCtrl.availableDocuments.length).toBe(1);
    expect(PageEditCtrl.primaryDocument).toBe(PageEditCtrl.availableDocuments[0]);
  });

  it('flashes a toast when the retrieval of the templates fails', () => {
    ChannelService.getNewPageModel.and.returnValue($q.reject());
    const PageEditCtrl = compileDirectiveAndGetController();
    $rootScope.$digest();

    expect(PageEditCtrl.prototypes).toEqual([]);
    expect(FeedbackService.showErrorResponseOnSubpage).toHaveBeenCalledWith(undefined, 'ERROR_PAGE_MODEL_RETRIEVAL_FAILED');
  });

  it('calls the callback when navigating back', () => {
    compileDirectiveAndGetController();

    $element.find('.qa-button-back').click();
    expect($scope.onDone).toHaveBeenCalled();
  });

  it('saves the new item values successfully', () => {
    const PageEditCtrl = compileDirectiveAndGetController();
    $rootScope.$digest();

    PageEditCtrl.title = 'newTitle';
    PageEditCtrl.primaryDocument = PageEditCtrl.availableDocuments[1];
    PageEditCtrl.isAssigningNewTemplate = true;
    PageEditCtrl.prototype = pageModel.prototypes[1];

    const savedItem = {
      id: 'siteMapItemId',
      parentId: null,
      name: 'name',
      pageTitle: 'newTitle',
      primaryDocumentRepresentation: PageEditCtrl.availableDocuments[1],
      componentConfigurationId: 'prototype-b',
    };

    PageEditCtrl.save();
    expect(SiteMapItemService.updateItem).toHaveBeenCalledWith(savedItem, 'siteMapId');
    $rootScope.$digest();

    expect(HippoIframeService.reload).toHaveBeenCalled();
    expect(SiteMapService.load).toHaveBeenCalledWith('siteMapId');
    expect(ChannelService.recordOwnChange).toHaveBeenCalled();
    expect($scope.onDone).toHaveBeenCalled();
  });

  it('tells the user that the page is already locked', () => {
    const response = { key: 'value' };
    SiteMapItemService.updateItem.and.returnValue($q.reject(response));
    const PageEditCtrl = compileDirectiveAndGetController();
    $rootScope.$digest();

    const savedItem = {
      id: 'siteMapItemId',
      parentId: null,
      name: 'name',
      pageTitle: 'title',
      primaryDocumentRepresentation: PageEditCtrl.availableDocuments[0],
    };

    PageEditCtrl.save();
    expect(SiteMapItemService.updateItem).toHaveBeenCalledWith(savedItem, 'siteMapId');
    $rootScope.$digest();

    expect(FeedbackService.showErrorResponseOnSubpage)
      .toHaveBeenCalledWith(response, 'ERROR_PAGE_SAVE_FAILED', PageEditCtrl.errorMap);
    expect($scope.onDone).not.toHaveBeenCalled();
  });

  it('flashes a toast when saving failed', () => {
    SiteMapItemService.updateItem.and.returnValue($q.reject());
    const PageEditCtrl = compileDirectiveAndGetController();
    $rootScope.$digest();

    PageEditCtrl.title = 'newTitle';
    PageEditCtrl.primaryDocument = PageEditCtrl.availableDocuments[0];

    const savedItem = {
      id: 'siteMapItemId',
      parentId: null,
      name: 'name',
      pageTitle: 'newTitle',
      primaryDocumentRepresentation: PageEditCtrl.availableDocuments[0],
    };

    PageEditCtrl.save();
    expect(SiteMapItemService.updateItem).toHaveBeenCalledWith(savedItem, 'siteMapId');
    $rootScope.$digest();

    expect(FeedbackService.showErrorResponseOnSubpage)
      .toHaveBeenCalledWith(undefined, 'ERROR_PAGE_SAVE_FAILED', PageEditCtrl.errorMap);
    expect($scope.onDone).not.toHaveBeenCalled();
  });

  it('checks if the channel has prototypes available', () => {
    let PageEditCtrl = compileDirectiveAndGetController();
    $rootScope.$digest();
    expect(PageEditCtrl.hasPrototypes()).toBe(true);

    siteMapItem.availableDocumentRepresentations.shift();
    ChannelService.getNewPageModel.and.returnValue($q.when({ prototypes: [] }));
    PageEditCtrl = compileDirectiveAndGetController();
    $rootScope.$digest();
    expect(PageEditCtrl.hasPrototypes()).toBe(false);

    siteMapItem.availableDocumentRepresentations.shift();
    ChannelService.getNewPageModel.and.returnValue($q.reject());
    PageEditCtrl = compileDirectiveAndGetController();
    $rootScope.$digest();
    expect(PageEditCtrl.hasPrototypes()).toBe(false);
  });

  it('shows an alert dialog when assigning a new template to a page with container items', () => {
    const PageEditCtrl = compileDirectiveAndGetController();
    $rootScope.$digest();

    siteMapItem.hasContainerItemInPageDefinition = true;
    PageEditCtrl.isAssigningNewTemplate = false;
    PageEditCtrl.evaluatePrototype();
    expect($mdDialog.show).not.toHaveBeenCalled();

    siteMapItem.hasContainerItemInPageDefinition = false;
    PageEditCtrl.isAssigningNewTemplate = false;
    PageEditCtrl.evaluatePrototype();
    expect($mdDialog.show).not.toHaveBeenCalled();

    siteMapItem.hasContainerItemInPageDefinition = false;
    PageEditCtrl.isAssigningNewTemplate = true;
    PageEditCtrl.evaluatePrototype();
    expect($mdDialog.show).not.toHaveBeenCalled();

    $mdDialog.show.calls.reset();
    mockAlert.textContent.calls.reset();
    siteMapItem.hasContainerItemInPageDefinition = true;
    PageEditCtrl.isAssigningNewTemplate = true;
    PageEditCtrl.prototype = pageModel.prototypes[0]; // has containers
    PageEditCtrl.evaluatePrototype();
    expect($mdDialog.show).toHaveBeenCalledWith(mockAlert);
    expect(mockAlert.textContent).toHaveBeenCalledWith('SUBPAGE_PAGE_EDIT_ALERT_CONTENT_REPOSITIONING');

    $mdDialog.show.calls.reset();
    mockAlert.textContent.calls.reset();
    siteMapItem.hasContainerItemInPageDefinition = true;
    PageEditCtrl.isAssigningNewTemplate = true;
    PageEditCtrl.prototype = pageModel.prototypes[1]; // has no
    PageEditCtrl.evaluatePrototype();
    expect($mdDialog.show).toHaveBeenCalledWith(mockAlert);
    expect(mockAlert.textContent).toHaveBeenCalledWith('SUBPAGE_PAGE_EDIT_ALERT_CONTENT_REMOVAL');
  });
});
