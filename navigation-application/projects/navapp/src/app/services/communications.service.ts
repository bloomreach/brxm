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
import { Observable } from 'rxjs';
import { flatMap } from 'rxjs/operators';

import { ClientAppService } from '../client-app/services';

import { OverlayService } from './overlay.service';

@Injectable({
  providedIn: 'root',
})
export class CommunicationsService {
  constructor(
    private clientAppService: ClientAppService,
    private overlay: OverlayService,
  ) {}

  get parentApiMethods(): ParentApi {
    return {
      showMask: () => this.overlay.enable(),
      hideMask: () => this.overlay.disable(),
    };
  }

  navigate(clientAppId: string, path: string): void {
    this.clientAppService
      .getApp(clientAppId)
      .api.navigate({ path })
      .then(() => {
        this.clientAppService.activateApplication(clientAppId);
      });
  }

  logout(): Observable<Promise<void>> {
    return this.clientAppService.apps$.pipe(
      flatMap(apps =>
        apps.filter(app => app.api.logout).map(app => {
          console.log(`logging out of ${app.id}`);
          return app.api.logout();
        })),
    );
  }
}
