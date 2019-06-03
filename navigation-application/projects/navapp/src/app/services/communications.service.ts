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
  ChildConnectConfig,
  ChildPromisedApi,
  connectToChild,
} from '@bloomreach/navapp-communication';
import { from, Observable } from 'rxjs';

import { ClientApplicationsManagerService } from '../client-applications-manager/services';

@Injectable({
  providedIn: 'root',
})
export class CommunicationsService {
  private connections: Map<string, Observable<ChildPromisedApi>> = new Map();

  constructor(private clientAppsManager: ClientApplicationsManagerService) {}

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
