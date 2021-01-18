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

describe('radioGroup', () => {
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
      $ctrl = $componentController('radioGroup', {
        $element,
        $scope,
        ContentService,
      });
    });
  });

  describe('orientation', () => {
    it('is only vertical when config string is exactly equal to \'vertical\'', () => {
      $ctrl.orientation = 'vertical';
      expect($ctrl.isOrientationHorizontal()).toBe(false);
    });

    it('is horizontal when configuration is not set', () => {
      expect($ctrl.isOrientationHorizontal()).toBe(true);
    });

    it('is horizontal when configuration is not set to \'vertical\'', () => {
      ['Vertical', 'horizontal', 'blabla'].forEach((config) => {
        $ctrl.orientation = config;
        expect($ctrl.isOrientationHorizontal()).toBe(true);
      });
    });
  });

  describe('load options for radio group field', () => {
    it('loads button values correctly', () => {
      const valueList = [{ key: 's', label: 'Ship' }, { key: 'b', label: 'Boat' }];
      ContentService.getValueList.and.returnValue($q.resolve(valueList));

      $ctrl.$onInit();
      $scope.$apply();

      expect($ctrl.buttonValues()).toEqual(['s', 'b']);
    });

    it('loads button display values correctly', () => {
      const valueList = [{ key: 's', label: 'Ship' }, { key: 'b', label: 'Boat' }];
      ContentService.getValueList.and.returnValue($q.resolve(valueList));

      $ctrl.$onInit();
      $scope.$apply();

      expect($ctrl.buttonDisplayValues(0)).toEqual('Ship');
      expect($ctrl.buttonDisplayValues(1)).toEqual('Boat');
    });
  });

  describe('load options for boolean radio group field with configured labels', () => {
    beforeEach(() => {
      $ctrl.fieldType = 'BOOLEAN_RADIO_GROUP';
      $ctrl.falseLabel = 'Bad';
      $ctrl.trueLabel = 'Good';
    });

    it('loads button values correctly', () => {
      $ctrl.$onInit();
      $scope.$apply();

      expect($ctrl.buttonValues()).toEqual(['true', 'false']);
    });

    it('loads button display values correctly', () => {
      $ctrl.$onInit();
      $scope.$apply();

      expect($ctrl.buttonDisplayValues(0)).toEqual('Good');
      expect($ctrl.buttonDisplayValues(1)).toEqual('Bad');
    });
  });

  describe('load options for boolean radio group field with value list', () => {
    beforeEach(() => {
      $ctrl.fieldType = 'BOOLEAN_RADIO_GROUP';
      $ctrl.falseLabel = 'Bad'; // should be ignored when list is configured
      $ctrl.trueLabel = 'Good'; // should be ignored when list is configured
      $ctrl.optionsSource = '/path/to/list/is/configured';
    });

    it('loads button values correctly even if the list is too long', () => {
      const valueList = [
        { key: 'true', label: 'True' },
        { key: 'false', label: 'False' },
        { key: 'true', label: 'True Again' },
        { key: 'false', label: 'False Again' },
      ];
      ContentService.getValueList.and.returnValue($q.resolve(valueList));

      $ctrl.$onInit();
      $scope.$apply();

      expect($ctrl.buttonValues()).toEqual(['true', 'false']);
    });

    it('loads button values correctly with empty list', () => {
      const valueList = [];
      ContentService.getValueList.and.returnValue($q.resolve(valueList));

      $ctrl.$onInit();
      $scope.$apply();

      expect($ctrl.buttonValues()).toEqual(['true', 'false']);
    });

    it('loads button display values correctly even if the list is too long', () => {
      const valueList = [
        { key: 'true', label: 'True' },
        { key: 'false', label: 'False' },
        { key: 'true', label: 'True Again' },
        { key: 'false', label: 'False Again' },
      ];
      ContentService.getValueList.and.returnValue($q.resolve(valueList));

      $ctrl.$onInit();
      $scope.$apply();

      expect($ctrl.buttonDisplayValues(0)).toEqual('True');
      expect($ctrl.buttonDisplayValues(1)).toEqual('False');
    });

    it('loads default button display values correctly with empty list', () => {
      const valueList = [];
      ContentService.getValueList.and.returnValue($q.resolve(valueList));

      $ctrl.$onInit();
      $scope.$apply();

      expect($ctrl.buttonDisplayValues(0)).toEqual('true');
      expect($ctrl.buttonDisplayValues(1)).toEqual('false');
    });
  });
});
