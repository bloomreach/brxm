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
import { ParentApi } from '@bloomreach/navapp-communication';

import { ClientAppService } from '../client-app/services';

import { ConnectionService } from './connection.service';
import { OverlayService } from './overlay.service';

@Injectable({
  providedIn: 'root',
})
export class CommunicationsService {
  constructor(
    private clientAppsManager: ClientAppService,
    private overlay: OverlayService,
    private connectionService: ConnectionService,
  ) {}

  get parentApiMethods(): ParentApi {
    return {
      showMask: () => this.overlay.enable(),
      hideMask: () => this.overlay.disable(),
    };
  }

  navigate(clientAppId: string, path: string): void {
    this.connectionService
      .getConnection(clientAppId)
      .navigate({ path })
      .then(() => {
        this.clientAppsManager.activateApplication(clientAppId);
      });
  }
}
