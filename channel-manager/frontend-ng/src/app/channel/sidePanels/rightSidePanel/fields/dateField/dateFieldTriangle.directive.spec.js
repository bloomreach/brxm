/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

describe('DateFieldTriangleDirective', () => {
  let mdDatepicker;
  let mdInputContainer;
  let $compile;
  let $element;
  let $timeout;
  let $scope;

  beforeEach(() => angular.mock.module('hippo-cm'));

  beforeEach(inject((_$compile_, _$timeout_, _$rootScope_) => {
    $compile = _$compile_;
    $scope = _$rootScope_.$new();
    $timeout = _$timeout_;
  }));

  beforeEach(() => {
    $scope.value = new Date();
    $scope.fieldType = { type: 'DATE_ONLY' };

    const element = $compile(`
      <md-input-container>
        <date-field ng-model="value" field-type="fieldType">
          <md-datepicker></md-datepicker>
        </date-field>
      </md-input-container>
    `)($scope);
    $scope.$digest();

    mdDatepicker = element.find('md-datepicker').controller('mdDatepicker');
    mdInputContainer = element.controller('mdInputContainer');
    $element = element.find('.md-datepicker-triangle-button');

    spyOn(mdDatepicker, 'setFocused').and.callThrough();
    spyOn(mdInputContainer, 'setFocused').and.callThrough();
  });

  it('sets parent components focused', () => {
    $element.triggerHandler('focus');

    $timeout.flush();

    expect(mdDatepicker.setFocused).toHaveBeenCalledWith(true);
    expect(mdInputContainer.setFocused).toHaveBeenCalledWith(true);
  });

  it('blurs parent components', () => {
    $element.triggerHandler('blur');

    expect(mdDatepicker.setFocused).toHaveBeenCalledWith(false);
    expect(mdInputContainer.setFocused).toHaveBeenCalledWith(false);
  });
});
