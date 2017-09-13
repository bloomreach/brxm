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

describe('stopPropagationDirective', () => {
  let $rootScope;
  let $compile;

  beforeEach(angular.mock.module('hippo-cm'));
  beforeEach(inject((_$rootScope_, _$compile_) => {
    $rootScope = _$rootScope_;
    $compile = _$compile_;
  }));

  it('stop event bubbling', () => {
    const $scope = $rootScope.$new();
    $scope.clickIt = () => {};
    const $element = angular.element('<a ng-click="clickIt()" stop-propagation>click</a>');
    $compile($element)($scope);
    $scope.$digest();

    const clickSpy = window.spyOnEvent($element, 'click');
    $element.click();
    expect('click').toHaveBeenStoppedOn($element);
    expect(clickSpy).toHaveBeenStopped();
  });
});
