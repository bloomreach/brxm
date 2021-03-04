/*
 * Copyright 2019-2021 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

import { DOCUMENT, Location } from '@angular/common';
import { Inject, Injectable } from '@angular/core';
import { ClientErrorCodes } from '@bloomreach/navapp-communication';
import { NGXLogger } from 'ngx-logger';
import { filter } from 'rxjs/operators';

import { ClientAppService } from '../client-app/services/client-app.service';
import { ErrorHandlingService } from '../error-handling/services/error-handling.service';
import { AppSettings } from '../models/dto/app-settings.dto';

import { APP_SETTINGS } from './app-settings';
import { BusyIndicatorService } from './busy-indicator.service';
import { ConnectionService } from './connection.service';
import { MainLoaderService } from './main-loader.service';
import { UserActivityService } from './user-activity.service';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  constructor(
    private readonly connectionService: ConnectionService,
    private readonly clientAppService: ClientAppService,
    private readonly mainLoaderService: MainLoaderService,
    private readonly busyIndicatorService: BusyIndicatorService,
    private readonly errorHandlingService: ErrorHandlingService,
    private readonly location: Location,
    private readonly logger: NGXLogger,
    @Inject(APP_SETTINGS) private readonly appSettings: AppSettings,
    @Inject(DOCUMENT) private readonly document: Document,
    userActivityService: UserActivityService,
  ) {
    this.connectionService
      .onError$
      .pipe(filter(error => error.errorCode === ClientErrorCodes.NotAuthorizedError))
      .subscribe(error => this.logout(error.errorCode.toString()));

    this.connectionService
      .onSessionExpired$
      .subscribe(() => this.logout('SessionExpired'));

    // User activity propagation prevents auto log out due to inactivity if a client app properly
    // handle this propagation
    userActivityService.startPropagation();
  }

  async loginAllResources(): Promise<void> {
    const loginResources = this.appSettings.loginResources || [];
    const loginPromises = loginResources.map(resource => {
      return this.connectionService
        .connect(resource.url)
        .catch(e => {
          this.logger.error(`Silent login has failed for '${resource.url}'`, e);

          return Promise.reject(e);
        })
        .finally(() => {
          this.connectionService.disconnect(resource.url);
        });
    });

    try {
      await Promise.all(loginPromises);
    } catch (error) {
      this.logout('SilentLoginFailed');
    }
  }

  async activeLogout(): Promise<void> {
    const beforeLogoutAppPromises = this.clientAppService.apps
      .filter(app => !!app.api.beforeLogout)
      .map(app => app.api.beforeLogout());
    await Promise.all(beforeLogoutAppPromises).then(
      () => this.logout('UserLoggedOut'),
    );
  }

  async logout(loginMessageKey: string): Promise<void> {
    this.mainLoaderService.show();
    this.busyIndicatorService.show();

    const logoutAppPromises = this.clientAppService.apps
      .filter(app => !!app.api.logout)
      .map(app => app.api.logout());
    const logoutResources = this.appSettings.logoutResources || [];
    const logoutResourcePromises = logoutResources.map(resource => this.connectionService.connect(resource.url));

    try {
      await Promise.all(logoutAppPromises);
      await Promise.all(logoutResourcePromises);

      this.redirectToLoginPage(loginMessageKey);
    } catch (e) {
      this.mainLoaderService.hide();
      this.busyIndicatorService.hide();

      this.errorHandlingService.setCriticalError('ERROR_UNABLE_TO_LOG_OUT', e.message);
    }
  }

  private getLoginLocation(loginMessageKey: string): string {
    const queryParams = loginMessageKey && `/?loginmessage=${loginMessageKey}` || '/';
    const baseUrl = this.appSettings.loginPath || this.appSettings.basePath;

    return this.location.prepareExternalUrl(`${baseUrl}${queryParams}`);
  }

  private redirectToLoginPage(loginMessageKey: string): void {
    const loginLocation = this.getLoginLocation(loginMessageKey);

    // A temporary solution related to complexity in notifying the navapp when the logout process is done
    // It's suggested that logout process is done in 1000 ms
    setTimeout(() => {
      this.document.location.replace(loginLocation);
    }, 1000);
  }
}
