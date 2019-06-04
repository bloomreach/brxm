/*
 * Copyright 2019 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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
import { NavAppSettings } from '../app/models';

// This file can be replaced during build by using the `fileReplacements` array.
// `ng build --prod` replaces `environment.ts` with `environment.prod.ts`.
// The list of file replacements can be found in `angular.json`.

const NavAppSettings: NavAppSettings = {
  userSettings: {
    userName: 'Frank Zappa',
    language: 'en',
    timeZone: 'Europe/Amsterdam',
  },
  appSettings: {
    navConfigResources: [
      {
        resourceType: 'IFRAME',
        url: 'http://localhost:4201',
      },
      {
        resourceType: 'REST',
        url: 'http://localhost:4201/assets/navitems.json',
      },
    ],
  },
};

export const environment = {
  production: false,
  NavAppSettings,
};

/*
 * For easier debugging in development mode, you can import the following file
 * to ignore zone related error stack frames such as `zone.run`, `zoneDelegate.invokeTask`.
 *
 * This import should be commented out in production mode because it will have a negative impact
 * on performance if an error is thrown.
 */
// import 'zone.js/dist/zone-error';  // Included with Angular CLI.
