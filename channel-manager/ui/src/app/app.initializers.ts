/*!
 * Copyright 2021 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
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

import { NgxMatDateAdapter } from '@angular-material-components/datetime-picker';
import { NgxMatMomentAdapter } from '@angular-material-components/moment-adapter';
import moment from 'moment';
import 'moment-timezone';

import { Ng1NavappService } from './services/ng1/navapp.ng1.service';

export function initializer(
  navapp: Ng1NavappService,
  dateAdapter: NgxMatDateAdapter<NgxMatMomentAdapter>,
): () => Promise<void> {
  return async () => {
    // ensure connection with navapp
    await navapp.connect();
    const { language = 'en', timeZone } = await navapp.getUserSettings();

    moment.tz.setDefault(timeZone);
    dateAdapter.setLocale(language);

    // add user activity listener
    document.body.addEventListener('click', () => navapp.onUserActivity());
  };
}
