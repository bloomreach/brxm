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

describe('SubpageToolbar', () => {
  'use strict';

  let $element;
  let $scope;
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

    spyOn($translate, 'instant').and.callFake((key) => key);
  });

  function compileDirectiveAndGetController() {
    $scope = $rootScope.$new();
    $scope.onBack = jasmine.createSpy('onBack');
    $scope.title = 'testTitle';
    $element = angular.element('<subpage-toolbar title="{{title}}" on-back="onBack()"> </subpage-toolbar>');
    $compile($element)($scope);
    $scope.$digest();

    return $element.controller('subpage-toolbar');
  }

  it('displays the passed-in page title', () => {
    compileDirectiveAndGetController();

    expect($element.find('h2').text()).toBe('testTitle');
  });

  it('returns to the page when clicking the "back" button', () => {
    compileDirectiveAndGetController();

    $element.find('.qa-button-back').click();
    expect($scope.onBack).toHaveBeenCalled();
  });
});
