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

describe('PageActionAdd', () => {
  'use strict';

  let $q;
  let $scope;
  let $rootScope;
  let $compile;
  let $translate;
  let ChannelService;
  let FeedbackService;
  let HippoIframeService;
  let SiteMapService;
  let pageModel;

  beforeEach(() => {
    module('hippo-cm');

    inject((_$q_, _$rootScope_, _$compile_, _$translate_, _ChannelService_, _FeedbackService_, _HippoIframeService_,
            _SiteMapService_) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      $compile = _$compile_;
      $translate = _$translate_;
      ChannelService = _ChannelService_;
      FeedbackService = _FeedbackService_;
      HippoIframeService = _HippoIframeService_;
      SiteMapService = _SiteMapService_;
    });

    spyOn($translate, 'instant').and.callFake((key) => {
      if (key === 'VALIDATION_ILLEGAL_CHARACTERS') {
        return 'Illegal Characters';
      }

      return key;
    });

    pageModel = {
      prototypes: [
        { id: 'prototype-a', displayName: 'Prototype A' },
        { id: 'prototype-b', displayName: 'Prototype B' },
      ],
      locations: [
        { id: null, location: 'www.test.com/' },
        { id: 'location-2', location: 'www.test.com/path/' },
      ],
    };

    spyOn(ChannelService, 'getNewPageModel').and.returnValue($q.when(pageModel));
    spyOn(ChannelService, 'getSiteMapId').and.returnValue('siteMapId');
    spyOn(ChannelService, 'recordOwnChange');
    spyOn(FeedbackService, 'showError');
    spyOn(HippoIframeService, 'load');
    spyOn(SiteMapService, 'create').and.returnValue($q.when({ renderPathInfo: 'renderPathInfo' }));
    spyOn(SiteMapService, 'load');
  });

  function compileDirectiveAndGetController() {
    $scope = $rootScope.$new();
    $scope.onDone = jasmine.createSpy('onDone');
    const $element = angular.element('<page-add on-done="onDone()"> </page-add>');
    $compile($element)($scope);
    $scope.$digest();

    return $element.controller('page-add');
  }

  it('initializes correctly', () => {
    const PageAddCtrl = compileDirectiveAndGetController();

    expect(PageAddCtrl.feedbackParent.length).toBe(1);
    expect(PageAddCtrl.illegalCharacters).toBe('/ :');
    expect(PageAddCtrl.illegalCharactersMessage).toBe('Illegal Characters');
    expect(PageAddCtrl.siteMapId).toBe('siteMapId');
    expect(PageAddCtrl.updateLastPathInfoElementAutomatically).toBe(true);
    expect(ChannelService.getNewPageModel).toHaveBeenCalled();
    $rootScope.$digest();

    expect(PageAddCtrl.locations).toBe(pageModel.locations);
    expect(PageAddCtrl.location).toBe(pageModel.locations[0]);
    expect(PageAddCtrl.prototypes).toBe(pageModel.prototypes);
    expect(PageAddCtrl.prototype).toBe(pageModel.prototypes[0]);
  });

  it('updates the last pathinfo element as long as it is coupled to the title field', () => {
    const PageAddCtrl = compileDirectiveAndGetController();
    $rootScope.$digest();

    expect(PageAddCtrl.title).toBeUndefined();
    expect(PageAddCtrl.lastPathInfoElement).toBe('');
    PageAddCtrl.title = '/foo :bar:';
    $rootScope.$digest();
    expect(PageAddCtrl.lastPathInfoElement).toBe('-foo--bar-');

    PageAddCtrl.disableAutomaticLastPathInfoElementUpdate();
    PageAddCtrl.title = 'bar';
    $rootScope.$digest();
    expect(PageAddCtrl.lastPathInfoElement).toBe('-foo--bar-');
  });

  it('calls the callback when navigating back', () => {
    const PageAddCtrl = compileDirectiveAndGetController();
    $rootScope.$digest();

    PageAddCtrl.back();
    expect($scope.onDone).toHaveBeenCalled();
  });

  it('flashes a toast when retrieval of the new page model fails', () => {
    ChannelService.getNewPageModel.and.returnValue($q.reject());
    const PageAddCtrl = compileDirectiveAndGetController();
    $rootScope.$digest();

    expect(FeedbackService.showError).toHaveBeenCalledWith('SUBPAGE_PAGE_ADD_ERROR_MODEL_RETRIEVAL_FAILED',
                                                           undefined, PageAddCtrl.feedbackParent);
  });

  it('gracefully handles absend prototypes or locations', () => {
    pageModel.prototypes = [];
    pageModel.locations = [];

    const PageAddCtrl = compileDirectiveAndGetController();
    $rootScope.$digest();

    expect(PageAddCtrl.location).toBeUndefined();
    expect(PageAddCtrl.prototype).toBeUndefined();
  });

  it('successfully creates a new page', () => {
    const PageAddCtrl = compileDirectiveAndGetController();
    $rootScope.$digest();

    PageAddCtrl.title = 'title';
    PageAddCtrl.lastPathInfoElement = 'lastPathInfoElement';
    PageAddCtrl.create();

    expect(SiteMapService.create).toHaveBeenCalledWith('siteMapId', undefined, {
      pageTitle: 'title',
      name: 'lastPathInfoElement',
      componentConfigurationId: 'prototype-a',
    });
    $rootScope.$digest();

    expect(HippoIframeService.load).toHaveBeenCalledWith('renderPathInfo');
    expect(SiteMapService.load).toHaveBeenCalledWith('siteMapId');
    expect(ChannelService.recordOwnChange).toHaveBeenCalled();
    expect($scope.onDone).toHaveBeenCalled();
  });

  it('flashes a toast when failing to create a new page with a parent sitemap item id', () => {
    SiteMapService.create.and.returnValue($q.reject());
    const PageAddCtrl = compileDirectiveAndGetController();
    $rootScope.$digest();

    PageAddCtrl.title = 'title';
    PageAddCtrl.lastPathInfoElement = 'lastPathInfoElement';
    PageAddCtrl.location = pageModel.locations[1];
    PageAddCtrl.prototype = pageModel.prototypes[1];
    PageAddCtrl.create();

    expect(SiteMapService.create).toHaveBeenCalledWith('siteMapId', 'location-2', {
      pageTitle: 'title',
      name: 'lastPathInfoElement',
      componentConfigurationId: 'prototype-b',
    });
    $rootScope.$digest();

    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_PAGE_ADD_FAILED',
                                                           undefined, PageAddCtrl.feedbackParent);
  });

  it('correctly dispatches the error from the server when trying to create a new page', () => {
    const PageAddCtrl = compileDirectiveAndGetController();
    $rootScope.$digest();

    const lockedError = {
      errorCode: 'ITEM_ALREADY_LOCKED',
      data: { lockedBy: 'tobi' },
    };
    SiteMapService.create.and.returnValue($q.reject(lockedError));
    PageAddCtrl.create();
    $rootScope.$digest();
    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_PAGE_LOCKED_BY', lockedError.data,
                                                           PageAddCtrl.feedbackParent);

    SiteMapService.create.and.returnValue($q.reject({ errorCode: 'ITEM_NOT_IN_PREVIEW' }));
    PageAddCtrl.create();
    $rootScope.$digest();
    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_PAGE_PARENT_MISSING', undefined,
                                                           PageAddCtrl.feedbackParent);

    SiteMapService.create.and.returnValue($q.reject({ errorCode: 'ITEM_NAME_NOT_UNIQUE' }));
    PageAddCtrl.create();
    $rootScope.$digest();
    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_PAGE_PATH_EXISTS', undefined,
                                                           PageAddCtrl.feedbackParent);

    SiteMapService.create.and.returnValue($q.reject({ errorCode: 'INVALID_PATH_INFO' }));
    PageAddCtrl.create();
    $rootScope.$digest();
    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_PAGE_PATH_INVALID', undefined,
                                                           PageAddCtrl.feedbackParent);
  });
});
