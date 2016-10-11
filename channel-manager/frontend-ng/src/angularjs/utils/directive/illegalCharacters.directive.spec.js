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

describe('IllegalCharactersDirective', () => {
  let $rootScope;
  let $compile;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$rootScope_, _$compile_) => {
      $rootScope = _$rootScope_;
      $compile = _$compile_;
    });
  });

  function compileDirectiveAndGetScope(illegalCharacters) {
    const $scope = $rootScope.$new();
    $scope.illegalCharacters = illegalCharacters;
    const $element = angular.element('<form name="form">' +
      '<input name="field" ng-model="input" illegal-characters="{{illegalCharacters}}">' +
      '</form>');
    $compile($element)($scope);
    $scope.$digest();

    return $scope;
  }

  it('invalidates the field if invalid characters are input', () => {
    const $scope = compileDirectiveAndGetScope('?/');
    expect($scope.form.field.$error).toEqual({});

    $scope.input = 'What happens if we add an illegal character?';
    $rootScope.$digest();
    expect($scope.form.field.$error).toEqual({ illegalCharacters: true });

    $scope.input = 'all is well, there are no illegal characters in this string';
    $rootScope.$digest();
    expect($scope.form.field.$error).toEqual({});

    $scope.input = 'And a string with / another / illegal / character...';
    $rootScope.$digest();
    expect($scope.form.field.$error).toEqual({ illegalCharacters: true });
  });
});
