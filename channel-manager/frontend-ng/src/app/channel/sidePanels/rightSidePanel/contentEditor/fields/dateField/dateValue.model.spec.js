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

import moment from 'moment-timezone';
import DateValue from './dateValue.model';

describe('DateValue', () => {
  let dateValue;

  describe('when populated', () => {
    beforeEach(() => {
      dateValue = new DateValue('2018-03-12T08:01:50.041+01:00');
    });

    it('contains data', () => {
      expect(dateValue.date).not.toBe(null);
      expect(dateValue.hours).not.toBe(null);
      expect(dateValue.minutes).not.toBe(null);
      expect(dateValue.toDateString()).not.toBeEmpty();
    });

    it('sets seconds and milliseconds to zero, even if they are part of the input value', () => {
      expect(dateValue.moment.seconds()).toBe(0);
      expect(dateValue.moment.milliseconds()).toBe(0);
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

    it('does not change hours or minutes values when changing the date', () => {
      const hoursBefore = dateValue.hours;
      const minutesBefore = dateValue.minutes;
      const newDate = new Date(2017, 2, 2, 0, 0, 0, 0); // date with hours and minutes = 0
      dateValue.date = newDate;
      expect(dateValue.hours).toBe(hoursBefore);
      expect(dateValue.minutes).toBe(minutesBefore);
    });

    it('does not change date or minutes values when changing the hours', () => {
      const minutesBefore = dateValue.minutes;
      const yearBefore = dateValue.date.getFullYear();
      const monthBefore = dateValue.date.getMonth();
      const dayBefore = dateValue.date.getDate();
      dateValue.hours = 3;
      expect(dateValue.minutes).toBe(minutesBefore);
      expect(dateValue.date.getFullYear()).toBe(yearBefore);
      expect(dateValue.date.getMonth()).toBe(monthBefore);
      expect(dateValue.date.getDate()).toBe(dayBefore);
    });

    it('does not change date or hours values when changing the minutes', () => {
      const hoursBefore = dateValue.hours;
      const yearBefore = dateValue.date.getFullYear();
      const monthBefore = dateValue.date.getMonth();
      const dayBefore = dateValue.date.getDate();
      dateValue.minutes = 59;
      expect(dateValue.hours).toBe(hoursBefore);
      expect(dateValue.date.getFullYear()).toBe(yearBefore);
      expect(dateValue.date.getMonth()).toBe(monthBefore);
      expect(dateValue.date.getDate()).toBe(dayBefore);
    });

    it('initializes the JavaScript date correctly for a non English locale', () => {
      const dateValueNew = new DateValue('');
      const germanMoment = moment().locale('de');
      dateValueNew._init(germanMoment);
      expect(dateValueNew.jsDate.getTime()).not.toBeNaN();
    });

    describe('when populated for dateOnly', () => {
      beforeEach(() => {
        dateValue = new DateValue('2018-03-12T08:01:50.041+01:00', true);
      });

      it('sets hours and minutes to zero, even if they are part of the input value', () => {
        expect(dateValue.moment.hours()).toBe(0);
        expect(dateValue.moment.minutes()).toBe(0);
      });
    });
  });

  describe('when populated with time zone', () => {
    // Note that the day, month year part of the date are shown in the md-datepicker which uses the get/set date
    // methods. The fields for the hours and minutes part of the date are using their own getter/setters.

    it('shows the day of the month correctly across the dateline', () => {
      // in Amsterdam this is January 1 in 2019 at 0:30 (am)
      // in London this is December 31 in 2018 at 23:30
      const dateString = '2019-01-01T00:30:00.000+01:00';

      dateValue = new DateValue(dateString, false, 'Europe/London');
      expect(dateValue.date.getDate()).toBe(31);

      dateValue = new DateValue(dateString, false, 'Europe/Amsterdam');
      expect(dateValue.date.getDate()).toBe(1);
    });

    it('shows the month correctly across the dateline', () => {
      // in Amsterdam this is January 1 in 2019 at 0:30 (am)
      // in London this is December 31 in 2018 at 23:30
      const dateString = '2019-01-01T00:30:00.000+01:00';

      dateValue = new DateValue(dateString, false, 'Europe/London');
      expect(dateValue.date.getMonth()).toBe(11);

      dateValue = new DateValue(dateString, false, 'Europe/Amsterdam');
      expect(dateValue.date.getMonth()).toBe(0);
    });

    it('shows the year correctly across the dateline', () => {
      // in Amsterdam this is January 1 in 2019 at 0:30 (am)
      // in London this is December 31 in 2018 at 23:30
      const dateString = '2019-01-01T00:30:00.000+01:00';

      dateValue = new DateValue(dateString, false, 'Europe/London');
      expect(dateValue.date.getFullYear()).toBe(2018);

      dateValue = new DateValue(dateString, false, 'Europe/Amsterdam');
      expect(dateValue.date.getFullYear()).toBe(2019);
    });

    it('shows the hours correctly across the timezone', () => {
      // in Amsterdam this is January 1 in 2019 at 0:30 (am)
      // in London this is December 31 in 2018 at 23:30
      const dateString = '2019-01-01T00:30:00.000+01:00';

      dateValue = new DateValue(dateString, false, 'Europe/London');
      expect(dateValue.hours).toBe(23);

      dateValue = new DateValue(dateString, false, 'Europe/Amsterdam');
      expect(dateValue.hours).toBe(0);
    });

    it('shows the minutes correctly across the timezone', () => {
      // in Amsterdam this is January 1 in 2019 at 0:30 (am)
      // in Calcutta this is January 1 in 2019 at 5:00 (India timezone differs 5.5 hours with CET).
      const dateString = '2019-01-01T00:30:00.000+01:00';

      dateValue = new DateValue(dateString, false, 'Asia/Calcutta');
      expect(dateValue.minutes).toBe('00'); // string due to zero padding

      dateValue = new DateValue(dateString, false, 'Europe/Amsterdam');
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

    it('sets seconds and milliseconds to zero when a new date is initialized', () => {
      dateValue.setToNow();
      expect(dateValue.moment.seconds()).toBe(0);
      expect(dateValue.moment.milliseconds()).toBe(0);
    });

    describe('when empty for dateOnly', () => {
      beforeEach(() => {
        dateValue = new DateValue('', true);
      });

      it('sets hours, minutes, seconds and milliseconds to zero when a new day is initialized', () => {
        dateValue.setToNow();
        expect(dateValue.moment.hours()).toBe(0);
        expect(dateValue.moment.minutes()).toBe(0);
        expect(dateValue.moment.seconds()).toBe(0);
        expect(dateValue.moment.milliseconds()).toBe(0);
      });

      it('keeps hours and minutes to zero when the date is set', () => {
        dateValue.date = new Date();
        expect(dateValue.moment.hours()).toBe(0);
        expect(dateValue.moment.minutes()).toBe(0);
      })
    });
  });
});
