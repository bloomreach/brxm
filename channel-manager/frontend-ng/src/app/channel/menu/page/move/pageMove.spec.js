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

describe('PageMoveComponent', () => {
  let $compile;
  let $componentController;
  let $ctrl;
  let $q;
  let $rootScope;
  let $translate;
  let ChannelService;
  let FeedbackService;
  let HippoIframeService;
  let SiteMapService;
  let SiteMapItemService;
  let pageModel;
  let siteMapItem;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((
      _$compile_,
      _$componentController_,
      _$q_,
      _$rootScope_,
      _$translate_,
      _ChannelService_,
      _FeedbackService_,
      _HippoIframeService_,
      _SiteMapService_,
      _SiteMapItemService_,
    ) => {
      $compile = _$compile_;
      $componentController = _$componentController_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      $translate = _$translate_;
      ChannelService = _ChannelService_;
      FeedbackService = _FeedbackService_;
      HippoIframeService = _HippoIframeService_;
      SiteMapService = _SiteMapService_;
      SiteMapItemService = _SiteMapItemService_;
    });

    pageModel = {
      locations: [
        {
          id: null,
          location: 'localhost/',
        },
        {
          id: 'root-level',
          location: 'localhost/foo/',
        },
        {
          id: 'nested',
          location: 'localhost/foo/bar/',
        },
      ],
    };

    siteMapItem = {
      id: 'siteMapItemId',
      parentLocation: {
        id: null,
      },
      name: 'name',
    };

    spyOn($translate, 'instant').and.callFake(key => key);
    spyOn(ChannelService, 'getNewPageModel').and.returnValue($q.when(pageModel));
    spyOn(ChannelService, 'getSiteMapId').and.returnValue('siteMapId');
    spyOn(ChannelService, 'recordOwnChange');
    spyOn(FeedbackService, 'showErrorResponse');
    spyOn(HippoIframeService, 'load');
    spyOn(SiteMapItemService, 'get').and.returnValue(siteMapItem);
    spyOn(SiteMapItemService, 'isEditable').and.returnValue(true);
    spyOn(SiteMapItemService, 'updateItem');
    spyOn(SiteMapService, 'load');

    $ctrl = $componentController('pageMove', null, {
      onDone: jasmine.createSpy('onDone'),
    });
  });

  describe('$element tests', () => {
    let $element;
    let $scope;

    function compileComponentAndGetController() {
      $scope = $rootScope.$new();
      $scope.onDone = jasmine.createSpy('onDone');
      $element = angular.element('<page-move on-done="onDone()"> </page-move>');
      $compile($element)($scope);
      $scope.$digest();

      return $element.controller('pageMove');
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

    expect(ChannelService.getNewPageModel).toHaveBeenCalled();

    expect($ctrl.locations.length).toBe(3);
    expect($ctrl.location).toBe($ctrl.locations[0]);
    expect($ctrl.lastPathInfoElement).toBe('name');
    expect($translate.instant).toHaveBeenCalledWith('SUBPAGE_PAGE_MOVE_TITLE', { pageName: 'name' });
  });

  it('flashes a toast when the retrieval of the locations fails', () => {
    ChannelService.getNewPageModel.and.returnValue($q.reject());
    $ctrl.$onInit();
    $rootScope.$digest();

    expect($ctrl.locations).toEqual([]);
    expect($ctrl.location).toBeUndefined();
    expect(FeedbackService.showErrorResponse).toHaveBeenCalledWith(undefined, 'ERROR_PAGE_MODEL_RETRIEVAL_FAILED');
  });

  it('filters out unavailable locations', () => {
    siteMapItem.name = 'foo';
    $ctrl.$onInit();
    $rootScope.$digest();

    expect($ctrl.locations.length).toBe(1);
    expect($ctrl.locations[0].id).toBe(null);
    expect($ctrl.location).toBe($ctrl.locations[0]);

    siteMapItem.name = 'bar';
    siteMapItem.parentLocation = { id: 'root-level', location: 'localhost/foo/' };

    $ctrl.$onInit();
    $rootScope.$digest();

    expect($ctrl.locations.length).toBe(2);
    expect($ctrl.locations[0].id).toBe(null);
    expect($ctrl.locations[1].id).toBe('root-level');
    expect($ctrl.location).toBe($ctrl.locations[1]);
  });

  it('skips the filtering when the current page matches no location', () => {
    siteMapItem.parentLocation = { id: 'unknown', location: 'localhost/unknown/' };
    $ctrl.$onInit();
    $rootScope.$digest();

    expect($ctrl.locations.length).toBe(3);
    expect($ctrl.location).toBeUndefined();
  });

  it('gracefully deals with a lack of locations', () => {
    ChannelService.getNewPageModel.and.returnValue($q.when({}));
    $ctrl.$onInit();
    $rootScope.$digest();

    expect($ctrl.locations.length).toBe(0);
    expect($ctrl.location).toBeUndefined();
  });

  it('successfully moves the page', () => {
    const expectedItem = {
      id: 'siteMapItemId',
      parentId: 'nested',
      name: 'tobi',
    };
    const responseData = {
      renderPathInfo: '/abc/123',
    };
    SiteMapItemService.updateItem.and.returnValue($q.when(responseData));
    $ctrl.$onInit();
    $rootScope.$digest();

    $ctrl.location = pageModel.locations[2];
    $ctrl.lastPathInfoElement = 'tobi';
    $ctrl.move();

    expect(SiteMapItemService.updateItem).toHaveBeenCalledWith(expectedItem, 'siteMapId');
    $rootScope.$digest();

    expect(HippoIframeService.load).toHaveBeenCalledWith('/abc/123');
    expect(SiteMapService.load).toHaveBeenCalled();
    expect(ChannelService.recordOwnChange).toHaveBeenCalled();
    expect($ctrl.onDone).toHaveBeenCalled();
  });

  it('flashes a toast when moving of a page failed', () => {
    $ctrl.$onInit();
    $rootScope.$digest();

    SiteMapItemService.updateItem.and.returnValue($q.reject());
    $ctrl.move();
    $rootScope.$digest();

    expect(FeedbackService.showErrorResponse)
      .toHaveBeenCalledWith(undefined, 'ERROR_PAGE_MOVE_FAILED', $ctrl.errorMap);
    expect($ctrl.onDone).not.toHaveBeenCalled();
  });

  it('correctly dispatches the error from the server when trying to move a page', () => {
    $ctrl.$onInit();
    $rootScope.$digest();

    const response = { key: 'value' };
    SiteMapItemService.updateItem.and.returnValue($q.reject(response));
    $ctrl.move();
    $rootScope.$digest();
    expect(FeedbackService.showErrorResponse)
      .toHaveBeenCalledWith(response, 'ERROR_PAGE_MOVE_FAILED', $ctrl.errorMap);
  });
});
