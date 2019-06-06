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
import { Observable } from 'rxjs';
import { mergeMap } from 'rxjs/operators';

import { ClientApp } from '../client-app/models/client-app.model';
import { ClientAppService } from '../client-app/services';

@Injectable({
  providedIn: 'root',
})
export class CommunicationsService {

  private static async resolveAlways<T>(p: Promise<T>): Promise<any> {
    try {
      return await p;
    } catch (err) {
      return err;
    }
  }
  constructor(
    private clientAppService: ClientAppService,
  ) { }

  navigate(clientAppId: string, path: string): Promise<void> {
    return this.clientAppService
      .getApp(clientAppId)
      .api.navigate({ path })
      .then(() => {
        this.clientAppService.activateApplication(clientAppId);
      });
  }

  logout(): Observable<any[]> {
    return this.clientAppService.apps$.pipe(
      mergeMap(apps => Promise.all(this.logoutApps(apps))),
    );
  }

  private logoutApps(clientApps: ClientApp[]): Promise<any>[] {
    return clientApps
      .filter(app => app.api.logout)
      .map(app => CommunicationsService.resolveAlways(app.api.logout()));
  }

}
