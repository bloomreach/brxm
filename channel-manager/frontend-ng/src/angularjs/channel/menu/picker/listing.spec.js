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

describe('Listing', () => {
  'use strict';

  let $rootScope;
  let $compile;
  let $scope;

  beforeEach(() => {
    module('hippo-cm');

    inject((_$rootScope_, _$compile_) => {
      $rootScope = _$rootScope_;
      $compile = _$compile_;
    });
  });

  function compileDirectiveAndGetController() {
    $scope = $rootScope.$new();
    $scope.selectedItem = {};
    $scope.selectedDocument = {};
    const $element = angular.element('<div listing selected-item="selectedItem" selected-document="selectedDocument"></div>');
    $compile($element)($scope);
    $scope.$digest();
    return $element.controller('listing');
  }

  it('can select a document', () => {
    const ListingCtrl = compileDirectiveAndGetController();
    ListingCtrl.selectDocument('b');
    $rootScope.$digest();
    expect($scope.selectedDocument).toBe('b');
  });
});
