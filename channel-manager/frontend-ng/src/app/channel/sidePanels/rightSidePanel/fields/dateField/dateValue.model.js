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

/**
 * The DateValue class supports displaying and setting date values correctly. For manipulation of the moment
 * in time moment.js is used. The md-datepicker works with a JavaScript Date object. Calculations with this
 * class are not consistent across browsers though.
 *
 * User time zone is taken into account to show the moment in time to the user as the moment in the users' time zone.
 */
class DateValue {
  constructor(dateString, userTimeZone, dateOnly = false) {
    this.editMinutes = false;
    this.userTimeZone = userTimeZone;
    this.dateOnly = dateOnly;

    if (dateString === '') {
      this._initBlank();
    } else {
      this._init(moment(dateString));
    }
  }

  _init(initMoment) {
    this.moment = initMoment;
    this.moment.seconds(0);
    this.moment.milliseconds(0);
    if (this.userTimeZone) {
      this.moment.tz(this.userTimeZone);
    }
    if (this.dateOnly) {
      this.moment.startOf('day');
    }
    this._initJsDate();
  }

  _initJsDate() {
    // use only the year, month and day of the moment and ignore the time and time zone
    this.jsDate = new Date(this.moment.format('YYYY-MM-DD'));
  }

  _initBlank() {
    this.moment = null;
    this.jsDate = null;
  }

  _initIfNeeded() {
    if (this.moment === null) {
      this._init(moment());
    }
  }

  get hours() {
    return this.moment ? this.moment.hours() : null;
  }

  set hours(hours) {
    const checkedHours = hours > 23 ? 23 : hours;
    this._initIfNeeded();
    this.moment.hours(checkedHours);
  }

  /**
   * @returns {*}
   */
  get minutes() {
    if (!this.moment) {
      return null;
    }
    const minutes = this.moment.minutes();
    if (minutes < 10 && this.editMinutes === false) {
      return `0${minutes}`; // 00 zero padding when viewing
    }
    return minutes;
  }

  set minutes(minutes) {
    this._initIfNeeded();
    this.moment.minutes(minutes);
  }

  get date() {
    return this.jsDate;
  }

  set date(date) {
    if (date) {
      this._initIfNeeded();
      this.moment.year(date.getFullYear());
      this.moment.month(date.getMonth());
      this.moment.date(date.getDate()); // day of the month
      this._initJsDate();
    } else {
      this._initBlank();
    }
  }

  toDateString() {
    return this.moment ? this.moment.format('YYYY-MM-DDTHH:mm:ss.SSSZ') : '';
  }

  setToNow() {
    this._init(moment());
  }

  focusMinutes() {
    this.editMinutes = true;
  }

  blurMinutes() {
    this.editMinutes = false;
  }
}

export default DateValue;
