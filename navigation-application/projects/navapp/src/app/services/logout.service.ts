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

import { Location } from '@angular/common';
import { Injectable } from '@angular/core';

import { ClientAppService } from '../client-app/services/client-app.service';
import { WindowRef } from '../shared/services/window-ref.service';

import { BusyIndicatorService } from './busy-indicator.service';
import { NavConfigService } from './nav-config.service';

@Injectable({
  providedIn: 'root',
})
export class LogoutService {

  constructor(
    private busyIndicatorService: BusyIndicatorService,
    private clientAppService: ClientAppService,
    private navConfigService: NavConfigService,
    private location: Location,
    private windowRef: WindowRef,
  ) { }

  logout(loginMessageKey: string): void {
    this.busyIndicatorService.show();

    this.clientAppService.logoutApps()
      .then(() => this.navConfigService.logout())
      .finally(() => {
        this.busyIndicatorService.hide();
        const loginLocation = this.getLoginLocation(loginMessageKey);
        this.windowRef.nativeWindow.location.replace(loginLocation);
      });
  }

  private getLoginLocation(loginMessageKey: string): string {
    const queryParams = loginMessageKey && `/?loginmessage=${loginMessageKey}` || '/';
    return this.location.prepareExternalUrl(queryParams);
  }
}
