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

import { TestBed } from '@angular/core/testing';

import { GlobalSettingsService } from './global-settings.service';

describe('GlobalSettingService', () => {
  function setup(): {
    navAppSettingsService: GlobalSettingsService;
    userName: string;
  } {
    const userName = 'Frank Zappa';

    (window as any).NavAppSettings = {
      userSettings: {
        userName,
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

    TestBed.configureTestingModule({
      providers: [GlobalSettingsService],
    });

    return {
      navAppSettingsService: TestBed.get(GlobalSettingsService),
      userName,
    };
  }

  it('should take the settings object from the window', () => {
    const { userName, navAppSettingsService } = setup();

    expect(navAppSettingsService.userSettings.userName).toEqual(userName);
  });
});
