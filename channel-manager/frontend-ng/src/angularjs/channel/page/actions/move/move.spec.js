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

describe('PageActionMove', () => {
  let $element;
  let $q;
  let $scope;
  let $rootScope;
  let $compile;
  let $translate;
  let ChannelService;
  let FeedbackService;
  let HippoIframeService;
  let SiteMapService;
  let SiteMapItemService;
  let siteMapItem;
  const pageModel = {
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

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$q_, _$rootScope_, _$compile_, _$translate_, _ChannelService_, _FeedbackService_,
            _HippoIframeService_, _SiteMapService_, _SiteMapItemService_) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      $compile = _$compile_;
      $translate = _$translate_;
      ChannelService = _ChannelService_;
      FeedbackService = _FeedbackService_;
      HippoIframeService = _HippoIframeService_;
      SiteMapService = _SiteMapService_;
      SiteMapItemService = _SiteMapItemService_;
    });

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
    spyOn(FeedbackService, 'showErrorResponseOnSubpage');
    spyOn(HippoIframeService, 'load');
    spyOn(SiteMapItemService, 'get').and.returnValue(siteMapItem);
    spyOn(SiteMapItemService, 'isEditable').and.returnValue(true);
    spyOn(SiteMapItemService, 'updateItem');
    spyOn(SiteMapService, 'load');
  });

  function compileDirectiveAndGetController() {
    $scope = $rootScope.$new();
    $scope.onDone = jasmine.createSpy('onDone');
    $element = angular.element('<page-move on-done="onDone()"> </page-move>');
    $compile($element)($scope);
    $scope.$digest();

    return $element.controller('pageMove');
  }

  it('initializes correctly', () => {
    const PageMoveCtrl = compileDirectiveAndGetController();
    expect(ChannelService.getNewPageModel).toHaveBeenCalled();
    $rootScope.$digest();

    expect(PageMoveCtrl.locations.length).toBe(3);
    expect(PageMoveCtrl.location).toBe(PageMoveCtrl.locations[0]);
    expect(PageMoveCtrl.lastPathInfoElement).toBe('name');
    expect($translate.instant).toHaveBeenCalledWith('SUBPAGE_PAGE_MOVE_TITLE', { pageName: 'name' });
  });

  it('flashes a toast when the retrieval of the locations fails', () => {
    ChannelService.getNewPageModel.and.returnValue($q.reject());
    const PageMoveCtrl = compileDirectiveAndGetController();
    $rootScope.$digest();

    expect(PageMoveCtrl.locations).toEqual([]);
    expect(PageMoveCtrl.location).toBeUndefined();
    expect(FeedbackService.showErrorResponseOnSubpage).toHaveBeenCalledWith(undefined, 'ERROR_PAGE_MODEL_RETRIEVAL_FAILED');
  });

  it('filters out unavailable locations', () => {
    siteMapItem.name = 'foo';
    let PageMoveCtrl = compileDirectiveAndGetController();
    $rootScope.$digest();

    expect(PageMoveCtrl.locations.length).toBe(1);
    expect(PageMoveCtrl.locations[0].id).toBe(null);
    expect(PageMoveCtrl.location).toBe(PageMoveCtrl.locations[0]);

    siteMapItem.name = 'bar';
    siteMapItem.parentLocation = { id: 'root-level', location: 'localhost/foo/' };
    PageMoveCtrl = compileDirectiveAndGetController();
    $rootScope.$digest();

    expect(PageMoveCtrl.locations.length).toBe(2);
    expect(PageMoveCtrl.locations[0].id).toBe(null);
    expect(PageMoveCtrl.locations[1].id).toBe('root-level');
    expect(PageMoveCtrl.location).toBe(PageMoveCtrl.locations[1]);
  });

  it('skips the filtering when the current page matches no location', () => {
    siteMapItem.parentLocation = { id: 'unknown', location: 'localhost/unknown/' };
    const PageMoveCtrl = compileDirectiveAndGetController();
    $rootScope.$digest();

    expect(PageMoveCtrl.locations.length).toBe(3);
    expect(PageMoveCtrl.location).toBeUndefined();
  });

  it('gracefully deals with a lack of locations', () => {
    ChannelService.getNewPageModel.and.returnValue($q.when({}));
    const PageMoveCtrl = compileDirectiveAndGetController();
    $rootScope.$digest();

    expect(PageMoveCtrl.locations.length).toBe(0);
    expect(PageMoveCtrl.location).toBeUndefined();
  });

  it('calls the callback when navigating back', () => {
    compileDirectiveAndGetController();

    $element.find('.qa-button-back').click();
    expect($scope.onDone).toHaveBeenCalled();
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
    const PageMoveCtrl = compileDirectiveAndGetController();
    $rootScope.$digest();

    PageMoveCtrl.location = pageModel.locations[2];
    PageMoveCtrl.lastPathInfoElement = 'tobi';
    PageMoveCtrl.move();

    expect(SiteMapItemService.updateItem).toHaveBeenCalledWith(expectedItem, 'siteMapId');
    $rootScope.$digest();

    expect(HippoIframeService.load).toHaveBeenCalledWith('/abc/123');
    expect(SiteMapService.load).toHaveBeenCalled();
    expect(ChannelService.recordOwnChange).toHaveBeenCalled();
    expect($scope.onDone).toHaveBeenCalled();
  });

  it('flashes a toast when moving of a page failed', () => {
    const PageMoveCtrl = compileDirectiveAndGetController();
    $rootScope.$digest();

    SiteMapItemService.updateItem.and.returnValue($q.reject());
    PageMoveCtrl.move();
    $rootScope.$digest();

    expect(FeedbackService.showErrorResponseOnSubpage)
      .toHaveBeenCalledWith(undefined, 'ERROR_PAGE_MOVE_FAILED', PageMoveCtrl.errorMap);
    expect($scope.onDone).not.toHaveBeenCalled();
  });

  it('correctly dispatches the error from the server when trying to move a page', () => {
    const PageMoveCtrl = compileDirectiveAndGetController();
    $rootScope.$digest();

    const response = { key: 'value' };
    SiteMapItemService.updateItem.and.returnValue($q.reject(response));
    PageMoveCtrl.move();
    $rootScope.$digest();
    expect(FeedbackService.showErrorResponseOnSubpage)
      .toHaveBeenCalledWith(response, 'ERROR_PAGE_MOVE_FAILED', PageMoveCtrl.errorMap);
  });
});
