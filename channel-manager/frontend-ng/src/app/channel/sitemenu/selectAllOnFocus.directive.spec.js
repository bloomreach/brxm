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
import { fakeAsync, tick } from '@angular/core/testing';

describe('selectAllOnFocusDirective', () => {
  let $rootScope;
  let $compile;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.sitemenu');

    inject((_$rootScope_, _$compile_) => {
      $rootScope = _$rootScope_;
      $compile = _$compile_;
    });
  });

  it('selects all content on focus', fakeAsync(() => {
    const $scope = $rootScope.$new();
    const $element = angular.element('<input select-all-on-focus value="foo bah"></input>');
    const valueLength = $element.val().length;
    $compile($element)($scope);
    $scope.$digest();

    $element.triggerHandler('focus');

    tick(1);

    expect($element[0].selectionStart).toEqual(0);
    expect($element[0].selectionEnd).toEqual(valueLength);
  }));
});
