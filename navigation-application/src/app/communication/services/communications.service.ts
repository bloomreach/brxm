/*
 * (C) Copyright 2019 Bloomreach. All rights reserved. (https://www.bloomreach.com)
 */

import { Injectable } from '@angular/core';

import { ClientApplicationsManagerService } from '../../client-applications-manager/services';

@Injectable()
export class CommunicationsService {
  constructor(private clientAppsManager: ClientApplicationsManagerService) {}

  navigate(clientAppId: string, path: string): void {
    const handler = this.clientAppsManager.getApplicationHandler(clientAppId);

    alert(`Sending a command navigate('${path}') to the app with id = ${handler.url} and iframe's url ${handler.iframeEl.src}`);
    this.clientAppsManager.activateApplication(clientAppId);
    // commLib.navigate(window, path)
  }
}
