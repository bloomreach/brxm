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
import {
  ClientError, ClientErrorCodes, connectToChild, NavLocation, ParentApi, SiteId,
} from '@bloomreach/navapp-communication';

import { version } from '../../../../../package.json';
import { ClientAppService } from '../client-app/services/client-app.service';
import { Connection } from '../models/connection.model';
import { FailedConnection } from '../models/failed-connection.model';

import { BusyIndicatorService } from './busy-indicator.service';
import { GlobalSettingsService } from './global-settings.service';
import { LogoutService } from './logout.service';
import { NavigationService } from './navigation.service';
import { OverlayService } from './overlay.service';

@Injectable({
  providedIn: 'root',
})
export class CommunicationsService {
  constructor(
    private busyIndicatorService: BusyIndicatorService,
    private clientAppService: ClientAppService,
    private logoutService: LogoutService,
    private navigationService: NavigationService,
    private overlay: OverlayService,
    private settings: GlobalSettingsService,
  ) {
  }

  get parentApiMethods(): ParentApi {
    return {
      getConfig: () => ({
        apiVersion: version,
        userSettings: this.settings.userSettings,
      }),
      showMask: () => this.overlay.enable(),
      hideMask: () => this.overlay.disable(),
      navigate: (location: NavLocation) => this.navigationService.navigateByNavLocation(location),
      updateNavLocation: (location: NavLocation) => this.navigationService.updateByNavLocation(location),
      onError: (clientError: ClientError) => this.onClientError(clientError),
      onSessionExpired: () => this.logoutService.logout('SessionExpired'),
      onUserActivity: () => this.clientAppService.onUserActivity(),
    };
  }

  updateSelectedSite(siteId: SiteId): Promise<void> {
    this.busyIndicatorService.show();

    return this.clientAppService.activeApp.api.updateSelectedSite(siteId).then(() => {
      const updatePromises = this.clientAppService.apps
        .filter(app => app.api && app.api.updateSelectedSite && app !== this.clientAppService.activeApp)
        .map(app => app.api.updateSelectedSite());

      return Promise.all(updatePromises).then(() => this.busyIndicatorService.hide());
    });
  }

  connectToChild(iframe: HTMLIFrameElement): Promise<void> {
    const appUrl = iframe.src;

    return connectToChild({
      iframe,
      methods: this.parentApiMethods,
      timeout: this.settings.appSettings.iframesConnectionTimeout,
    }).then(
      api => this.clientAppService.addConnection(new Connection(appUrl, api)),
      error => this.clientAppService.addConnection(new FailedConnection(appUrl, error)),
    );
  }

  private onClientError(clientError: ClientError): void {
    // TODO delegate to errorService
    const errorCode = clientError.errorCode;
    switch (errorCode) {
      case ClientErrorCodes.NotAuthorizedError:
        this.logoutService.logout(errorCode.toString());
        break;
      case ClientErrorCodes.UnknownError:
      case ClientErrorCodes.GenericCommunicationError:
      case ClientErrorCodes.PageNotFoundError:
      case ClientErrorCodes.InternalError:
      case ClientErrorCodes.UnableToConnectToServerError:
        console.log(`Client error: code = ${ClientErrorCodes[clientError.errorCode]}, message = ${clientError.message}`);
        break;
    }
  }
}
