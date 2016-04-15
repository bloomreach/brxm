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

describe('PageActions', () => {
  'use strict';

  let $rootScope;
  let $compile;
  let $translate;

  beforeEach(() => {
    module('hippo-cm');

    inject((_$rootScope_, _$compile_, _$translate_) => {
      $rootScope = _$rootScope_;
      $compile = _$compile_;
      $translate = _$translate_;
    });

    spyOn($translate, 'instant').and.callFake((key) => {
      if (key.startsWith('TOOLBAR_MENU_PAGES_')) {
        return key.replace(/^TOOLBAR_MENU_PAGES_/, '');
      }

      return key;
    });
  });

  function compileDirectiveAndGetController() {
    const $scope = $rootScope.$new();
    const $element = angular.element('<page-actions></page-actions>');
    $compile($element)($scope);
    $scope.$digest();

    return $element.controller('page-actions');
  }

  it('displays a menu with 5 disabled actions', () => {
    const PageActionsCtrl = compileDirectiveAndGetController();

    expect(PageActionsCtrl.actions.length).toBe(5);
    expect(PageActionsCtrl.actions[0].id).toBe('edit');
    expect(PageActionsCtrl.actions[0].label).toBe('EDIT');
    expect(PageActionsCtrl.actions[0].isEnabled).toBe(false);
    expect(PageActionsCtrl.actions[1].id).toBe('add');
    expect(PageActionsCtrl.actions[2].id).toBe('delete');
    expect(PageActionsCtrl.actions[3].id).toBe('move');
    expect(PageActionsCtrl.actions[4].id).toBe('copy');
  });
});
