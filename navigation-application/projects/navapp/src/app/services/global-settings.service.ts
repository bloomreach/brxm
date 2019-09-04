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

import { Injectable } from '@angular/core';

import { AppSettings } from '../models/dto/app-settings.dto';
import { GlobalSettings } from '../models/dto/global-settings.dto';
import { UserSettings } from '../models/dto/user-settings.dto';

@Injectable({
  providedIn: 'root',
})
export class GlobalSettingsService implements GlobalSettings {
  appSettings: AppSettings;
  userSettings: UserSettings;

  constructor() {
    const settings = (window as any).NavAppSettings;
    Object.assign(this, settings);

    this.appSettings.navAppBasePath = this.extractBasePath();

    if (!this.appSettings.iframesConnectionTimeout) {
      this.appSettings.iframesConnectionTimeout = 30000;
    }
  }

  private extractBasePath(): string {
    const initialPath = this.appSettings.initialPath || '/';
    const href = window.location.href;
    const fullPath = href.replace(window.location.origin, '');
    const index = fullPath.indexOf(initialPath);

    return fullPath.slice(0, index);
  }
}
