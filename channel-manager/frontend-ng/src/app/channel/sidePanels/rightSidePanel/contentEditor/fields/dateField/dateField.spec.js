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

import {DateValue} from "./dateField.controller";

describe('DateField', () => {
  let $componentController;
  let $ctrl;
  let ngModel;

  const fieldType = {id: 'field:type'};
  const fieldValue = {value: '2015-08-24T06:53:00.000Z'};

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$componentController_) => {
      $componentController = _$componentController_;
    });

    ngModel = jasmine.createSpyObj('ngModel', [
      '$setViewValue',
    ]);

    ngModel.$setViewValue.and.callFake((value) => {
      ngModel.$viewValue = value;
    });

    $ctrl = $componentController('dateField', {}, {
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
    const oldValue = new DateValue('2015-08-24T06:53:00.000Z');
    $ctrl.dateValue = new DateValue('2015-08-24T06:53:00.000Z');

    $ctrl.setToNow();

    expect($ctrl.valueChanged).toHaveBeenCalled();
    expect(oldValue.jsDate).toBeLessThan($ctrl.dateValue.date);
  });
});

describe('DateValue', () => {
  let dateValue;

  describe('when populated', () => {
    beforeEach(() => {
      dateValue = new DateValue('2018-03-12T12:01:50.041+01:00');
    });

    it('contains data', () => {
      expect(dateValue.date).not.toBe(null);
      expect(dateValue.hours).not.toBe(null);
      expect(dateValue.minutes).not.toBe(null);
      expect(dateValue.toDateString()).not.toBeEmpty();
    });

    it('with focus on minutes field gives minutes not zero padded', () => {
      dateValue.focusMinutes();
      expect(dateValue.minutes).toBe(1);
      dateValue.blurMinutes();
      expect(dateValue.minutes).toBe('01');
    });

    it('without focus on minutes field gives minutes zero padded', () => {
      expect(dateValue.minutes).toBe('01');
    });

    it('it keeps hours to a maximum of 23 when a higher number is set', () => {
      dateValue.hours = 24;
      expect(dateValue.hours).toBe(23);
    });

    it('when a new date is set, existing hours and minutes are maintained', () => {
      const newDate = new Date(2017, 2, 2, 0, 0, 0, 0); // date with hours and minutes = 0
      dateValue.date = newDate;
      expect(dateValue.hours).toBe(12);
      expect(dateValue.minutes).toBe('01');
    });
  });

  describe('when populated with time zone', () => {
    // Note that the day, month year part of the date are shown in the md-datepicker which uses the get/set date
    // methods. The fields for the hours and minutes part of the date are using their own getter/setters.

    it('shows the day of the month correctly across the dateline', () => {
      // in Amsterdam this is january 1 in 2019 at 0:30 (am)
      // in Londen this is december 31 in 2018 at 23:30
      const dateString = '2019-01-01T00:30:00.000+01:00';

      dateValue = new DateValue(dateString, 'Europe/London');
      expect(dateValue.date.getDate()).toBe(31);

      dateValue = new DateValue(dateString, 'Europe/Amsterdam');
      expect(dateValue.date.getDate()).toBe(1);
    });

    it('shows the month correctly across the dateline', () => {
      // in Amsterdam this is january 1 in 2019 at 0:30 (am)
      // in Londen this is december 31 in 2018 at 23:30
      const dateString = '2019-01-01T00:30:00.000+01:00';

      dateValue = new DateValue(dateString, 'Europe/London');
      expect(dateValue.date.getMonth()).toBe(11);

      dateValue = new DateValue(dateString, 'Europe/Amsterdam');
      expect(dateValue.date.getMonth()).toBe(0);
    });

    it('shows the year correctly across the dateline', () => {
      // in Amsterdam this is january 1 in 2019 at 0:30 (am)
      // in Londen this is december 31 in 2018 at 23:30
      const dateString = '2019-01-01T00:30:00.000+01:00';

      dateValue = new DateValue(dateString, 'Europe/London');
      expect(dateValue.date.getFullYear()).toBe(2018);

      dateValue = new DateValue(dateString, 'Europe/Amsterdam');
      expect(dateValue.date.getFullYear()).toBe(2019);
    });

    it('shows the hours correctly across the timezone', () => {
      // in Amsterdam this is january 1 in 2019 at 0:30 (am)
      // in Londen this is december 31 in 2018 at 23:30
      const dateString = '2019-01-01T00:30:00.000+01:00';

      dateValue = new DateValue(dateString, 'Europe/London');
      expect(dateValue.hours).toBe(23);

      dateValue = new DateValue(dateString, 'Europe/Amsterdam');
      expect(dateValue.hours).toBe(0);
    });

    it('shows the minutes correctly across the timezone', () => {
      // in Amsterdam this is january 1 in 2019 at 0:30 (am)
      // in Calcutta this is january 1 in 2019 at 5:00 (India timezone differs 5.5 hours with CET).
      const dateString = '2019-01-01T00:30:00.000+01:00';

      dateValue = new DateValue(dateString, 'Asia/Calcutta');
      expect(dateValue.minutes).toBe('00'); // string due to zero padding

      dateValue = new DateValue(dateString, 'Europe/Amsterdam');
      expect(dateValue.minutes).toBe(30);
    });
  });

  describe('when empty', () => {
    beforeEach(() => {
      dateValue = new DateValue('');
    });

    it('does not contain data', () => {
      expect(dateValue.date).toBe(null);
      expect(dateValue.hours).toBe(null);
      expect(dateValue.minutes).toBe(null);
      expect(dateValue.toDateString()).toBe('');
    });

    it('gets populated when set data is called', () => {
      dateValue.date = new Date();
      expect(dateValue.date).not.toBe(null);
    });

    it('gets populated when set hours is called', () => {
      dateValue.hours = 1;
      expect(dateValue.date).not.toBe(null);
    });

    it('sets hours to 23 if set with a higher number', () => {
      dateValue.hours = 24;
      expect(dateValue.hours).toBe(23);
    });

    it('gets populated when set minutes is called', () => {
      dateValue.minutes = 1;
      expect(dateValue.date).not.toBe(null);
    });
  });

});