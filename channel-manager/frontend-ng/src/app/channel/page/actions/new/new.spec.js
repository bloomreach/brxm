/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

describe('PageActionNew', () => {
  let $q;
  let $scope;
  let $rootScope;
  let $compile;
  let $element;
  let $translate;
  let ChannelService;
  let FeedbackService;
  let HippoIframeService;
  let SiteMapService;
  let pageModel;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

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
    spyOn(FeedbackService, 'showErrorResponse');
    spyOn(HippoIframeService, 'load');
    spyOn(SiteMapService, 'create').and.returnValue($q.when({ renderPathInfo: 'renderPathInfo' }));
    spyOn(SiteMapService, 'load');
  });

  function compileDirectiveAndGetController() {
    $scope = $rootScope.$new();
    $scope.onDone = jasmine.createSpy('onDone');
    $element = angular.element('<page-new on-done="onDone()"> </page-new>');
    $compile($element)($scope);
    $scope.$digest();

    return $element.controller('pageNew');
  }

  it('initializes correctly', () => {
    const $ctrl = compileDirectiveAndGetController();

    expect($ctrl.illegalCharacters).toBe('/ :');
    expect($ctrl.illegalCharactersMessage).toBe('Illegal Characters');
    expect($ctrl.siteMapId).toBe('siteMapId');
    expect($ctrl.updateLastPathInfoElementAutomatically).toBe(true);
    expect(ChannelService.getNewPageModel).toHaveBeenCalled();
    $rootScope.$digest();

    expect($ctrl.locations).toBe(pageModel.locations);
    expect($ctrl.location).toBe(pageModel.locations[0]);
    expect($ctrl.prototypes).toBe(pageModel.prototypes);
    expect($ctrl.prototype).toBe(pageModel.prototypes[0]);
  });

  it('updates the last pathinfo element as long as it is coupled to the title field', () => {
    const $ctrl = compileDirectiveAndGetController();
    $rootScope.$digest();

    expect($ctrl.title).toBeUndefined();
    expect($ctrl.lastPathInfoElement).toBe('');
    $ctrl.title = '/foo :bar:';
    $rootScope.$digest();
    expect($ctrl.lastPathInfoElement).toBe('-foo--bar-');

    $ctrl.disableAutomaticLastPathInfoElementUpdate();
    $ctrl.title = 'bar';
    $rootScope.$digest();
    expect($ctrl.lastPathInfoElement).toBe('-foo--bar-');
  });

  it('calls the callback when navigating back', () => {
    compileDirectiveAndGetController();

    $element.find('.qa-button-back').click();
    expect($scope.onDone).toHaveBeenCalled();
  });

  it('flashes a toast when retrieval of the new page model fails', () => {
    ChannelService.getNewPageModel.and.returnValue($q.reject());
    compileDirectiveAndGetController();
    $rootScope.$digest();

    expect(FeedbackService.showErrorResponse)
      .toHaveBeenCalledWith(undefined, 'ERROR_PAGE_MODEL_RETRIEVAL_FAILED');
  });

  it('gracefully handles absend prototypes or locations', () => {
    pageModel.prototypes = [];
    pageModel.locations = [];

    const $ctrl = compileDirectiveAndGetController();
    $rootScope.$digest();

    expect($ctrl.location).toBeUndefined();
    expect($ctrl.prototype).toBeUndefined();
  });

  it('successfully creates a new page', () => {
    const $ctrl = compileDirectiveAndGetController();
    $rootScope.$digest();

    $ctrl.title = 'title';
    $ctrl.lastPathInfoElement = 'lastPathInfoElement';
    $ctrl.create();

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
    const $ctrl = compileDirectiveAndGetController();
    $rootScope.$digest();

    $ctrl.title = 'title';
    $ctrl.lastPathInfoElement = 'lastPathInfoElement';
    $ctrl.location = pageModel.locations[1];
    $ctrl.prototype = pageModel.prototypes[1];
    $ctrl.create();

    expect(SiteMapService.create).toHaveBeenCalledWith('siteMapId', 'location-2', {
      pageTitle: 'title',
      name: 'lastPathInfoElement',
      componentConfigurationId: 'prototype-b',
    });
    $rootScope.$digest();

    expect(FeedbackService.showErrorResponse)
      .toHaveBeenCalledWith(undefined, 'ERROR_PAGE_CREATION_FAILED', $ctrl.errorMap);
  });

  it('correctly dispatches the error from the server when trying to create a new page', () => {
    const $ctrl = compileDirectiveAndGetController();
    $rootScope.$digest();

    const response = { key: 'value' };
    SiteMapService.create.and.returnValue($q.reject(response));
    $ctrl.create();
    $rootScope.$digest();
    expect(FeedbackService.showErrorResponse)
      .toHaveBeenCalledWith(response, 'ERROR_PAGE_CREATION_FAILED', $ctrl.errorMap);
  });
});
