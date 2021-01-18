/*
 * Copyright 2019-2021 Hippo B.V. (http://www.onehippo.com)
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

describe('dynamicDropdown', () => {
  let ContentService;
  let $ctrl;
  let $element;
  let $q;
  let $scope;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.rightSidePanel.contentEditor.fields');

    inject(($componentController, _$q_, $rootScope) => {
      $q = _$q_;
      ContentService = jasmine.createSpyObj('ContentService', ['getValueList']);

      $element = angular.element('<div/>');
      $scope = $rootScope.$new();
      $ctrl = $componentController('dynamicDropdown', {
        $element,
        $scope,
        ContentService,
      });
    });
  });

  describe('the none option', () => {
    it('is shown if showDefault is true and the field is not required', () => {
      $ctrl.showDefault = true;
      $ctrl.isRequired = false;
      expect($ctrl.showNone()).toBe(true);
    });

    it('is not shown if the field is required', () => {
      $ctrl.showDefault = true;
      $ctrl.isRequired = true;
      expect($ctrl.showNone()).toBe(false);
    });
  });

  describe('load options for dynamic dropdown field', () => {
    it('loads options correctly', () => {
      const valueList = [{ key: 's', label: 'Ship' }, { key: 'b', label: 'Boat' }];
      ContentService.getValueList.and.returnValue($q.resolve(valueList));

      $ctrl.$onInit();
      $scope.$apply();

      expect($ctrl.options).toEqual(valueList);
    });

    it('shows zero options when loading fails', () => {
      ContentService.getValueList.and.returnValue($q.reject());

      $ctrl.$onInit();
      $scope.$apply();

      expect($ctrl.options).toEqual([]);
    });
  });
});
