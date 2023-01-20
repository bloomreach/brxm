/*
 * Copyright 2021-2023 Bloomreach
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

import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class DateService {
  getCurrentDate(): Date {
    return new Date(Date.now());
  }

  getFutureDate(date: Date, days = 0, hours = 0, minutes = 0): Date {
    const dateCopy = new Date(date);
    dateCopy.setDate(dateCopy.getDate() + days);
    dateCopy.setHours(dateCopy.getHours() + hours);
    dateCopy.setMinutes(dateCopy.getMinutes() + minutes);
    return new Date(dateCopy);
  }
}
