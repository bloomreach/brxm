/*
 * Copyright 2018-2021 Hippo B.V. (http://www.onehippo.com)
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

import DateValue from './dateValue.model';

describe('DateField', () => {
  let $componentController;
  let $ctrl;
  let $element;
  let ngModel;

  const fieldType = { id: 'field:type' };
  const fieldValue = { value: '2015-08-24T06:53:00.000Z' };

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.rightSidePanel.fields.dateField');

    inject((_$componentController_) => {
      $componentController = _$componentController_;
    });

    ngModel = jasmine.createSpyObj('ngModel', [
      '$setViewValue',
    ]);

    ngModel.$setViewValue.and.callFake((value) => {
      ngModel.$viewValue = value;
    });

    $element = angular.element('<div/>');
    $ctrl = $componentController('dateField', { $element }, {
      fieldType,
      fieldValue,
      name: 'test-name',
      ngModel,
    });

    $ctrl.$onInit();
  });

  it('initializes the component', () => {
    expect($ctrl.fieldType).toBe(fieldType);
    expect($ctrl.fieldValue).toBe(fieldValue);
    expect($ctrl.name).toBe('test-name');
  });

  it('initializes the date value from ngModel', () => {
    ngModel.$viewValue = '2015-08-24T06:53:00.000Z';
    ngModel.$render();
    expect($ctrl.dateValue).toBeDefined();
  });

  it('sets the value of a date to the current date and time when set to now is called', () => {
    spyOn($ctrl, 'valueChanged');
    const oldValue = new DateValue(fieldValue.value);
    $ctrl.dateValue = new DateValue('2015-08-24T06:53:00.000Z');

    $ctrl.setToNow();

    expect($ctrl.valueChanged).toHaveBeenCalled();
    expect(oldValue.jsDate).toBeLessThan($ctrl.dateValue.date);
  });
});
