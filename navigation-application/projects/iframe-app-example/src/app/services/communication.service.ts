/*
 * Copyright 2020 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

// tslint:disable:no-console
import { Injectable } from '@angular/core';
import {
  ChildApi,
  ClientErrorCodes,
  connectToParent,
  ParentApi,
  ParentConfig,
  ParentConnectConfig,
} from '@bloomreach/navapp-communication';

import { AppState } from './app-state';

@Injectable({
  providedIn: 'root',
})
export class CommunicationService {
  private parentApi: ParentApi;
  private config: ParentConfig;

  constructor(private readonly state: AppState) { }

  get parentApiVersion(): string {
    return this.config ? this.config.apiVersion : '';
  }

  async connect(childApiMethods: ChildApi): Promise<void> {
    if (window.parent === window) {
      console.error('Iframe app was not loaded inside iframe');
      return;
    }

    const connectionConfig: ParentConnectConfig = {
      parentOrigin: '*',
      methods: childApiMethods,
    };

    this.parentApi = await connectToParent(connectionConfig);
    this.config = await this.parentApi.getConfig();
  }

  navigateTo(path: string, breadcrumbLabel?: string): void {
    if (!this.state.navigatedTo || !path.startsWith(this.state.navigatedTo.path)) {
      this.parentApi.navigate({ path, breadcrumbLabel });

      return;
    }

    this.state.navigatedTo.path = path;
    this.parentApi.updateNavLocation({ path: this.state.navigatedTo.path, breadcrumbLabel });
  }

  toggleMask(): void {
    if (this.state.overlaid) {
      this.parentApi.hideMask().then(() => {
        this.state.overlaid = false;
      });
    } else {
      this.parentApi.showMask().then(() => {
        this.state.overlaid = true;
      });
    }
  }

  sendError(errorCode: ClientErrorCodes, message?: string): void {
    this.parentApi.onError({ errorCode, message });
  }

  notifyAboutUserActivity(): void {
    this.parentApi.onUserActivity();
  }
}
