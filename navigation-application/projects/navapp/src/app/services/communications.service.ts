/*
 * (C) Copyright 2019 Bloomreach. All rights reserved. (https://www.bloomreach.com)
 */

import { Injectable } from '@angular/core';
import {
  ChildConnectConfig,
  ChildPromisedApi,
  connectToChild,
  ParentApi,
} from '@bloomreach/navapp-communication';
import { from, Observable } from 'rxjs';

import { ClientApplicationsManagerService } from '../client-applications-manager/services';

import { OverlayService } from './overlay.service';

@Injectable({
  providedIn: 'root',
})
export class CommunicationsService {
  private connections: Map<string, Observable<ChildPromisedApi>> = new Map();

  constructor(
    private clientAppsManager: ClientApplicationsManagerService,
    private overlay: OverlayService,
  ) {}

  private get parentApiMethods(): ParentApi {
    return {
      showMask: () => this.overlay.enable(),
      hideMask: () => this.overlay.disable(),
    };
  }

  navigate(clientAppId: string, path: string): void {
    const handler = this.clientAppsManager.getApplicationHandler(clientAppId);

    this.createConnection(clientAppId, handler.iframeEl);

    this.getConnection(clientAppId).subscribe(api => {
      api.navigate({ path }).then(() => {
        this.clientAppsManager.activateApplication(clientAppId);
      });
    });
  }

  private createConnection(id: string, iframeEl: HTMLIFrameElement): void {
    if (this.connections.has(id)) {
      return;
    }

    const config: ChildConnectConfig = {
      iframe: iframeEl,
      methods: this.parentApiMethods,
    };

    const obs = from(connectToChild(config));
    this.connections.set(id, obs);
  }

  private getConnection(id: string): Observable<ChildPromisedApi> {
    if (!this.connections.has(id)) {
      throw new Error(`There is no connection to an ifrane with id = ${id}`);
    }

    return this.connections.get(id);
  }
}
