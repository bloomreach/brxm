/*!
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

import { DOCUMENT } from '@angular/common';
import { Inject, Injectable } from '@angular/core';

import { ClientAppService } from '../client-app/services/client-app.service';

import { BusyIndicatorService } from './busy-indicator.service';
import { GlobalSettingsService } from './global-settings.service';
import { NavConfigService } from './nav-config.service';

@Injectable({
  providedIn: 'root',
})
export class LogoutService {

  constructor(
    private busyIndicatorService: BusyIndicatorService,
    private clientAppService: ClientAppService,
    @Inject(DOCUMENT) private document: Document,
    private globalSettingsService: GlobalSettingsService,
    private navConfigService: NavConfigService,
  ) { }

  logout(loginMessageKey: string): void {
    this.busyIndicatorService.show();

    this.clientAppService.logoutApps()
      .then(() => this.navConfigService.logout())
      .finally(() => {
        this.busyIndicatorService.hide();
        const loginLocation = this.getLoginLocation(loginMessageKey);
        this.document.location.replace(loginLocation);
      });
  }

  private getLoginLocation(loginMessageKey: string): string {
    const queryParams = loginMessageKey && `/?loginmessage=${loginMessageKey}` || '/';
    const baseUrl = this.globalSettingsService.appSettings.navAppBaseURL;
    return `${baseUrl}${queryParams}`;
  }
}
