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

import { AppSettings } from './app-settings.dto';
import { AppSettingsMock } from './app-settings.mock';
import { GlobalSettings } from './global-settings.dto';
import { UserSettings } from './user-settings.dto';
import { UserSettingsMock } from './user-settings.mock';

export class GlobalSettingsMock implements GlobalSettings {
  userSettings = new UserSettingsMock() as UserSettings;
  appSettings = new AppSettingsMock() as AppSettings;
}
