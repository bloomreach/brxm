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

/* eslint-disable prefer-const */

describe('MenuEditor', () => {
  'use strict';

  let $q;
  let $element;
  let $scope;
  let $rootScope;
  let $compile;
  let SiteMenuService;
  let menu;

  beforeEach(() => {
    module('hippo-cm');

    inject((_$q_, _$rootScope_, _$compile_, _SiteMenuService_) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      $compile = _$compile_;
      SiteMenuService = _SiteMenuService_;
    });

    menu = { items: [] };

    spyOn(SiteMenuService, 'loadMenu').and.returnValue($q.when(menu));
  });

  function compileDirectiveAndGetController() {
    $scope = $rootScope.$new();
    $scope.onDone = jasmine.createSpy('onDone');
    $scope.onError = jasmine.createSpy('onError');
    $scope.menuUuid = 'testUuid';
    $element = angular.element('<menu-editor menu-uuid="{{menuUuid}}" on-done="onDone()" on-error="onError(key)"> </menu-editor>');
    $compile($element)($scope);
    $scope.$digest();

    return $element.controller('menu-editor');
  }

  it('initializes correctly', () => {
    const MenuEditorCtrl = compileDirectiveAndGetController();

    expect(MenuEditorCtrl.menuUuid).toBe('testUuid');
    expect(MenuEditorCtrl.items).toBe(menu.items);
  });

  it('returns to the main page when it fails to load the menu', () => {
    SiteMenuService.loadMenu.and.returnValue($q.reject());
    compileDirectiveAndGetController();

    expect($scope.onError).toHaveBeenCalledWith('ERROR_MENU_LOAD_FAILED');
  });

  it('returns to the page when clicking the "back" button', () => {
    compileDirectiveAndGetController();

    $element.find('.qa-button-back').click();
    expect($scope.onDone).toHaveBeenCalled();
  });
});

